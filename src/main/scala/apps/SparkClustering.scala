package apps

import apps.SparkClustering.{ClusterGraph, PriorityGraph, PartiallyAssignedGraph, InputGraph}
import collections.RddUtils.isEmpty
import org.apache.spark.graphx._
import org.apache.spark.graphx.VertexId
import sequences.DnaSeq
import sequences.jaligner.AlignmentParameters.usearch
import sequences.jaligner.{AlignmentParameters, JalignerUtils}
import utils.Equations.elementInformationScore
import scala.math.Ordering.Implicits._
import scala.reflect.ClassTag

/**
 * Created by tobber on 11/2/15.
 */

@SerialVersionUID (5709304680510234L)
abstract class SparkClustering[T: ClassTag]() extends Serializable {
  private def is(target: ClusterAssignment) : ((VertexId, (T, ClusterAssignment))) => Boolean = {
    case (id,(dna,assignment)) => target == assignment
  }
  private def isNot(target: ClusterAssignment) : ((VertexId, (T, ClusterAssignment))) => Boolean = {
    case (id,(dna,assignment)) => target != assignment
  }

  def edgeInformationContribution(edgeWeight: Double) : Double

  private def setPriorities(graph: InputGraph[T]) : PriorityGraph = {
    val contributions = graph.aggregateMessages[Double]({ ec =>
      val contribution = edgeInformationContribution(ec.attr)
      ec.sendToDst (contribution)
      ec.sendToSrc (contribution)
    },
    _ + _,
    TripletFields.EdgeOnly)

    val priorityGraph = graph.outerJoinVertices (contributions)(
      (id, dna, contribution) => createPriority (id, dna, contribution.getOrElse (0.0)))
    priorityGraph
  }

  def createPriority(id: VertexId, dna: T, priority: Double) : Priority

  def markNeighbors(centeredGraph: Graph[(T, Boolean), Double]) : VertexRDD[Boolean]

  def assignToCenters(graph: PartiallyAssignedGraph[T]) : VertexRDD[Assigned]

  def extractName(dna: T) : String

  private def clusterStep(graph: InputGraph[T]): PartiallyAssignedGraph[T] = {
    val priorityGraph = setPriorities(graph)

    // Find max neighbor priority
    val maxNeighborPriorities = priorityGraph.aggregateMessages[Priority]({ ec =>
      ec.sendToDst (ec.srcAttr); ec.sendToSrc (ec.dstAttr)
    }, {
      _ max _
    })

    // Find cluster centers
    val isCenter = priorityGraph.outerJoinVertices (maxNeighborPriorities)(
      (_, ownPriority, neighborPriority) => neighborPriority.isEmpty || ownPriority > neighborPriority.get
    )
    val centeredGraph = graph.outerJoinVertices (isCenter.vertices)(
      (_, dna, isCenterOpt) => (dna, isCenterOpt.get)
    )

    // Create clusters
    // For each edge that is connected to a center, we calculate the alignment.
    // If the alignment is good enough we assign the centerId as clusterId
    val centerNeighbors = markNeighbors(centeredGraph)

    // Mark centers and neighbors
    val clusteredGraph: PartiallyAssignedGraph[T] = centeredGraph.outerJoinVertices (centerNeighbors) {
      (vid, attr, isNeighborOpt) => {
        val (dna, isCenter) = attr
        (isCenter, isNeighborOpt.getOrElse (false)) match {
          case (false, false) => (dna, Unassigned)
          case (true, false) => (dna, Center)
          case (false, true) => (dna, Neighbor)
          case (true, true) => throw new IllegalStateException ("Center is neighbor to a center: ")
        }
      }
    }
    clusteredGraph
  }

  private def filterUnassigned(newAssignments: PartiallyAssignedGraph[T]) = {
    // Select only nodes that have no cluster center defined
    val subgraph = newAssignments.subgraph (vpred = (id,pair) => pair._2 == Unassigned)
    // Remove 'Unassigned' attribute
    subgraph mapVertices ((id, attr) => attr._1)
  }

  private def addClusteredNodes(newAssignments: PartiallyAssignedGraph[T], partlyAssignedGraph: PartiallyAssignedGraph[T]) = {
    // Select only nodes that are Center or Neighbor (No Assigned should exist)
    val newClusterVertices = newAssignments.vertices.filter(isNot(Unassigned))
    // Overwrite all 'Unassigned' values.
    partlyAssignedGraph.joinVertices (newClusterVertices)((vid, current, newAttr) => {
      (current._2, newAttr._2) match {
        case (Unassigned, _) => newAttr
        case (_, _) => throw new IllegalStateException ("Trying to overwrite %s with %s in node %d".format(current, newAttr, vid))
      }
    })
  }

