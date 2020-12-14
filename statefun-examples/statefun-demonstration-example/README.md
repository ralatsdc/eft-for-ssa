Stream Processor for Space Fence Data Archive
=============================================

The processor-x.y.z.tar.gz archive contains an Apache Flink based
stream processor for Space Fence Type4 metric observation messages.

The processor consumes each message from a Kafka "tracks" topic, then
uses Orekit to create an orbit corresponding to each message, and
correlates this orbit to any previously created orbits. The processor
retains the orbit created with the maximum number of tracks, and
retains orbits created with a specified number of tracks, or
fewer. All orbits are removed after a configurable period. The
processor produces log messages to a Kafka "default" topic.


Archive Requests
----------------

To request an archive, navigate in a browser to transfer.ll.mit.edu,
then login using your laboratory single sign on credentials. Select
"All Files" in the left panel, select "My Folder", select "... More",
select "Request files to folder", then in the "Request file from"
field enter "raymond.leclair@springbok.io", in the "Subject field"
enter the required version, or "latest", then select "Send request".


Archive Contents
----------------

Specifically, the archive contains:

+ Saved Docker images

  - statefun-demonstration-example_master-x.y.z.tar.gz
  - statefun-demonstration-example_worker-x.y.z.tar.gz
  - wurstmeister-kafka-2.12-2.0.1.tar.gz
  - wurstmeister-zookeeper-latest.tar.gz

+ Configuration files:

  - docker-compose.yaml
  - docker-variables.env
  - docker.properties - Set processing properties here
  - flink-conf.yaml

+ Required directories

  - checkpoint-dir
  - orekit-data


Installation
------------

To install, extract the archive, then load the docker images:

    $ docker load < statefun-demonstration-example_master-1.0.1.tar.gz
    $ docker load < statefun-demonstration-example_worker-1.0.1.tar.gz
    $ docker load < wurstmeister-kafka-2.12-2.0.1.tar.gz
    $ docker load < wurstmeister-zookeeper-latest.tar.gz

Or, use the install-images.sh script.


Processing
----------

1) Use docker-compose in the directory containing the extracted
archive to run the docker images:

    $ docker-compose up -–scale worker=2

Note that the number of workers and the value of the
"parallelism.default" property in the flink-conf.yaml must agree.

Or, use the start-images.sh script.

2) Use docker-compose in the archive directory to run a console
consumer:

    $ docker-compose exec kafka-broker kafka-console-consumer.sh \
          --bootstrap-server localhost:9092 \
          --isolation-level read_committed \
          --from-beginning \
          --topic default

All messages sent to the Kafka "default" topic, used for logging, will
be echoed to stdout.

Or, use the start-consumer.sh script.

3) The processor and consumer are stopped using a keybord
interrupt. The use docker-compose in the directory containing the
extracted archive to stop the docker images:

    $ docker-compose down

Or, use the stop-images.sh script.


Versions
--------

Note that Docker Engine 19.03.12, Docker Compose 1.27.2, and Java
1.8.0_241 were used on macOS 10.15.6 to build the artifacts, and test.


Questions
---------

For questions, contact Raymond LeClair at 978-621-5755, or
raymond.leclair@springbok.io.
