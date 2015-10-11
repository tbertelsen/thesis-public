#!/bin/bash

export PBS_O_WORKDIR=$HOME/pbs-wd
export PBS_ARRAYID=1$1
export PBS_O_HOME=$HOME/pbs-home

./spark.sge.sh
