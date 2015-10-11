#!/bin/bash

# This scripts starts a single machine spark cluster for a limited time period
# Connect to it using ssh-tunneling. Only the master webui is accessable at port 8080

# Contents:
#  - User configured options and qsub-arguments
#  - Derived options
#  - Check hardcoded ports
#  - Set env vars
#  - Start Spark (+ send email)
#  - Sleep
#  - Stop Spark (+ send email)

EMAIL=MYEMAIL@gmail.com

ERRORCODE=
if [ "$SPARK_HOME" = "" ]; then
  echo "ERROR: SPARK_HOME is not set"
  ERRORCODE=1
fi
if [ "$SPARK_DATA_DIR" = "" ]; then
  echo "ERROR: SPARK_DATA_DIR is not set"
  ERRORCODE=1
fi
if [ "$SPARK_LOG_DIR" = "" ]; then
  echo "ERROR: SPARK_LOG_DIR is not set"
  ERRORCODE=1
fi
if [ "$TASKID" = "" ]; then
  echo "ERROR: TASKID is not set"
  ERRORCODE=1
fi
if [ "$SPARK_SHARED" = "" ]; then
  echo "ERROR: SPARK_SHARED is not set"
  ERRORCODE=1
fi
if [ "$CORES" = "" ]; then
  echo "ERROR: CORES is not set"
  ERRORCODE=1
fi
if [ "$MEM_GB" = "" ]; then
  echo "ERROR: MEM_GB is not set"
  ERRORCODE=1
fi
if [ "$RUNTIME_HOURS" = "" ]; then
  echo "ERROR: RUNTIME_HOURS is not set"
  ERRORCODE=1
fi
if [ ! -z $ERRORCODE ] ; then
  exit $ERRORCODE
fi


######################################
# Derived options:

source spark.functions.sh

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
export SPARK_WORKER_MEMORY=${MEM_GB}g
export SPARK_WORKER_WEBUI_PORT=$WORKER_WEBUI_PORT
export SPARK_MASTER_WEBUI_PORT=$MASTER_WEBUI_PORT
export SPARK_MASTER_PORT=$MASTER_PORT
# Make sure these are not overwritten in spark-env.sh
echo "" > $SPARK_HOME/conf/spark-env.sh


#######################################
# Start spark:
# Store stdout and stderr (2>&1) in two variables so we can email them later
echo "Running spark at `hostname` `hostname -i`"
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
