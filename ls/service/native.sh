#!/usr/bin/env bash 

./mvnw -DskipTests -Pnative native:compile   
./target/service
