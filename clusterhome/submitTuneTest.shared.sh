#!/bin/bash

if [ "$dataDir" = "" ] || [ "$filename" = "" ] || [ "$SPARK_HOME" = "" ]; then
  echo "ERROR: A variable is not not set:"
  exit 3
fi

input=$dataDir/$filename.fa
outdir=$dataDir/tunetests/$filename/

if [ ! -s $input ] ; then
  echo "Input not found: $input"
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


echo "Running spark at $master, with -i $input -m $master -w $outdir"

$SPARK_HOME/bin/spark-submit \
  --class apps.TuneTester \
  --master $master \
  --executor-memory ${executor_memory} \
  --conf "spark.eventLog.dir=$HOME/spark-evetlog/applicationHistory" \
  --conf "spark.eventLog.enabled=true" \
  --conf "spark.default.parallelism=$parallelism" \
  $HOME/lib/thesis.jar run -i $input -m $master -w $outdir

echo "Done"
