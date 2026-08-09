#!/bin/bash

trap 'echo; echo "Beendet. Enter drücken..."; read' EXIT

source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk current java
sdk current maven

mvn --version
java --version

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"



mvn clean package -DskipTests -Dvaadin.productionMode=true -Pproduction
java -Dvaadin.productionMode=true -jar "$SCRIPT_DIR/target/kontoauszuege-app-1.0-SNAPSHOT.jar"
