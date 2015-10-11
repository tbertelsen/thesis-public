#!/bin/bash

source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"
s3_results=...
s3_data=...
master=spark://...:7077

aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
aws configure set default.region eu-west-1

s3outdir=s3://$s3_results/uclust
datadir=$HOME/data

rm -rf $datadir
mkdir -p $datadir/uclust

for size in 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 ; do
	filename=HMP253.${size}000

	input=$datadir/$filename.fa
	s3input=s3://$s3_data/$filename.fa
	sorted=$datadir/uclust/$filename-sorted.fa
	output=$datadir/uclust/$filename.uc
	s3output=$s3outdir/$filename.uc
	timefile=$datadir/uclust/$filename.time
	s3timefile=$s3outdir/$filename.time
	stdoutfile=$datadir/uclust/$filename.out.txt
	s3stdoutfile=$s3outdir/$filename.out.txt
	stderrfile=$datadir/uclust/$filename.err.txt
	s3stderrfile=$s3outdir/$filename.err.txt

	date
	start=$SECONDS
	aws s3 cp $s3input $input
	end=$SECONDS
	getdatatime=$(( $end - $start ))
	echo "Done"

	echo "Sorting "
	start=$SECONDS
	$HOME/lib/usearch8.0/usearch -sortbylength $input -fastaout $sorted -minseqlength 1 > $stdoutfile 2> $stderrfile
	echo "# start clustering" >> $stdoutfile 2>> $stderrfile
	echo "Clustering"
	$HOME/lib/usearch8.0/usearch -cluster_smallmem $sorted -id 0.95 -uc $output >> $stdoutfile 2>> $stderrfile
	end=$SECONDS
	uclusttime=$(( $end - $start ))
	echo "Done"

	start=$SECONDS
	aws s3 cp $output $s3output
	end=$SECONDS
	saveresultstime=$(( $end - $start ))

	totaltime=$(( $getdatatime + $uclusttime + $saveresultstime ))

	echo "# `date`" > $timefile
	echo "getdatatime=$getdatatime" >> $timefile
	echo "uclusttime=$uclusttime" >> $timefile
	echo "saveresultstime=$saveresultstime" >> $timefile
	echo "totaltime=$totaltime" >> $timefile
	aws s3 cp $timefile $s3timefile
	aws s3 cp $stdoutfile $s3stdoutfile
	aws s3 cp $stderrfile $s3stderrfile

	date

	echo "$filename took $totaltime seconds"

done
