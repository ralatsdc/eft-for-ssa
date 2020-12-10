set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
DIR="$( dirname "$( dirname "$( dirname $DIR )" )" )"
VER=1.0.2

pushd $DIR

git checkout v$VER

mkdir processor-$VER

mvn clean package -DskipTests

docker-compose down
docker-compose build --no-cache

echo "Saving master image ..."
docker save statefun-demonstration-example_master:latest \
    | gzip > statefun-demonstration-example_master-$VER.tar.gz
mv statefun-demonstration-example_master-$VER.tar.gz processor-$VER

echo "Saving worker image ..."
docker save statefun-demonstration-example_worker:latest \
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

cp docker-compose.yaml processor-$VER
cp docker-variables.env processor-$VER
cp src/resources/docker.properties processor-$VER
cp conf/flink-conf.yaml processor-$VER

cp -r ../../orekit-data processor-$VER

tar -czvf processor-$VER.tar.gz processor-$VER

popd
