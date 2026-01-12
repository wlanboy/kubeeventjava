#!/bin/sh
set -e

# Wir nutzen exec, damit Java die PID 1 übernimmt.
# Dies ist wichtig für das Signal-Handling (z.B. in Kubernetes).
exec java \
  -Djava.security.egd=file:/dev/./urandom \
  -XX:SharedArchiveFile=application.jsa \
  -XX:MaxRAMPercentage=50 \
  -XX:InitialRAMPercentage=30 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ExplicitGCInvokesConcurrent \
  -XX:+ExitOnOutOfMemoryError \
  org.springframework.boot.loader.launch.JarLauncher \
  --spring.config.location=file:/app/config/application.properties