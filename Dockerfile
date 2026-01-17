# ============================
# 1. Build Stage
# ============================
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS build
# docker run --rm registry.access.redhat.com/ubi8/openjdk-21 id
# uid=185(jboss) gid=0(root) groups=0(root),185(jboss)

WORKDIR /app

COPY --chown=185:0 pom.xml .
# → Nur die pom.xml wird kopiert, damit Maven bereits alle Dependencies auflösen kann,
#   ohne dass sich der Sourcecode ändert. Das verbessert das Layer-Caching.

RUN --mount=type=cache,target=/home/jboss/.m2 mvn -q -DskipTests dependency:go-offline
# → Lädt alle Maven-Dependencies vorab herunter.
# → --mount=type=cache sorgt dafür, dass das lokale Maven-Repository zwischen Builds gecached wird.

COPY --chown=185:0 src ./src
# → Jetzt erst der Sourcecode, damit Änderungen am Code nicht das Dependency-Layer invalidieren.

RUN --mount=type=cache,target=/home/jboss/.m2 mvn -q -DskipTests package
# → Baut das eigentliche JAR.
# → Wieder mit Maven-Cache, um Build-Zeit zu sparen.

RUN cp target/kubeevent-0.0.1-SNAPSHOT.jar app.jar && \
    java -Djarmode=layertools -jar app.jar extract
# → Spring Boot Layertools extrahieren das JAR in einzelne Layer:
#     - dependencies
#     - spring-boot-loader
#     - snapshot-dependencies
#     - application
# → Vorteil: Docker kann diese Layer getrennt cachen → schnellere Deployments.

# ============================
# 2. Runtime Stage
# ============================
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

# OCI-konforme Labels
LABEL org.opencontainers.image.title="KubeEvent Java" \
      org.opencontainers.image.description="Kubernetes Event Watcher and Dashboard" \
      org.opencontainers.image.version="0.0.1-SNAPSHOT" \
      org.opencontainers.image.vendor="wlanboy" \
      org.opencontainers.image.source="https://github.com/example/kubeeventjava" \
      org.opencontainers.image.licenses="MIT"

WORKDIR /app

USER root
# → Temporär root, um Verzeichnisse anzulegen und Berechtigungen zu setzen.

RUN mkdir -p /app/config /app/data && \
    chown -R 185:0 /app && \
    chmod -R g+w /app
# → /app/config: für externe Konfigurationen
# → /app/data: für persistente Daten
# → chown: Eigentümer auf User 185 setzen, damit die App später ohne root läuft.
# → chmod g+w: Gruppe darf schreiben → wichtig für OpenShift/PodSecurity-Kontexte.

USER 185
# → Zurück zum nicht-privilegierten User.

COPY --from=build /app/dependencies/ ./
# → Kopiert nur die Dependency-Layer. Ändern sich selten.

COPY --from=build /app/spring-boot-loader/ ./
# → Enthält den Spring Boot Launcher (Main-Class Loader). Ändern sich selten.

COPY --from=build /app/snapshot-dependencies/ ./
# → Snapshot-Dependencies (z. B. lokale libs), ändern sich häufiger.

COPY --from=build /app/application/ ./
# → Der eigentliche Applikationscode (Kompilat). Ändert sich.

COPY --chown=185:0 containerconfig/application.properties /app/config/application.properties
# → Externe Konfiguration ins Config-Verzeichnis für die Referenz für ENV Vars

COPY --chown=185:0 entrypoint.sh /app/entrypoint.sh
# → Custom Entrypoint für Java OPTS.

EXPOSE 8080
# → Dokumentiert den Port, den die App verwendet (Spring Boot Default).

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
# → Nutzt den Spring Boot Actuator Health Endpoint.

ENTRYPOINT ["/app/entrypoint.sh"]
# → Startet die App über das Entry-Skript.
# → Vorteil: Skript kann Umgebungsvariablen verarbeiten, ENTRYPOINT nicht.
