This will help you run spark in the cluster.

1. Develop locally:
-------

SBT and scala is only needed on your local machine.

Package our code with [SBT assembly](https://github.com/sbt/sbt-assembly) to create a jar including your dependencies. Then put your 'jar' onto porus with `scp`.

Remember to set spark as "provided" in build.sbt:

``` scala
libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "1.2.0" % "provided",
    "org.apache.spark" %% "spark-graphx" % "1.2.0" % "provided",
    "org.apache.spark" %% "spark-mllib" % "1.2.0" % "provided"
)
```

2. Launch a private spark standalone.
--------

Launch a private spark cluster, as an array job.
I have create script for this. It will save the master address to a file, and automatically shut down if you delete that file.

You do not need SBT or Scala on the cluster. The spark distribution has all it needs – including its scala dependencies.

###First time:

Get the script and set `$SPARK_HOME`:

``` bash
cp /home/dkn957/lib/spark/spark.porus.example.sh ~/spark.porus.sh
echo "export SPARK_HOME=spark.porus.example.sh" >> ~/.bashrc
source ~/.bashrc
```

Update all codeblocks in the script marked with `# UPDATE:`

```
nano ~/spark.porus.sh
```

###Everytime

Update the runtime, cores and memory in `~/spark.porus.sh`

Schedule it with `-t 1-n` where `n` is the number of worker nodes e.g.

```
qsub -t 1-3 ~/spark.porus.sh
```

Check your email.

3. Connect to the cluster
-----------------

When the cluster is running, get the master address from the email sent to you.
Submit a job as usual by running

```
$SPARK_HOME/bin/spark-submit ....
```

You can also launch an interactive shell, by first starting a interactive qlogin session(!!!),  and then running the spark shell

```
qlogin
# Wait a bit
$SPARK_HOME/bin/spark-shell ...
```

You must be logged in to the cluster, to run these commands.


4. Access the web UI.
--------------------

The script will send you an email with the address to the web UI, so you can monitor your jobs.
You will need to create a SOCKS proxy to access it, since the firewall blocks all ports except `22` and `8080` to the cluster.

On your local machine run:

```
ssh -D 4321 porus01
```

And then set your browser to use `localhost:4321` to access it.

If you are on linux. You can also look into sshuttle.

