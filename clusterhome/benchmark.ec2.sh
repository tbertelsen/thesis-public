#!/bin/bash

SPARK_HOME=/root/spark

source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"
s3_results=...
s3_data=...
master=spark://...:7077

timestamp=`date +%Y-%m-%d_%H-%M`

function setup {
	input=s3n://$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY@$s3_data/$filename.fa

	subpath=benchmarks/$filename/$cores/$timestamp

	outdir=$HOME/output/$subpath
	resultbucket=s3://$s3_results/$subpath
	allresultsbucket=s3://$s3_results/benchmarks/all/$runid
	clusteruritext=s3n://$s3_results/clusterassignments/$subpath/clusters.txt
	clusteruriobjects=s3n://$s3_results/clusterassignments/$subpath/clusters.objects

	echo "Creating $outdir"
	mkdir -p $outdir
}

function run {
	echo "Running spark at $master, with -f $input -m $master -o $outdir"
	parallelism=$(( 4 * $cores ))
	export dk_tbertelsen_cores=$cores

	$SPARK_HOME/bin/spark-submit \
	  --class apps.GeneClustering \
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
	  $HOME/thesis.jar benchmark -f $input -m $master -o $outdir -c 0.05 -l 80 -e 0.10 $@
	  # --conf "spark.akka.frameSize=1000" \
	  # --conf "spark.executor.instances=1" \
	  # --total-executor-cores $parallelism \
	  # --conf "spark.driver.memory=10G" \
}

function storeresults {
	echo "Copying results to s3"
	$HOME/bin/aws s3 cp $outdir $resultbucket --recursive
	$HOME/bin/aws s3 cp $outdir $allresultsbucket --recursive
}

function scalecores {
	runid=scalecores
	for cores in 8 16 32 ; do
		cores=$cores
		executor_memory=55G
		filename=HMP253.8000

		setup
		run --repetitions 2 --exact-corr
		sleep 5
		storeresults
	done
}

function scaledata {
	runid=scaledata3
	# for size in 2 4 8 16 32 64 128 256 512 ; do
	for size in 8192 ; do
		cores=1024
		executor_memory=230G
		filename=HMP253.${size}000

		setup
		run --repetitions 1 --exact-corr
		sleep 5
		storeresults
	done

}

function testmodifications {
	runid=mods
	cores=128
	executor_memory=230G
	filename=HMP253.2048000
	reps=1

	setup
	run --repetitions $reps
	# sleep 10
	# run --repetitions $reps --proxy-centers
	sleep 10
	run --repetitions $reps --exact-corr
	storeresults
}

function singlerun {
	runid=scaledata3
	cores=1024
	executor_memory=230G
	filename=HMP253

	setup
	run --repetitions 1 --exact-corr --save-clusters --cluster-text-uri $clusteruritext --cluster-objects-uri $clusteruriobjects
	storeresults
}

# testmodifications
singlerun
# scaledata

echo "Done"
