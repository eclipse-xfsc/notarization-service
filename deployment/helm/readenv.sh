#!/bin/bash

# Safely load the values in the .env file:
while IFS== read -r key value; do
    # TEMP_VAR=$(printf -v "$key" %s "$value")
    printf -v "$key" %s "$value" && export "$key"
done < <(grep -v '^#' .env | grep '=')
# done < .env
