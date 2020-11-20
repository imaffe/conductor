#!/bin/bash

curl -vX POST http://localhost:8080/api/workflow -H 'Content-Type: application/json' -d @$1

