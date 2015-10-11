#!/bin/bash
source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"

$SPARK_HOME/ec2/spark-ec2 \
-k $IRELAND_PEM_NAME \
-i $IRELAND_PEM \
-s 4 \
--instance-type=r3.4xlarge \
--master-instance-type=r3.large \
--region=eu-west-1 \
--zone=eu-west-1b \
--spot-price=1 \
--spark-version=1.3.0 \
--hadoop-major-version=2 \
launch genecluster
