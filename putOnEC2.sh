#!/bin/sh

sbt assembly

rsync -avhzP target/scala-2.10/thesis.jar ec2SparkMaster:/root/thesis.jar
