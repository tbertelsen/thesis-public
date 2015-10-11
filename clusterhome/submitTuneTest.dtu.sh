#!/bin/bash
#PBS -S /bin/bash
#PBS -l walltime=10:00:00
#PBS -l nodes=1:ppn=2
#PBS -l vmem=10gb
#PBS -m abe -M MYEMAIL@gmail.com
#PBS -N TuneTest


dataDir=$HOME/genedata
SPARK_HOME=$HOME/lib/spark-1.3.0-bin-hadoop2.4

filename=1495
filename=HMP253.16000
filename=HMP253.32000

executor_memory=100G
