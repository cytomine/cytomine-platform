#!/bin/sh
docker run -v $(pwd)/inputs:/inputs -v $(pwd)/outputs:/outputs -it add_integers:latest