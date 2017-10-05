#!/bin/bash

for i in `seq 1 10`;
do
    echo "msg #${i}" 1>&2
    sleep 0.1
done