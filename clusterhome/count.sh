#!/bin/bash
#$ -S /bin/bash
#$ -pe smp 63
#$ -l h_vmem=4G
#$ -l mem_free=4G
#$ -m abe -M MYEMAIL@gmail.com

if [[ -z "$filename" ]]; then
	echo "Please run with -v filename=HMP253.64000 or an other filename without '.fa'"
	exit 1
fi

#filename=HMP253.64000
dataDir=/eva/users/dkn957/data
input=$dataDir/$filename.fa
outdir=$dataDir/tunetests/$filename/
echo "Creating $outdir"
mkdir -p $outdir
java $JVM_OPT_XXL -cp lib/thesis.jar apps.TuneTester count -i $input -w $outdir
