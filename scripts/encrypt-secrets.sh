#!/bin/bash
set -e
set -o pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)

# For each of our files in our encrypted config
for src_file in $(find $SCRIPT_DIR/../deploy/k8s -name *.enc.secrets) ; do
  echo "Encrypting: $src_file"
  sops --encrypt -i "$src_file"
done



