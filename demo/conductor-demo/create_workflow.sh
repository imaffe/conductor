#!/bin/bash

echo "The workflow file is $1"
curl -vX POST http://localhost:8080/api/metadata/workflow -H 'Content-Type: application/json' -d @$1
