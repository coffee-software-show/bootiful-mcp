#!/usr/bin/env bash

rm -rf target

./mvnw -DskipTests -Pnative native:compile 

