#!/bin/bash

python recomputeFafbLandmarks.py

sed -e 's/^/"/g' -e 's/$/"/g' -e 's/,/","/g' world_tmp.txt > world_tmp_q.txt
awk -F, '{print $1 $2 $3 $4 $5}' ../lm-em-landmarks.csv > moving.txt
paste -d',' moving.txt world_tmp_q.txt > NEW-lm-em-landmarks.csv
