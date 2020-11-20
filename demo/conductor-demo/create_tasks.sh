#!/bin/bash

curl -vX POST http://localhost:8080/api/metadata/taskdefs -H 'Content-Type: application/json' -d @tasks.json
