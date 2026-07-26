#!/usr/bin/env bash
set -euo pipefail

# Absolute paths to be safe
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
DIST_DIR="$ROOT_DIR/dist"

mkdir -p "$DIST_DIR"

echo "Building production JAR..."
mvn clean package -Dvaadin.productionMode=true -Pproduction

# Find the first .jar in target/ that is not a .jar.original
JAR=$(ls "$ROOT_DIR"/target/*.jar 2>/dev/null | grep -v '\.original$' | head -n1 || true)
if [ -z "$JAR" ]; then
  echo "No jar found in target/" >&2
  exit 1
fi

echo "Copying $JAR to $DIST_DIR/"
cp -v "$JAR" "$DIST_DIR/"

echo "Done. Dist contents:"
ls -l "$DIST_DIR"

echo "Building Electron app"
# Allow passing the npm script to run as first argument, default to "dist:win"
BUILD_TARGET="${1:-dist:win}"
echo "Running: npm run $BUILD_TARGET"
cd electron-starter
npm run "$BUILD_TARGET"
cd ..
