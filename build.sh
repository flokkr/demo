#!/usr/bin/env bash

set -e
files=(hadoop/Flekszible ozone/Flekszible);
for file in ${files[@]}; do
    DIR=$(dirname $file)
    echo "Generating files from/to $DIR"
    flekszible -s $DIR -d $DIR generate
done
