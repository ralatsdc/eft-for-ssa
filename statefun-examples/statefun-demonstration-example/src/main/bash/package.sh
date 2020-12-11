#!/usr/bin/env bash

set -e

# Work in statefun-demonstration-example directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
DIR="$( dirname "$( dirname "$( dirname $DIR )" )" )"
pushd $DIR

# Package this version
VER=1.0.2
git checkout v$VER
rm -rf processor-$VER
mkdir -p processor-$VER/checkpoint-dir

# Run Java build
mvn clean package -DskipTests

# Run Docker build
docker-compose down
docker-compose build --no-cache
docker tag statefun-demonstration-example_master:latest statefun-demonstration-example_master:$VER
docker tag statefun-demonstration-example_worker:latest statefun-demonstration-example_worker:$VER

echo "Saving master image ..."
docker save statefun-demonstration-example_master:$VER \
    | gzip > statefun-demonstration-example_master-$VER.tar.gz
mv statefun-demonstration-example_master-$VER.tar.gz processor-$VER

echo "Saving worker image ..."
docker save statefun-demonstration-example_worker:$VER \
    | gzip > statefun-demonstration-example_worker-$VER.tar.gz
mv statefun-demonstration-example_worker-$VER.tar.gz processor-$VER

echo "Saving kafka image ..."
docker save wurstmeister/kafka:2.12-2.0.1 \
    | gzip > wurstmeister-kafka-02.12-2.0.1.tar.gz
mv wurstmeister-kafka-02.12-2.0.1.tar.gz processor-$VER

echo "Saving zookeeper image ..."
docker save wurstmeister/zookeeper:latest \
    | gzip > wurstmeister-zookeeper-latest.tar.gz
mv wurstmeister-zookeeper-latest.tar.gz processor-$VER

# Move needed files into archive directory, and archive
cat docker-compose.yaml.deploy \
    | sed "s/<VER>/$VER/" > processor-$VER/docker-compose.yaml
cat load.sh.deploy \
    | sed "s/<VER>/$VER/" > processor-$VER/load.sh
cp docker-variables.env processor-$VER
cp src/resources/docker.properties processor-$VER
cp conf/flink-conf.yaml processor-$VER
cp -r ../../orekit-data processor-$VER
tar -czvf processor-$VER.tar.gz processor-$VER

popd
