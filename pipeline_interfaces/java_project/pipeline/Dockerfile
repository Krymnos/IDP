FROM mlaccetti/docker-oracle-java8-ubuntu-16.04

# Update the repository and install Redis Server
RUN         apt-get update && apt-get install -y redis-server
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

## pipeline stuff
ADD target/pipeline-0.1-jar-with-dependencies.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENV ARGUMENTS=""

# default values for provenance system
ENV CONF_LOC=EVAR
ENV NODE_ID=000000
ENV NODE_NAME=NONAME
ENV SUCCESSOR=000000
ENV NEIGHBOURS=000000:127.0.0.1
ENV SINK=cassandra
ENV CASSANDRA_HOST=127.0.0.1
ENV CASSANDRA_PORT=9042
ENV CASSANDRA_KEYSPACE_NAME=provenancekey
ENV CASSANDRA_TABLE_NAME=provenancetable
ENV CASSANDRA_REPLICATION_STRATEGY=SimpleStrategy
ENV CASSANDRA_REPLICATION_FACTOR=1
ENV BUFFER_CAPACITY=10
ENV METRICS=meterid,metricid,loc,line,class,app,ctime,stime,rtime

ENTRYPOINT [ "sh", "-c", "/usr/bin/redis-server --daemonize yes && java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar $ARGUMENTS $CONFIGFILE" ]
