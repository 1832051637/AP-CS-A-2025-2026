#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)

exec mvn -f "$PROJECT_DIR/pom.xml" org.openjfx:javafx-maven-plugin:0.0.8:run "$@"