  def checkNo(assignment: ClusterAssignment, assignedGraph: PartiallyAssignedGraph[T]) = {
    val count = assignedGraph.vertices.filter(is(Unassigned)) count()
    if (count > 0) {
     throw new IllegalStateException("Clustered graph contains %d nodes that are " + assignment)
    }
  }

  def graphInfo(g : PartiallyAssignedGraph[T]) = {
      g.cache()
      println("New Centers:      " + g.vertices.filter(_._2._2 == Center).count())
      println("New Neighbors:    " + g.vertices.filter(_._2._2 == Neighbor).count())
      println("Still Unassigned: " + g.vertices.filter(_._2._2 == Unassigned).count())
      println("Center Neighbors: " + g.filter[(T, ClusterAssignment), Double](g2 => g2,
        epred = et => et.srcAttr._2 == Center && et.dstAttr._2 == Center).edges.count())
      println("Missing Neighbors: " + g.filter[(T, ClusterAssignment), Double](g2 => g2,
        epred = et => (et.srcAttr._2, et.dstAttr._2) match {
          case (Center, Unassigned) | (Unassigned, Center) => true
          case _ => false
        }).edges.count())
  }

  def cluster(graph: InputGraph[T]) : ClusterGraph = {
    var partlyAssignedGraph = clusterStep (graph) cache()
    var remainingGraph = filterUnassigned (partlyAssignedGraph) cache()
    var oldCount = -1L
    var newCount = -2L
    while (!isEmpty (remainingGraph.vertices) && oldCount != newCount) {
      val newAssignments = clusterStep (remainingGraph)
      remainingGraph = filterUnassigned (newAssignments) cache()
      partlyAssignedGraph = addClusteredNodes (newAssignments, partlyAssignedGraph)
      oldCount = newCount
      newCount = remainingGraph.vertices.count ()
      // println (List ("\nCounts:", oldCount, newCount, "\n").mkString (" "))
    }
    // Check all nodes got a cluster
    if (newCount == oldCount) {
      throw new RuntimeException ("Could not assign %d nodes to clusters".format (newCount))
    }

    // Assign nodes to cluster
    val assignments = assignToCenters(partlyAssignedGraph) cache()
    val assignedGraph = partlyAssignedGraph.joinVertices(assignments) { (id, valPair, newAssignment) =>
      (valPair._2, newAssignment) match {
        case (Center, _) => valPair
        case (Neighbor, _) => (valPair._1, newAssignment)
        case _ => throw new IllegalStateException("Node %d is neither a Neighbor or Center".format(id))
      }
    }

    // Sanity checks:
    assignedGraph cache()
    checkNo(Unassigned, assignedGraph)
    checkNo(Neighbor, assignedGraph)
    assignedGraph.mapVertices{(_, value) => (extractName(value._1), value._2)}
  }
}

object SparkClustering {
  type PartiallyAssignedGraph[T] = Graph[(T, ClusterAssignment), Double]
  type ClusterGraph = Graph[(String, ClusterAssignment), Double]
  type InputGraph[T] = Graph[T, Double]
  type PriorityGraph = Graph[Priority, Double]
}

@SerialVersionUID(46470141920371L)
class SparkIdentityClustering(alpha: Double, minIdentity: Double = 0.95) extends SparkClustering[String] {

  def edgeInformationContribution(edgeWeight: Double) = {
    elementInformationScore(edgeWeight, minIdentity, alpha)
  }

  override def markNeighbors(centeredGraph: Graph[(String, Boolean), Double]) = {
    centeredGraph.mapVertices((id,pair) => pair._2).aggregateMessages[Boolean]({ ec =>
      (ec.srcAttr, ec.dstAttr) match {
        case (false, false) => ()
        case (true, true) => throw new IllegalStateException ("Neighboring cluster-centers: " + ec.srcId + " " + ec.dstId)
        case (isSrcCenter, _) =>
          if (isSrcCenter) {
            ec.sendToDst (true)
          } else {
            ec.sendToSrc (true)
          }
      }
    }, _ || _, TripletFields.All
    )
  }

  def assignToCenters(graph: PartiallyAssignedGraph[String])  = {
    graph.mapVertices((id,pair) => pair._2).aggregateMessages[Assigned]({ec =>
      (ec.srcAttr, ec.dstAttr) match {
        case (Neighbor, Neighbor) => ()
        case (Neighbor, Center) => ec.sendToSrc(Assigned(ec.attr, ec.dstId))
        case (Center, Neighbor) => ec.sendToDst(Assigned(ec.attr, ec.srcId))
        case (x,y) => throw new IllegalStateException("Unexpected neighboring assignments: " + (x,y))
      }
    },{_ max _}, TripletFields.All)
  }

