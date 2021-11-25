#!/bin/bash

PWD=$(pwd)
DESTINATION="$PWD/$1"
echo $DESTINATION

cd ..
unlink jacoco.exec
unlink ./full-teaching-master/target/jacoco.exec

java -javaagent:./jacoco-0.8.7/lib/jacocoagent.jar -jar ./full-teaching-master/target/full-teaching-1.0.0.war &
PID=$!
sleep 30
cd full-teaching-master
mvn test

cd ..


java -jar ./jacoco-0.8.7/lib/jacococli.jar merge ./full-teaching-master/target/jacoco.exec ./jacoco.exec --destfile $DESTINATION/jacoco.exec
java -jar ./jacoco-0.8.7/lib/jacococli.jar report ./full-teaching-master/target/jacoco.exec ./jacoco.exec --classfiles full-teaching-master/target/classes --html $DESTINATION

kill -9 $PID
