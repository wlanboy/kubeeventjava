# ============================
# 1. Build Stage
# ============================
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS build

WORKDIR /app

# Erst nur die Dependencies
COPY --chown=185:0 pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline

# Dann erst der Sourcecode
COPY --chown=185:0 src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

# Wir behalten das JAR für das Training UND extrahieren die Layer
RUN cp target/kubeevent-0.0.1-SNAPSHOT.jar app.jar && \
    java -Djarmode=layertools -jar app.jar extract

# ============================
# 3. Runtime Stage
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

# Dependencies 
COPY --from=build /app/dependencies/ ./
COPY --from=build /app/spring-boot-loader/ ./
COPY --from=build /app/snapshot-dependencies/ ./

# Application
COPY --from=build /app/application/ ./

COPY config/application.properties /app/config/application.properties
COPY entrypoint.sh /app/entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]