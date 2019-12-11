#!/usr/bin/env bash
POD=$(kubectl get po --selector=app=hadoop,service=test -o=custom-columns=:metadata.name --no-headers)
ksync create --selector=app=hadoop,service=test --local-read-only $(pwd) /tmp/tests
