#!/bin/bash

export SGE_O_WORKDIR=$HOME/sge-wd
export SGE_TASKID=1$1

./spark.sge.sh
