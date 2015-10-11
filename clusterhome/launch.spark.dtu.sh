#!/bin/bash
#PBS -S /bin/bash
#PBS -t 1-5
#PBS -l walltime=10:05:00
#PBS -l nodes=1:ppn=20
#PBS -l vmem=125gb
#PBS -l mem=120gb
#PBS -m abe -M MYEMAIL@gmail.com
#PBS -N Tob_spark

# SPARK_DATA_DIR must be local or other fast storage. Optionaly shared between tasks.
# SPARK_SHARED Must be mounted directory that all workers/master can access

#PBS locations
export SPARK_DATA_DIR=$PBS_O_HOME/sparkdata
export SPARK_LOG_DIR="$PBS_O_HOME/spark-log"
export TASKID=$PBS_ARRAYID
export SPARK_SHARED=$PBS_O_WORKDIR/spark
export SPARK_HOME=$PBS_O_HOME/lib/spark-1.3.0-bin-hadoop2.4

export RUNTIME_HOURS=10
export CORES=20
export MEM_GB=110

./spark.sge.sh
