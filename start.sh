#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java -Dvaadin.productionMode=true -jar "$SCRIPT_DIR/kontoauszuege-app-1.0-SNAPSHOT.jar"
