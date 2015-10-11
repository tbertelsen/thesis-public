#!/bin/sh

sbt assembly

scp -i "$KUKEY" target/scala-2.10/thesis.jar dkn957@porus02:/home/dkn957/lib/thesis.jar
