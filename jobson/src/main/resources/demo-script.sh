#!/bin/bash

echo "Jobson can run *any* application. This demo is launching a bash script"
sleep 1
echo ""
echo "At runtime, Jobson created a clean working directory at $(pwd)"
echo "Arguments and dependencies get copied to that directory"
echo ""

echo "Jobson captures stdout"
sleep 3
echo "Jobson also captures stderr" 1>&2
sleep 3

echo "And provides support for capturing user inputs (as expectedInputs)"
echo "The inputs can be routed to the actual application (e.g. this script)"
echo ""
echo "The user's inputs:"
echo ""
echo "First name: $1"
echo "Json of favorite foods: $2"
echo "Json of favorite foods, written to a file: $3"
echo "Favorite color: $4"
echo ""

echo "Jobson copies output files to persistence after the application finishes."
echo "I didn't want to risk a library problem, so this output is just copied from the dependencies"
mv demo-dependency output

sleep 2
echo "The output has been 'produced' by the demo script, after this script exits, Jobson will copy it to its file storage"

echo "Execution finishes in 5..."
sleep 1
echo "4..."
sleep 1
echo "3..."
sleep 1
echo "2..."
sleep 1
echo "1..."
sleep 1
