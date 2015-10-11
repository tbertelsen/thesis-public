#!/bin/bash

SPARK_HOME=/root/spark

source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"
s3_results=...
s3_data=...
master=spark://...:7077

parallelism=128
executor_memory=230G

filename=HMP253.128000
input=s3n://$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY@$s3_data/$filename.fa

timestamp=`date +%Y-%m-%d_%H-%M`
subpath=tunetests/$filename/$timestamp

outdir=$HOME/output/$subpath
resultbucket=s3://$s3_results/$subpath

echo "Creating $outdir"

mkdir -p $outdir


echo "Running spark at $master, with -i $input -m $master -w $outdir"

$SPARK_HOME/bin/spark-submit \
  --class apps.TuneTester \
  --master $master \
  --executor-memory ${executor_memory} \
  --conf "spark.eventLog.enabled=false" \
  --conf "spark.default.parallelism=$parallelism" \
  $HOME/thesis.jar run -i $input -m $master -w $outdir


echo "Creating plot"
java -cp $HOME/thesis.jar apps.TuneTester plot -w $outdir

echo "Copying results to s3"
$HOME/bin/aws s3 cp $outdir $resultbucket --recursive

echo "Done"
