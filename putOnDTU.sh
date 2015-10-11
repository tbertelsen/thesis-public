#!/bin/sh

sbt assembly

scp -i "$KUKEY" target/scala-2.10/thesis.jar dtu-transfer:/zhome/bf/1/57671/lib/thesis.jar
