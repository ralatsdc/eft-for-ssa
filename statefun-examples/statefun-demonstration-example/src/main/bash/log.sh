#!/bin/bash

timestamp=`date "+%Y-%m-%dT%H:%M:%S"`
consumer='nohup docker-compose exec kafka-broker kafka-console-consumer.sh --bootstrap-server localhost:9092 --isolation-level read_committed --topic default'

$($consumer 2>&1 | grep 'Created track for id' > consumer-$timestamp-created-track.log &)
$($consumer 2>&1 | grep 'Created orbit for id' > consumer-$timestamp-created-orbit.log &)
$($consumer 2>&1 | grep 'Correlated orbits with ids' > consumer-$timestamp-correlated-orbits.log &)
$($consumer 2>&1 | grep 'Refined orbits with id' > consumer-$timestamp-refined-orbits.log &)
