#!/usr/bin/env bash

set -e
for file in $(find -name Flekszible); do
    DIR=$(dirname $file)
    echo "Generating files from/to $DIR"
    cd $DIR
    flekszible generate
    cd -
done
