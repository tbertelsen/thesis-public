#!/bin/bash
#$ -S /bin/bash
#$ -m base -M MYEMAIL@gmail.com
#$ -cwd
#$ -pe smp 12
#$ -l h_vmem=20G
#$ -l mem_free=20G


LIB=/home/dkn957/lib/
CP=$LIB/thesis.jar

opt="-Xmx220g -Xms16g"
echo "d=$d"
echo $opt
echo "pwd=$PWD"

java $opt -cp $CP apps.ProxyTester kstats -d $d --min-k 1 --max-k 15
