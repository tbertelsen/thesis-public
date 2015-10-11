#!/bin/sh

rsync -avhz $1 ec2SparkMaster:/root/$1
