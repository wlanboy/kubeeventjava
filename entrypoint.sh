#!/bin/sh
# → Standard-Shebang für POSIX‑kompatible Shells. 
#   /bin/sh ist bewusst gewählt, weil es in minimalen Container-Images garantiert vorhanden ist.

set -e
# → Beendet das Skript sofort, wenn ein Befehl einen Fehler zurückgibt.
#   Wichtig in Containern: Ein fehlerhafter Start soll nicht zu einem "halb gestarteten" Zustand führen.

# Wir nutzen exec, damit Java die PID 1 übernimmt.
# Dies ist wichtig für das Signal-Handling (z.B. in Kubernetes).
# → exec ersetzt den aktuellen Shell-Prozess durch den Java-Prozess.
# → Dadurch wird Java PID 1 im Container.

exec java \
  -Djava.security.egd=file:/dev/./urandom \
  # → Beschleunigt die Initialisierung kryptografischer Funktionen.
  #   /dev/urandom ist schneller als /dev/random und völlig ausreichend für Java-Apps.

  -XX:MaxRAMPercentage=50 \
  # → Java nutzt maximal 50% des verfügbaren Container-RAMs.

  -XX:InitialRAMPercentage=30 \
  # → Startet die JVM mit 30% des verfügbaren RAMs.
  #   Beschleunigt Startup, weil weniger dynamisch nachallokiert werden muss.

  -XX:+UseG1GC \
  # → Aktiviert den G1 Garbage Collector.
  #   Optimal für Server-Anwendungen mit niedrigen Latenzanforderungen.

  -XX:MaxGCPauseMillis=200 \
  # → Zielwert für maximale GC-Pause.

  -XX:+ExplicitGCInvokesConcurrent \
  # → Falls irgendwo System.gc() aufgerufen wird, führt G1 ihn nicht stop-the-world aus,
  #   sondern parallel → bessere Latenz.

  -XX:+ExitOnOutOfMemoryError \
  # → JVM beendet sich sofort bei OOM.
  #   Wichtig: Kubernetes kann den Pod dann sauber neustarten.
  #   Ohne diese Option würde die JVM manchmal "weiterlaufen", aber in einem kaputten Zustand.

  org.springframework.boot.loader.launch.JarLauncher \
  # → Startet die Spring-Boot-Anwendung aus den extrahierten Layern.
  #   JarLauncher ist notwendig, weil wir kein fat JAR mehr starten, sondern die Layer-Struktur.

  --spring.config.location=file:/app/config/application.properties
  # → Erzwingt, dass Spring Boot die externe Config-Datei nutzt.
  