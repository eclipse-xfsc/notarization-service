#!/bin/bash

source ./readenv.sh

echo "Given variables:"
printenv | grep "NOT_API_"
