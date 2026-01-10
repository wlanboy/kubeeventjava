# ============================
# 1. Build Stage
# ============================
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

# ============================
# 2. Runtime Stage
# ============================
FROM eclipse-temurin:21-jre-alpine

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Create required directories
RUN mkdir -p /app/config /app/data && chown -R spring:spring /app

USER spring

# Copy built JAR
COPY config/application.properties /app/config/application.properties
COPY --from=build /app/target/kubeevent-0.0.1-SNAPSHOT.jar /app/kubeevent.jar

# Expose port
EXPOSE 8080

# ENTRYPOINT with external config
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/kubeevent.jar", "--spring.config.location=file:/app/config/application.properties"]
