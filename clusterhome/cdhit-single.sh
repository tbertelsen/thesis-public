#!/bin/bash -e

source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"
s3_results=...
s3_data=...
master=spark://...:7077

aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
aws configure set default.region eu-west-1

s3outdir=s3://$s3_results/cdhit
shareddir=/mnt/data
datadir=$shareddir/data
libdir=$shareddir/lib

rm -rf $datadir
mkdir -p $datadir/cdhit

cdhitdir="`echo $libdir/cd-hit*`"
echo $cdhitdir

# for size in 2 ; do
for size in 2 4 8 16 32 64 128 256 512 1024 2048 4096 8192 16384 ; do
	filename=HMP253.${size}000

	input=$datadir/$filename.fa
	s3input=s3://$s3_data/$filename.fa
	output=$datadir/cdhit/$filename.cdhit
	s3output=$s3outdir/$filename.cdhit

	timefile=$datadir/cdhit/$filename.time
	s3timefile=$s3outdir/$filename.time
	stdoutfile=$datadir/cdhit/$filename.out.txt
	s3stdoutfile=$s3outdir/$filename.out.txt
	stderrfile=$datadir/cdhit/$filename.err.txt
	s3stderrfile=$s3outdir/$filename.err.txt

	date
	start=$SECONDS
	aws s3 cp $s3input $input
	end=$SECONDS
	getdatatime=$(( $end - $start ))
	echo "Done"

	echo "running CD hit "
	start=$SECONDS
	echo "$cdhitdir/cd-hit-est -i $input -o $output -c 0.95 -n 9 -M 230000 -T 32 -aS 0.8 -G 1 -g 1 -d 0 > $stdoutfile 2> $stderrfile"
	$cdhitdir/cd-hit-est -i $input -o $output -c 0.95 -n 9 -M 230000 -T 32 -aS 0.8 -G 1 -g 1 -d 0 > $stdoutfile 2> $stderrfile
	# --R $restartfile
	# > $stdoutfile 2> $stderrfile

	end=$SECONDS
	cdhittime=$(( $end - $start ))
	echo "Done"

	start=$SECONDS
	aws s3 cp $output $s3output
	aws s3 cp $output.clstr $s3output.clstr
	end=$SECONDS
	saveresultstime=$(( $end - $start ))

	totaltime=$(( $getdatatime + $cdhittime + $saveresultstime ))

	echo "# `date`" > $timefile
	echo "getdatatime=$getdatatime" >> $timefile
	echo "cdhittime=$cdhittime" >> $timefile
	echo "saveresultstime=$saveresultstime" >> $timefile
	echo "totaltime=$totaltime" >> $timefile
	aws s3 cp $timefile $s3timefile
	aws s3 cp $stdoutfile $s3stdoutfile
	aws s3 cp $stderrfile $s3stderrfile

	date

	echo "$filename took $totaltime seconds"

done
