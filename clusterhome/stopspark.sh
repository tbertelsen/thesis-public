#!/bin/bash
PID=`cat /tmp/tob/spark/spark.pid`
echo "Termination all 'sleep' childs of PID=$PID"
pkill -P $PID sleep
