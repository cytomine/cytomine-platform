#!/bin/sh

INPUT_FOLDER=/inputs
OUTPUT_FOLDER=/outputs

INPUT_A=$(cat $INPUT_FOLDER/a)
INPUT_B=$(cat $INPUT_FOLDER/b)

printf $(expr $INPUT_A + $INPUT_B) > $OUTPUT_FOLDER/sum