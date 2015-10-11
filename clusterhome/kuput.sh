#!/bin/sh

rsync -avhz -e "ssh -i '$KUKEY'" $1 dkn957@porus02:/home/dkn957/$1
