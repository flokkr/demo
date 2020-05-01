#!/usr/bin/env bash
set -e

export ALL_RESULT_DIR="target/all"

export RESULT_DIR="target/ozone"
./ozone/test.sh
rebot -n ozone -o "$ALL_RESULT_DIR/ozone.xml" "$RESULT_DIR/*.xml" || true

#sleep 10

export RESULT_DIR="target/hadoop"

./hadoop/test.sh

rebot -n hadoop -o "$ALL_RESULT_DIR/hadoop.xml" "$RESULT_DIR/*.xml" || true

mkdir -p "result"
rebot -n demo -d target/result "$ALL_RESULT_DIR/*.xml"
