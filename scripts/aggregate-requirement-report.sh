#!/usr/bin/env sh

ROOT_DIR="$(dirname "$0")/.."
TARGET_FILE="$(dirname "$0")/../build/reports/requirements/aggregate.csv"

mkdir -p "$(dirname "$TARGET_FILE")"
rm "$TARGET_FILE"

#for next_csv in $(find "$ROOT_DIR" -path "*/build/reports/requirements/*.csv") ; do
#    echo "$next_csv"
#done


i=0
find "$ROOT_DIR" -path "*/build/reports/requirements/*.csv" -print | while IFS="$(printf "\n")" read -r next_csv
do
    i=$((i + 1))

    # the first iteration keeps the header
    if [ ! $i -gt 1 ]; then
        cp "$next_csv" "$TARGET_FILE"
        continue
        echo "Yeah"
    fi

    tail -n +2 "$next_csv" >> "$TARGET_FILE"
done
