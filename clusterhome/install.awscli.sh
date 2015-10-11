#!/bin/bash
curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip"
unzip awscli-bundle.zip

./awscli-bundle/install -b ~/bin/aws

source ~/.secrets/awskeys.sh
export AWS_ACCESS_KEY_ID="$IRELAND_KEY_ID"
export AWS_SECRET_ACCESS_KEY="$IRELAND_KEY_SECRET"

bin/aws configure set aws_access_key_id $AWS_ACCESS_KEY_ID
bin/aws configure set aws_secret_access_key $AWS_SECRET_ACCESS_KEY
bin/aws configure set default.region eu-west-1

#Update configurartion

echo "export SPARK_WORKER_DIR=/mnt/spark/work" >> $HOME/spark/conf/spark-env.sh

$HOME/spark-ec2/copy-dir /root/spark/conf
