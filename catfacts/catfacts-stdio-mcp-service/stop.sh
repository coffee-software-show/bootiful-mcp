#!/usr/bin/env bash
lsof -i :8080 | cut -f 2 -d\   | while read l ; do kill $l ; done 