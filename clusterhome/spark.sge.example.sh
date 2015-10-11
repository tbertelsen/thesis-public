#!/bin/bash
#$ -S /bin/bash
#PBS -S /bin/bash

# IMPORTANT: Before using this script Update all parts that are marked with '# UPDATE:'

# This script starts a single machine spark cluster for a limited time period

# Contents:
#  - User configured options and qsub-arguments
#  - Functions
#  - Derived options
#  - Check hardcoded ports
#  - Set env vars
#  - Start Spark (+ send email)
#  - Sleep
#  - Stop Spark (+ send email)

######################################
# User options:

# UPDATE: Set how long the servers should run.
# Set walltime to a little more, to allow for startup/shutdown time.
RUNTIME_HOURS=10
#PBS -l walltime:10:10:00

# UPDATE: Set reseved resources, e.g. 5 cores with 4 GB ram each.
CORES=5
MEM_GB=20
#$ -pe smp 5 -l h_vmem=4G:mem_free=4G
#PBS -l nodes=1:ppn=5:vmem=20gb


# UPDATE: Give the job a name
#$ -N MY_spark_cluster
#PBS -N MY_spark_cluster


# UPDATE: Email for status emails:
EMAIL=MYEMAL@gmail.com
#$ -m base -M MYEMAL@gmail.com
#PBS -m abe -M MYEMAL@gmail.com


# UPDATE: Make sure all of these location are correct:
# SPARK_DATA_DIR must be local or other fast storage. Optionaly shared between tasks.
# SPARK_SHARED Must be mounted directory that all workers/master can access
if [ -n "$SGE_TASK_ID" ]; then
  # SGE locations
  SPARK_DATA_DIR="/tmp/$USER/spark"
  TASKID=$SGE_TASK_ID
  SPARK_SHARED=$SGE_O_WORKDIR/spark
elif [ -n "$PBS_ARRAYID" ];
  #PBS locations
  SPARK_DATA_DIR=$PBS_O_HOME/sparkdata
  TASKID=$PBS_ARRAYID
  SPARK_SHARED=$PBS_O_WORKDIR/spark
else
  echo "Not task id found. Please run as a array job using e.g. qsub -t 1-3"
  exit 1
fi
if [ "$SPARK_HOME" = "" ]; then
  SPARK_HOME="/eva/users/dkn957/spark-1.2.0-bin-hadoop2.4"
fi

######################################
# Functions:

function echoerr { echo "$@" 1>&2; }

function waitForContent {
  local TIMEOUT=$1
  local FILE=$2
  local SLEEP_PERIOD=$3
  if [ -z $SLEEP_PERIOD ]; then SLEEP_PERIOD=5 ; fi
  while true; do
    if [ -s $FILE ]; then
      cat $FILE
      break;
    elif [ $SECONDS -gt $TIMEOUT ]; then
    echoerr "Timed out while wainting for content in $SPARK_MASTER_URL_FILE"
    break
  fi
  sleep $SLEEP_PERIOD
  done
}

function sleepWhileContentExist {
  local TIMEOUT=$1
  local FILE=$2
  local SLEEP_PERIOD=$3
  if [ -z $SLEEP_PERIOD ]; then SLEEP_PERIOD=60 ; fi
  while true; do
    if [ ! -s $FILE ]; then
      echo "Continuing since file is missing or empty: $FILE"
      break
    elif [ $SECONDS -gt $TIMEOUT ]; then
      echo "Continuing since timeout exeeded. File is still here $FILE"
    break
  fi
  sleep $SLEEP_PERIOD
  done
}

function freePort {
  # http://unix.stackexchange.com/a/132524
  python -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()'
}

######################################
# Derived options:

CONNECT_SECONDS_LIMIT=$((10*60))
RUNTIME_SECONDS=$(($RUNTIME_HOURS*60*60))

MASTER_ADDR_FILE=$SPARK_SHARED/spark-master-addr

if [ "$TASKID" = 1 ]; then
  IS_MASTER=true
fi

mkdir -p $SPARK_DATA_DIR
mkdir -p $SPARK_SHARED


#Save PID
echo $$ > $SPARK_SHARED/sparkmaster.pid

#Dirs
SPARK_LOG_DIR=$SPARK_DATA_DIR/logs
SPARK_PID_DIR=$SPARK_DATA_DIR/tmp
SPARK_LOCAL_DIRS=$SPARK_DATA_DIR/local
SPARK_WORKER_DIR=$SPARK_DATA_DIR/worker

#IP and ports. Use `freePort` to pick one randomly
WORKER_WEBUI_PORT=`freePort`

if [ "$IS_MASTER" = "true" ]; then
  MASTER_HOST=`hostname`
  MASTER_PORT=`freePort`
  MASTER_WEBUI_PORT=`freePort`
