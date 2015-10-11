#!/bin/sh

rsync -avhz -e "ssh -i '$KUKEY'" $1 s093267@transfer.gbar.dtu.dk:/zhome/bf/1/57671/$1
