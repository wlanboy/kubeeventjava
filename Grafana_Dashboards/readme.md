# Grafana Dashboards – Kubernetes Event Monitoring

Alle Dashboards nutzen Prometheus als Datenquelle und basieren auf der `kubeevents_*`-Metrikfamilie, die vom kubeeventjava-Exporter bereitgestellt wird.

---

## Übersicht der Dashboards

### Kubernetes_Event_Monitoring.json

Globale Cluster-Übersicht

Einstiegspunkt für das gesamte Monitoring. Zeigt alle Kubernetes-Events clusterübergreifend mit Spike-Erkennung.

- Gesamtanzahl Events (Stat)
- Events pro Sekunde mit Spike-Indikator (Timeseries)
- Events nach Typ (Normal / Warning) (Timeseries)
- Namespace × Typ Matrix (Timeseries)
- Warning-Rate pro Namespace mit Spike-Erkennung (Timeseries)

---

### Namespace Detail.json

Drill-down auf einen einzelnen Namespace

Detailansicht für einen ausgewählten Namespace mit Aufschlüsselung nach Typ, Grund und Node.

- Gesamtanzahl Events im Namespace (Stat)
- Events pro Sekunde (Timeseries)
- Events nach Typ (Timeseries)
- Spike-Erkennung (Timeseries)
- Top Event-Gründe im Namespace (Timeseries + Tabellenlegende)
- Events nach Node/Host (Timeseries)

---

### Deployment_Detail.json

Detailansicht für ein einzelnes Deployment

Fokussiert auf Events eines spezifischen Deployments inkl. zugehöriger ReplicaSets und Pods.

- Gesamtanzahl Events für das Deployment (Stat)
- Events pro Sekunde (Timeseries)
- ReplicaSet Events (Timeseries)
- Pod CrashLoop / BackOff (Deployment-Pods) (Timeseries)
- Pod Scheduling-Fehler (FailedScheduling) (Timeseries)

---

### Pod_Detail.json

Detailansicht für einen einzelnen Pod

Granulares Monitoring auf Pod-Ebene mit Fokus auf Fehlerursachen.

- Gesamtanzahl Events für den Pod (Stat)
- Events pro Sekunde (Timeseries)
- Events nach Grund (Timeseries)
- Events nach Komponente (Timeseries)
- CrashLoop / BackOff (Timeseries)

---

### Node_Detail.json

Detailansicht für einen einzelnen Node

Monitoring der Node-Gesundheit und aller Events, die einem bestimmten Node zugeordnet sind.

- Gesamtanzahl Events auf dem Node (Stat)
- Events pro Sekunde (Timeseries)
- Node-Object-Events (Cluster-Gesundheit) (Timeseries)
- Events nach Komponente auf dem Node (Timeseries)
- Events nach Kind auf dem Node (Timeseries)
- Events nach Grund auf dem Node (Timeseries)

---

### Workload_Analysis.json

Workload-Analyse über Pods und Deployments

Vergleichende Analyse der Workload-Gesundheit mit 5-Minuten-Anstiegsrate als Indikator.

- Events pro Pod (5m-Anstieg) (Timeseries)
- Warnings pro Pod (5m-Anstieg) (Timeseries)
- Events pro Deployment (5m-Anstieg) (Timeseries)
- Deployment Normal vs. Warning (5m-Anstieg) (Timeseries)
- Pod CrashLoop / BackOff (5m-Anstieg) (Timeseries)

---

### Storage_Detail.json

PersistentVolumeClaim (PVC) Monitoring

Spezialisiertes Dashboard für Storage-Events mit Fokus auf kritische PVC-Fehler.

- Gesamtanzahl PVC Events (Stat)
- Warning Events (Stat, orange hervorgehoben)
- PVC Events pro Sekunde (Timeseries)
- PVC Events nach Typ (Normal vs. Warning) (Timeseries)
- Kritische Storage-Event-Gründe (Timeseries)
