#!/bin/bash

if [ "$dataDir" = "" ] || [ "$filename" = "" ] || [ "$SPARK_HOME" = "" ]; then
  echo "ERROR: A variable is not not set:"
  exit 3
fi

fastafile=$dataDir/$filename.fa
outdir=$dataDir/benchmarks/

if [ ! -s $fastafile ] ; then
  echo "fastafile not found: $fastafile"
  exit 1
fi


masterFile=$HOME/spark/spark-master-addr
if [ -z $master ] && [ -s $masterFile ] ; then
  master="spark://`cat $masterFile`"
fi

if [ -z $master ] ; then
  echo "No master set. $masterFile"
  exit 2
fi

echo "Creating $outdir"
mkdir -p $outdir


echo "Running spark at $master, with -f $fastafile -m $master -o $outdir"


function run {
$SPARK_HOME/bin/spark-submit \
  --class apps.GeneClustering \
  --master $master \
  --executor-memory ${executor_memory} \
  --conf "spark.eventLog.dir=$HOME/spark-evetlog/applicationHistory" \
  --conf "spark.eventLog.enabled=true" \
  --conf "spark.default.parallelism=$threads" \
  $HOME/lib/thesis.jar benchmark -f $fastafile -m $master -o $outdir $@
}

run --repetitions 1
sleep 10
run --repetitions 1 --proxy-centers


echo "Done"
