#!/bin/bash
#PBS -S /bin/bash
#PBS -t 1-4
#PBS -l walltime=00:05:00
#PBS -l nodes=1:ppn=20
#PBS -l vmem=120gb
#PBS -m abe -M MYEMAIL@gmail.com

date
echo "Hello from $PBS_ARRAYID at `hostname`"
sleep 120
date
