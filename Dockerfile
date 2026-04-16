FROM eclipse-temurin:17-jre-alpine

CMD ["java", "-jar", "/opt/census-rm-exception-manager.jar"]
RUN addgroup --gid 1000 exceptionmanager && \
    adduser --system --uid 1000 exceptionmanager exceptionmanager
USER exceptionmanager

COPY target/census-rm-exception-manager*.jar /opt/census-rm-exception-manager.jar
