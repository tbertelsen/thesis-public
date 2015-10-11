#!/bin/bash
#$ -S /bin/bash
#$ -N Tob_spark
#$ -m base -M MYEMAIL@gmail.com
#$ -pe smp 64
#$ -l h_vmem=4G
#$ -l mem_free=4G




# SPARK_DATA_DIR must be local or other fast storage. Optionaly shared between tasks.
# SPARK_SHARED Must be mounted directory that all workers/master can access

# SGE locations
export SPARK_DATA_DIR="/tmp/$USER/spark"
export SPARK_LOG_DIR=/home/dkn957/spark-logs
export TASKID=$SGE_TASK_ID
export SPARK_SHARED=/home/dkn957/spark
export SPARK_HOME=$HOME/lib/spark/spark-1.3.0-bin-hadoop2.4/

export RUNTIME_HOURS=38
export CORES=64
export MEM_GB=230

./spark.sge.sh
