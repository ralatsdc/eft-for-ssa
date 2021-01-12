#!/usr/bin/env bash

echo "consumer started"
docker-compose exec kafka-broker kafka-console-consumer.sh \
               --bootstrap-server localhost:9092 \
               --isolation-level read_committed \
               --topic default
