#!/usr/bin/env bash
mkdir -p /tmp/results
cd /tmp/results
robot /tmp/tests
python -m SimpleHTTPServer 8000
