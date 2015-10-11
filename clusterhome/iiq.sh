#!/bin/bash

SPARK_HOME=/root/spark

source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"
s3_results=...
s3_data=...
master=spark://...:7077

cores=$(( 4 * 16 ))
parallelism=$(( 4 * $cores ))
export dk_tbertelsen_cores=$cores
executor_memory=115G

outdir=$HOME/output/iiq
resultbucket=s3://$s3_results/iiq
echo "Creating $outdir"
mkdir -p $outdir

function setup {
	filename=HMP253.${size}000

	fasta=s3n://$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY@$s3_data/$filename.fa
	uclust=s3n://$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY@$s3_results/uclust/$filename.uc
	cdhit=s3n://$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY@$s3_results/cdhit/$filename.cdhit.clstr

	outfile=$outdir/$filename.iiq.txt
	s3outfile=$resultbucket/$filename.iiq.txt
}

function run {
	echo "Running spark at $master, with -f $fasta -o $outfile $@"

	$SPARK_HOME/bin/spark-submit \
	  --class apps.IiqCalculator \
	  --master $master \
	  --driver-memory 10G \
	  --executor-memory ${executor_memory} \
	  --conf "spark.eventLog.enabled=false" \
	  --conf "spark.default.parallelism=$parallelism" \
      --conf "spark.network.timeout=600" \
      --conf "spark.akka.timeout=600" \
      --conf "spark.akka.askTimeout=600" \
      --conf "spark.akka.lookupTimeout=600" \
      --conf "spark.akka.num.retries=10" \
      --conf "spark.storage.blockManagerSlaveTimeoutMs=1200000" \
	  $HOME/thesis.jar -m $master -f $fasta -o $outfile $@

	$HOME/bin/aws s3 cp $outfile $s3outfile

}

for size in 2 4 8 16 32 64 128 256 512 ; do
	setup
	run -c $cdhit -u $uclust
done

for size in 1024 2048 ; do
	setup
	run -c $cdhit
done
