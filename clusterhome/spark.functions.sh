#!/bin/bash

function echoerr { echo "$@" 1>&2; }

function waitForContent {
  local TIMEOUT=$1
  local FILE=$2
  local SLEEP_PERIOD=$3
  if [ -z $SLEEP_PERIOD ]; then SLEEP_PERIOD=5 ; fi
  while true; do
    echoerr "cat $FILE"
    echoerr "`cat $FILE`"
  	if [ -s $FILE ]; then
  	  content=`cat $FILE`
  	  if [ ! -z $content ] ; then
        echoerr "Found content: $content"
        echo $content
        break;
      else
        echoerr "File exists but is empty:"
      fi
  	elif [ $SECONDS -gt $TIMEOUT ]; then
  	  echoerr "Timed out while wainting for content in $SPARK_MASTER_URL_FILE"
  	  break
  	fi
    echoerr "Still no content in: $FILE"
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

