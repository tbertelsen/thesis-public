#!/bin/bash

source ~/.secrets/awskeys.sh

function startNfsHost {
  sudo yum -y install nfs-utils rpcbind
  sudo mkdir -p /mnt/data/
  sudo chown -R ec2-user /mnt/
  echo "/mnt/data	  *(rw,sync)" | sudo tee /etc/exports
  #reload /etc/exports
  sudo exportfs -ar
  # stop services
  sudo service rpcbind stop
  sudo service nfs stop
  sudo service nfslock stop
  # restart services
  sudo service rpcbind start
  sudo service nfs start
  sudo service nfslock start
}

function startNfsClient {
  sudo yum -y install nfs-utils rpcbind
  sudo mkdir -p /mnt/data/
  sudo chown -R ec2-user /mnt/
  sudo service rpcbind start
  sudo service nfslock start
  sudo mount -t nfs $1:/mnt/data /mnt/data
}

function addKeys {
  host=$1
  scp -i $IRELAND_PEM $HOME/.ssh/ec2-internal ec2-user@$host:/home/ec2-user/.ssh/id_rsa
  scp -i $IRELAND_PEM $HOME/.ssh/ec2-internal.pub ec2-user@$host:/home/ec2-user/.ssh/id_rsa.pub
  ssh -i $IRELAND_PEM ec2-user@$host 'cat /home/ec2-user/.ssh/id_rsa.pub >> /home/ec2-user/.ssh/authorized_keys'
}

function startAllNfsClients {
	runOnSlaves "$(typeset -f startNfsClient) ; startNfsClient `hostname`"
}

function getCdhit {
  sudo yum install make glibc-devel gcc gcc-c++

  sudo mkdir -p /mnt/data/
  sudo chown -R ec2-user /mnt/
  shareddir=/mnt/data
  libdir=$shareddir/lib

  rm -rf $libdir
  mkdir -p $libdir

  cdhiturl=https://github.com/weizhongli/cdhit/releases/download/V4.6.4/cd-hit-v4.6.4-2015-0603.tar.gz
  tarname=cdhit.tar.gz
  wget -O $libdir/$tarname $cdhiturl
  tar -xvzf $libdir/$tarname -C $libdir
  cdhitdir="`echo $libdir/cd-hit*`"
  pushd $cdhitdir
  make
  popd
}

function startupOnServer {
  addSlaves
  startNfsHost
  startAllNfsClients
  getCdhit
}

function copyMasterFiles {
  master=`cat master.txt`
  scp -i $IRELAND_PEM master.txt ec2-user@$master:/home/ec2-user/master.txt
  scp -i $IRELAND_PEM slaves.txt ec2-user@$master:/home/ec2-user/slaves.txt
  scp -i $IRELAND_PEM cdhit.sh ec2-user@$master:/home/ec2-user/cdhit.sh
  scp -i $IRELAND_PEM cdhit-single.sh ec2-user@$master:/home/ec2-user/cdhit-single.sh
  scp -i $IRELAND_PEM ec2functions.sh ec2-user@$master:/home/ec2-user/ec2functions.sh
}

function startupLocal {
  master=`cat master.txt`
  addKeys $master
  for line in $(cat slaves.txt ) ; do
    addKeys $line
  done <slaves.txt
  copyMasterFiles
}

function runOnSlaves {
  cmd=$1
  for line in $(cat $HOME/slaves.txt ) ; do
  	echo "--- $line ---"
    echo "#> $cmd "
    echo "----"
    ssh -t -t $line "$cmd"
  done <$HOME/slaves.txt
}

function addSlaves {
  touch $HOME/.ssh/config
  chmod 700 $HOME/.ssh/config
  i=0
  while read line; do
  	i=$(( $i + 1 ))
    echo "Adding slave$i=$line"
    echo "Host slave$i" >> $HOME/.ssh/config
    echo "  HostName $line" >> $HOME/.ssh/config
    echo "  User ec2-user" >> $HOME/.ssh/config
    echo "  IdentityFile $HOME/.ssh/id_rsa" >> $HOME/.ssh/config
  done <$HOME/slaves.txt
}
