#!/bin/bash

# Configuration:

# login info on remote server:
# REMOTE_HOST=porus01
REMOTE_HOST=hpc-fe.gbar.dtu.dk
SSH_SERVER=dtu
# location of the spark shared directory on remote:
#SPARK_SHARED=/home/dkn957
SPARK_SHARED=/zhome/bf/1/57671/pbs-wd/spark

# Functions;

source spark.functions.sh
function remoteCat {
	ssh $SSH_SERVER "cat $1"
}
function remotePort {
	ssh $SSH_SERVER "$(typeset -f); freePort"
}
function remoteTunnel {
	ssh -f $SSH_SERVER -R $REMOTE_HOST:$1:localhost:$1 -N
}
function killTunnel {
	kill $(ps -e | grep "[s]sh -f .* -R $1" | grep -o "^\s*[0-9][0-9]*" | grep -o "[0-9][0-9]*")
}

# Get the master address
echo "Fetting master address"
MASTER_ADDR_FILE=$SPARK_SHARED/spark-master-addr
MASTER=`remoteCat $MASTER_ADDR_FILE`
echo "Master: $MASTER"

# Get free ports on remote. We take the chance and hope they are free on the local machine.
echo "Getting ports"
DRIVER_PORT=`remotePort`
FILESERVER_PORT=`remotePort`
BROADCAST_PORT=`remotePort`
REPL_PORT=`remotePort`
BLOCK_PORT=`remotePort`


echo "Starting tunnels on: $DRIVER_PORT $FILESERVER_PORT $BROADCAST_PORT $REPL_PORT $BLOCK_PORT"
remoteTunnel $DRIVER_PORT
remoteTunnel $FILESERVER_PORT
remoteTunnel $BROADCAST_PORT
remoteTunnel $REPL_PORT
remoteTunnel $BLOCK_PORT

$SPARK_HOME/bin/spark-shell --master spark://$MASTER \
--conf "spark.driver.host=$REMOTE_HOST" \
--conf "spark.driver.port=$DRIVER_PORT" \
--conf "spark.fileserver.port=$FILESERVER_PORT" \
--conf "spark.broadcast.port=$BROADCAST_PORT" \
--conf "spark.replClassServer.port=$REPL_PORT" \
--conf "spark.blockManager.port=$BLOCK_PORT"



echo "Closing tunnels on: $DRIVER_PORT $FILESERVER_PORT $BROADCAST_PORT $REPL_PORT $BLOCK_PORT"
killTunnel $DRIVER_PORT
killTunnel $FILESERVER_PORT
killTunnel $BROADCAST_PORT
killTunnel $REPL_PORT
killTunnel $BLOCK_PORT


