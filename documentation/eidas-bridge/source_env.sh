#!/bin/bash

# Make the environment variables available to the calling script.
if [ -f .env ]; then
    source .env
else
    echo "Cannot source .env. File not found. Copy or move '.env.example' as reference."
    exit 1
fi
