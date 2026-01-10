# ============================
# 1. Build Stage
# ============================
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS build

WORKDIR /app

# Erst nur die Dependencies
COPY --chown=185:0 pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Dann erst der Sourcecode
COPY --chown=185:0 src ./src
RUN mvn -q -DskipTests package

# ============================
# 2. Runtime Stage
# ============================
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

WORKDIR /app

# UBI Images bringen bereits einen Standard-User (UID 185) mit, 
# der für den Betrieb von Java-Apps optimiert ist. 

USER root
RUN mkdir -p /app/config /app/data && \
    chown -R 185:0 /app && \
    chmod -R g+w /app

USER 185

COPY --from=build /app/target/kubeevent-0.0.1-SNAPSHOT.jar /app/kubeevent.jar
COPY config/application.properties /app/config/application.properties

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/kubeevent.jar", "--spring.config.location=file:/app/config/application.properties"]