  override def createPriority(id: VertexId, dna: String, priority: Double): Priority = {
    // We don't know the length of the DNA so just use zero to exclude it from the priority
    new Priority(priority,0,id)
  }

  override def extractName(dna: String): String = {
    dna
  }
}

@SerialVersionUID(46470141920371L)
class SparkProxyClustering(alpha: Double,
      minIdentity: Double = 0.95,
      expectedMinCorr: Double = 0.45,
      alignmentParameters: AlignmentParameters = usearch) extends SparkClustering[DnaSeq] {

  // The contribution function is written based these equations relationships
  // 1. The contribution is defined as
  //     contribution = ((identity - minIdentity) / (1 - minIdentity)))^alpha
  // 2. We expect the identity to be related to the correlation:
  //     expectedIdentity = minIdentity + (1 - minIdentity)(corr - expectedMinCorr) / (1 - expectedMinCorr)
  // where expectedMinCorr is the lowest correlation we expect to find any similarities above minIdentity
  // This yields
  //    contribution = ((corr - expectedMinCorr) / (1 - expectedMinCorr))^alpha
  def edgeInformationContribution(edgeWeight: Double) = {
    elementInformationScore(edgeWeight, expectedMinCorr, alpha)
  }


  def assignToCenters(graph: PartiallyAssignedGraph[DnaSeq])  = {
    val assignments = graph.aggregateMessages[Assigned]({ec =>
      (ec.srcAttr._2, ec.dstAttr._2) match {
        case (Neighbor, Neighbor) => ()
        case (Neighbor, Center) => sendIfSimilar(ec, true, (i => Assigned(i, ec.dstId)))
        case (Center, Neighbor) => sendIfSimilar(ec, false, (i => Assigned(i, ec.srcId)))
        case (Center, Center) => ensureNotSimilar(ec)
        case (x,y) => throw new IllegalStateException("Unexpected neighboring assignments: " + (x,y))
      }
    },{_ max _}, TripletFields.All)
    assignments
  }

  override def markNeighbors(centeredGraph: Graph[(DnaSeq, Boolean), Double]) = {
    centeredGraph.aggregateMessages[Boolean]({ ec =>
      (ec.srcAttr._2, ec.dstAttr._2) match {
        case (false, false) => ()
        case (true, true) => throw new IllegalStateException ("Neighboring cluster-centers: " + ec.srcId + " " + ec.dstId)
        case (_, toSrc) => sendIfSimilar(ec, toSrc, (i => true))
      }
    }, _ || _, TripletFields.All
    )
  }

  private def sendIfSimilar[T,S](ec: EdgeContext[(DnaSeq, S), Double, T], toSrc: Boolean, factory: Double => T): Unit =   {
    val identity = JalignerUtils.calcIdentity (ec.srcAttr._1, ec.dstAttr._1, alignmentParameters)
    if (identity > minIdentity) {
      if(toSrc) {
        ec.sendToSrc(factory(identity))
      } else {
        ec.sendToDst(factory(identity))
      }
    }
  }

  private def ensureNotSimilar(ec: EdgeContext[(DnaSeq, ClusterAssignment), Double, Assigned]) = {
    val identity = JalignerUtils.calcIdentity (ec.srcAttr._1, ec.dstAttr._1, alignmentParameters)
    if (identity > minIdentity) {
      throw new IllegalStateException("There are two center neighbors! %s and %s are centers but %.3f identical.".format(ec.srcId, ec.dstId, identity))
    }
  }

  override def createPriority(id: VertexId, dna: DnaSeq, priority: Double): Priority = {
    new Priority(priority, dna.content.length, id)
  }

  override def extractName(dna: DnaSeq): String = {
    dna.name
  }
}

@SerialVersionUID (547140618994L)
case class Priority(val priority: Double, val length: Int, val id: Long) extends Ordered[Priority] with Serializable {
  def max(that: Priority) = {
    if (this >= that) this else that
  }

  def asTuple = (priority, length, id)

  override def compare(that: Priority): Int = {
    if (asTuple < that.asTuple)
      -1
    else if (asTuple > that.asTuple)
      1
    else
      0
  }
}

//object Priority {
//  def apply(dna: DnaSeq, priority: Double) = {
//    new Priority (priority, dna.content.length, dna.id)
//  }
//}

sealed trait ClusterAssignment

case object Unassigned extends ClusterAssignment

case object Center extends ClusterAssignment

case object Neighbor extends ClusterAssignment

case class Assigned(identity: Double, center: VertexId) extends ClusterAssignment {
  def max(other: Assigned) = {
    if (identity < other.identity) {
      other
    } else {
      this
    }
  }
}
