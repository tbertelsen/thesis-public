#!/bin/bash
#$ -S /bin/bash
#$ -pe smp 63
#$ -l h_vmem=4G
#$ -l mem_free=4G
#$ -m abe -M MYEMAIL@gmail.com

DATA=/eva/users/dkn957/data/

SIZE=`echo "2 ^ ${SGE_TASK_ID}" | bc`

echo "Staring alignment of HMP253.${SIZE}000 at"
date

# Command to ggsearh
# cd ${DATA}
# NAME=HMP253.${SIZE}000
# FASTABIN=/home/dkn957/lib/fasta-36.3.7/bin
# DATAFILE=${NAME}.fa
# OUTFILE=${NAME}.align
# ${FASTABIN}/ggsearch36 -3 -m 8C -E 10000 -d 0 $DATAFILE $DATAFILE > $OUTFILE

# Command to thesis.jar
java $JVM_OPT_XXL -cp $HOME/lib/thesis.jar apps.ProxyTester align -d HMP$SIZE

echo "Completed at "
date
