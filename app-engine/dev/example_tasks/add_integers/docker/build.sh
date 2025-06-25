#!/bin/sh
docker build -t add_integers:latest .
docker save add_integers:latest -o ../image.tar