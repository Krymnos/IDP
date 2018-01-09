FROM  phusion/baseimage:0.9.17

# Update the repository and install Redis Server
RUN         apt-get update && apt-get install -y redis-server

## install java
RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
RUN apt-get -y update
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y -q python-software-properties software-properties-common

ENV JAVA_VER 8
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

RUN echo 'deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
    echo 'deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main' >> /etc/apt/sources.list && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys C2518248EEA14886 && \
    apt-get update && \
    echo oracle-java${JAVA_VER}-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections && \
    apt-get install -y --force-yes --no-install-recommends oracle-java${JAVA_VER}-installer oracle-java${JAVA_VER}-set-default && \
    apt-get clean && \
    rm -rf /var/cache/oracle-jdk${JAVA_VER}-installer

RUN update-java-alternatives -s java-8-oracle

RUN echo "export JAVA_HOME=/usr/lib/jvm/java-8-oracle" >> ~/.bashrc
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*


## pipeline stuff
ADD resources /resources
ADD target/pipeline-0.1-jar-with-dependencies.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS=""
ENV ARGUMENTS=""
ENTRYPOINT [ "sh", "-c", "/usr/bin/redis-server --daemonize yes && java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar $ARGUMENTS" ]


