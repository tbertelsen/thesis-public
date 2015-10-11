#!/bin/bash
#$ -S /bin/bash
#$ -m base -M MYEMAIL@gmail.com
#$ -cwd
#$ -pe smp 63
#$ -l h_vmem=4G
#$ -l mem_free=4G


LIB=/home/dkn957/lib/
CP=$LIB/thesis.jar

D=$d
opt=$JVM_OPT_XXL
echo "d=$d"
echo "pwd=$PWD"
echo $opt

for (( k = 1; k < 16; k++ )); do
	date
	java $opt -cp $CP apps.ProxyTester calcProxy -d $D -k $k
done


