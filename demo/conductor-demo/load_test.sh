#!/bin/bash


for i in $(seq 1 $1); do
  sleep 1;
  ab -n $2 -c $2 -T 'application/json' -p execution_sleep.json  http://localhost:8080/api/workflow
done