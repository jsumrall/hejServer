#!/bin/bash
cd hejserver/
rm *.class
cd ..

javac -cp ../lib/mongo-java-driver-2.12.2.jar:../lib/json-simple-1.1.1.jar: hejserver/Main.java


java -cp ../lib/mongo-java-driver-2.12.2.jar:../lib/json-simple-1.1.1.jar:  hejserver/Main
