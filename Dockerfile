FROM openjdk:11-slim

ARG JAR_FILE=census-rm-exception-manager*.jar
COPY target/$JAR_FILE /opt/census-rm-exception-manager.jar

RUN apt-get update
RUN apt-get -yq install curl
RUN apt-get -yq clean

CMD exec /usr/local/openjdk-11/bin/java $JAVA_OPTS -jar /opt/census-rm-case-api.jar
