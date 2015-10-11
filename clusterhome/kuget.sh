#!/bin/sh

rsync -avhz -e "ssh -i '$KUKEY'" dkn957@porus02:/home/dkn957/$1 $1
