FROM mlaccetti/docker-oracle-java8-ubuntu-16.04

# Update the repository and install Redis Server
RUN         apt-get update && apt-get install -y redis-server
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

## pipeline stuff
ADD resources /resources
ADD target/pipeline-0.1-jar-with-dependencies.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENV ARGUMENTS=""

#configfile where our environment variables are set
#ADD resources/provenance_docker.properties resources/provenance_docker.properties
ENV CONFIGFILE="--propertiesfile resources/provenance_docker.properties"


ENV NODE_ID=00000
ENV NODE_NAME=UNKNOWN_NODE
ENV SINK_NAME=cassandra
ENV CASSANDRA_HOST=127.0.0.1
ENV CASSANDRA_PORT=9042
ENV CASSANDRA_REPLICATION_STRATEGY=SimpleStrategy
ENV CASSANDRA_REPLICATION_FACTOR=1
ENV NODE_SUCCESSOR=0000
ENV BUFFER_CAPACITY=10
ENV PROVENANCE_KEYSPACE_NAME=provenancekey
ENV PROVENANCE_TABLE=provenancetable
ENV METRICS=meterid,metricid,loc,line,class,app,ctime,stime,rtime

ENTRYPOINT [ "sh", "-c", "/usr/bin/redis-server --daemonize yes && java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar $ARGUMENTS $CONFIGFILE" ]
