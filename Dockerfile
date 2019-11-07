FROM openjdk:11-slim

CMD ["/usr/local/openjdk-11/bin/java", "-jar", "/opt/census-rm-exception-manager.jar"]

RUN apt-get update && \
apt-get -yq install curl && \
apt-get -yq clean && \
rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=census-rm-exception-manager*.jar
COPY target/$JAR_FILE /opt/census-rm-exception-manager.jar