fi

######################################
# Check hardcoded ports:
PORTS_TO_CHECK="\(:$WORKER_WEBUI_PORT \)"
if [ "$IS_MASTER" = "true" ]; then
  PORTS_TO_CHECK="${PORTS_TO_CHECK}|\(:$MASTER_PORT \)\|\(:$MASTER_WEBUI_PORT \)"
fi

USED_PORTS=`netstat -an | grep "$PORTS_TO_CHECK"`
if [[ -n "$USED_PORTS" ]]
then
      echo "ERROR: Ports not free:"
      echo "$USED_PORTS"
      echo "$USED_PORTS" | mail -s "ERROR: Spark launch failed (`date`)" "$EMAIL"
      exit 1
else
      echo "Ports are free"
fi
echo "Starting Spark"

#######################################
# Set envars
export SPARK_LOG_DIR=$SPARK_LOG_DIR
export SPARK_PID_DIR=$SPARK_PID_DIR
export SPARK_LOCAL_DIRS=$SPARK_LOCAL_DIRS
export SPARK_WORKER_DIR=$SPARK_WORKER_DIR
export SPARK_WORKER_CORES=$CORES
export SPARK_WORKER_MEM=${MEM_GB}G
export SPARK_WORKER_WEBUI_PORT=$WORKER_WEBUI_PORT
export SPARK_MASTER_WEBUI_PORT=$MASTER_WEBUI_PORT
export SPARK_MASTER_PORT=$MASTER_PORT
# Make sure these are not overwritten in spark-env.sh
echo "" > $SPARK_HOME/conf/spark-env.sh


#######################################
# Start spark:
# Store stdout and stderr (2>&1) in two variables so we can email them later
echo "Starting spark at `hostname` `hostname -i`"
if [ "$IS_MASTER" = "true" ]; then
  echo "Starting master"
  MASTER_COMMAND="$SPARK_HOME/sbin/start-master.sh"
  echo "$MASTER_COMMAND"
  MASTER_COMMAND_OUT=`$MASTER_COMMAND 2>&1`
  echo "$MASTER_COMMAND_OUT"

  MASTER_ADDR="$MASTER_HOST:$MASTER_PORT"
  echo "$MASTER_ADDR" > $MASTER_ADDR_FILE

else
  echo "Waiting for master to start."
  MASTER_ADDR=`waitForContent $CONNECT_SECONDS_LIMIT $MASTER_ADDR_FILE`
fi

echo "Starting slave"
SLAVE_COMMAND="$SPARK_HOME/sbin/start-slave.sh $TASKID spark://$MASTER_ADDR"
echo "$SLAVE_COMMAND"
SLAVE_COMMAND_OUT=`$SLAVE_COMMAND 2>&1`
echo "$SLAVE_COMMAND_OUT"


#######################################
# Send email:
if [ "$IS_MASTER" = "true" ]; then
  echo "Spark master started
  Web UI:
  http://$MASTER_HOST:$MASTER_WEBUI_PORT

  Shell / submit:
  spark://$MASTER_HOST:$MASTER_PORT

  To stop spark delete this file (on $MASTER_HOST):
  $MASTER_ADDR_FILE
  ssh -i \"\$KUKEY\" \"$USER@$MASTER_HOST\" rm $MASTER_ADDR_FILE

  LOG:
  > hostname -i
  `hostname -i`

  $MASTER_COMMAND_OUT

  $SLAVE_COMMAND_OUT
  " | mail -s "Spark started at $MASTER_HOST (`date`)" "$EMAIL"
fi

#######################################
# Sleep:
echo "Sleeping for $RUNTIME_HOURS hours"
sleepWhileContentExist "$RUNTIME_SECONDS" "$MASTER_ADDR_FILE"

if [ "$IS_MASTER" = "true" ]; then
  rm -f "$MASTER_ADDR_FILE"
fi

#######################################
# Stop spark:
echo "Stopping Slave"
SLAVE_COMMAND_OUT=`$SPARK_HOME/sbin/spark-daemon.sh stop org.apache.spark.deploy.worker.Worker $TASKID 2>&1`
echo "$SLAVE_COMMAND_OUT"
echo "Stopping Master"
MASTER_COMMAND_OUT=`$SPARK_HOME/sbin/stop-master.sh 2>&1`
echo "$MASTER_COMMAND_OUT"

#######################################
# Send email:
if [ "$IS_MASTER" = "true" ]; then
  echo "Spark master stopped
  LOG:
  $MASTER_COMMAND_OUT

  $SLAVE_COMMAND_OUT
  " | mail -s "Spark stopped at $MASTER_HOST (`date`)" "$EMAIL"
fi
