#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
javac --release 17 -d out src/main/java/poc/*.java src/test/java/poc/*.java
java -cp out poc.ConsistentHashRouterTest
java -cp out poc.Experiment
