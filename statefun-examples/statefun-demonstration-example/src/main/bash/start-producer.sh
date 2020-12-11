#!/usr/bin/env bash

echo "producer started"
docker-compose exec kafka-broker kafka-console-producer.sh \
               --broker-list localhost:9092 \
               --topic tracks
