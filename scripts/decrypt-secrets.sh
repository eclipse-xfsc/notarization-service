#!/bin/bash
set -e
set -o pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd)

# For each of our files in our encrypted config
for src_file in $(find $SCRIPT_DIR/../deploy/k8s -name *.enc.secrets) ; do
  # echo "Decrypting: $src_file"
  DIR=${src_file%/*}
  NEW_FILE="$(basename -- $src_file .enc.secrets).secrets"
  echo "Decrypting: $src_file to $DIR/$NEW_FILE"
  sops --decrypt "$src_file"
  sops --decrypt "$src_file" > "$DIR/$NEW_FILE"
done



