#!/bin/bash
#$ -S /bin/bash
#$ -m base -M MYEMAIL@gmail.com
#$ -cwd
#$ -pe smp 12
#$ -l h_vmem=10G
#$ -l mem_free=10G
#$ -t 1-15


LIB=/home/dkn957/lib/
CP=$LIB/thesis.jar

opt="-Xmx110g -Xms16g"
echo "d=$d"
echo "min-align=$m"
echo $opt
echo "pwd=$PWD"

k=$SGE_TASK_ID
#for (( k = 1; k < 16; k++ )); do
	echo "k=$k"
	date
	java $opt -cp $CP apps.ProxyTester plot -d $d -k $k --min-align $m
	java $opt -cp $CP apps.ProxyTester stats -d $d -k $k
#done


