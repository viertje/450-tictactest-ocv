# CI/CD-Pipeline

Kurzübersicht über die Pipeline von TicTacTest. Stand: 09.09.2026.

## Überblick

Die Pipeline besteht aus zwei GitHub-Actions-Workflows und einem eigenen
Container-Image in der GitHub Container Registry (GHCR).

```
.devcontainer/Dockerfile
        |  (Publish container image)
        v
ghcr.io/viertje/450-tictactest-ocv:latest
        |  (Build)
        v
./gradlew assemble
```

| Baustein | Datei | Zweck |
|---|---|---|
| Image-Definition | `.devcontainer/Dockerfile` | Build- und Testumgebung: JDK 25 (Zulu) auf Alpine |
| Image publizieren | `.github/workflows/dockerimageupdate.yml` | baut das Image und lädt es nach GHCR |
| Projekt bauen | `.github/workflows/build.yml` | führt den Gradle-Build **im** Image aus |

## Das Image

Basis ist `azul/zulu-openjdk-alpine:25`. Die Wahl ist nicht beliebig:
`gradle/gradle-daemon-jvm.properties` verlangt `toolchainVendor=AZUL` und
`toolchainVersion=25`. Weil das Image genau das mitbringt, lädt Gradle keine
eigene JVM nach – was auf Alpine (musl) auch scheitern würde, da die dort
hinterlegten Downloads glibc-Builds sind.

Zusätzlich enthält der Dockerfile das Label
`org.opencontainers.image.source`. Es verknüpft das Image mit dem Repository.
Ohne diese Verknüpfung erhält der `GITHUB_TOKEN` des Workflows keinen Zugriff
auf das (standardmässig private) Package.

## Workflow 1: Publish container image

- **Auslöser:** Push auf `main`, wenn `.devcontainer/Dockerfile` geändert wurde; zusätzlich manuell über `workflow_dispatch`
- **Rechte:** `packages: write`
- **Ablauf:** Login an `ghcr.io` mit `GITHUB_TOKEN` → Buildx einrichten → Image bauen und pushen
- **Tags:** `:latest` (wird von `build.yml` konsumiert) und `:<commit-sha>` als unveränderlicher Tag zur Fehlersuche
- **`concurrency`:** verhindert, dass zwei Läufe gleichzeitig `:latest` überschreiben

## Workflow 2: Build

- **Auslöser:** Push und Pull Request auf `main`, zusätzlich manuell
- **Rechte:** `packages: read` (um das private Image zu ziehen)
- **Umgebung:** Der Job läuft über `container:` direkt im Image aus GHCR
- **Schritte:** Checkout → `chmod +x ./gradlew` → `./gradlew assemble --no-daemon`

Zwei Details, die leicht Fehler verursachen:

- `runs-on: ubuntu-latest` bleibt bestehen. `container:` ersetzt den Runner nicht, sondern startet das Image darauf.
- Das Image setzt `USER dev`, der Runner legt das Arbeitsverzeichnis aber als `root` an. Ohne `options: --user root` bricht der Checkout mit „permission denied“ ab.

## Einmalige manuelle Inbetriebnahme

Schritt 1 und 2 der Aufgabe wurden von Hand ausgeführt:

```bash
docker build -t ghcr.io/viertje/450-tictactest-ocv:latest -f .devcontainer/Dockerfile .
docker login ghcr.io -u viertje          # Passwort: klassischer PAT mit write:packages
docker push ghcr.io/viertje/450-tictactest-ocv:latest
```

GHCR akzeptiert nur **klassische** Personal Access Tokens; fein granulare Tokens
funktionieren nicht. Seit Workflow 1 existiert, geschieht dieser Schritt
automatisch.

## Änderungen gegenüber der vorherigen Pipeline

| Vorher | Nachher |
|---|---|
| `actions/setup-java@v4` richtete JDK 25 bei jedem Lauf ein | Das JDK kommt aus dem eigenen Image; der Schritt entfiel |
| Umgebung wurde pro Lauf neu zusammengesetzt | Umgebung ist als versioniertes Image festgehalten |
| kein Container-Image | `.devcontainer/Dockerfile` plus Publish-Workflow |
| `.gitignore` schluckte `.devcontainer/` über die Regel `.*` | `!.devcontainer/` ergänzt, damit der Dockerfile versioniert wird |
| Dockerfile mit CRLF | auf LF umgestellt und über `.gitattributes` abgesichert |

## Bekannte Einschränkungen

1. **Keine garantierte Reihenfolge.** Ändert ein Push den Dockerfile, starten beide Workflows gleichzeitig. `build.yml` zieht dann noch das alte `:latest`. Eine echte Reihenfolge wäre nur mit `needs:` innerhalb eines einzigen Workflows möglich. Praktisch bedeutet das eine Verzögerung um einen Commit.
2. **Die Pipeline führt keine Tests aus.** `assemble` kompiliert und paketiert nur. Für die Tests wäre `./gradlew build` oder ein eigener `test`-Job nötig. Siehe auch `docs/Testkonzept.md`, Abschnitt 6.
