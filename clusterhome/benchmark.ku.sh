#!/bin/bash
#$ -S /bin/bash
#$ -pe smp 8
#$ -l h_vmem=2G
#$ -l mem_free=2G
#$ -m abe -M MYEMAIL@gmail.com
#$ -N benchmark

export JAVA_TOOLS_OPTIONS="-Xmx14g -Xms1g"
export MALLOC_CHECK_=1

export dataDir=/eva/users/dkn957/data
export SPARK_HOME=$HOME/lib/spark/spark-1.3.0-bin-hadoop2.4/

if [ -z "$filename" ] ; then
	filename=1495
	filename=HMP253.16000
	filename=HMP253.32000
	filename=HMP253.64000
	filename=HMP253.128000
fi

export filename
export executor_memory=210g
export threads=$(( 64 * 6 ))


./benchmark.shared.sh
