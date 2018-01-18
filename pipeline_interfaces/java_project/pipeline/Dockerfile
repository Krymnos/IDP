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
ENTRYPOINT [ "sh", "-c", "/usr/bin/redis-server --daemonize yes && java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar $ARGUMENTS" ]
