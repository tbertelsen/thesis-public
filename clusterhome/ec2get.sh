#!/bin/sh

rsync -avhz ec2SparkMaster:/root/$1 $1
