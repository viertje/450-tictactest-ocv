# CI/CD-Pipeline

Kurzübersicht über die Pipeline von TicTacTest. Stand: 16.09.2026.

## Überblick

Die Pipeline besteht aus zwei GitHub-Actions-Workflows und einem eigenen
Container-Image in der GitHub Container Registry (GHCR).

```
.devcontainer/Dockerfile
        |  (Publish container image: baut, taggt vX.Y.Z, oeffnet Freigabe-PR)
        v
ghcr.io/viertje/450-tictactest-ocv:vX.Y.Z   (erst nach Merge der Freigabe-PR aktiv)
        |  (Build)
        v
./gradlew build  ->  JaCoCo-Coverage-Report (HTML, als Artifact)
```

| Baustein | Datei | Zweck |
|---|---|---|
| Image-Definition | `.devcontainer/Dockerfile` | Build- und Testumgebung: JDK 25 (Zulu) auf Alpine |
| Image publizieren + versionieren | `.github/workflows/dockerimageupdate.yml` | baut das Image, taggt es (inkl. Semantic Version), lädt es nach GHCR und eröffnet die Freigabe-PR |
| Projekt bauen | `.github/workflows/build.yml` | führt Build und Tests **im** Image aus, erzeugt den JaCoCo-Coverage-Report |
| Lokale Umgebung | `.devcontainer/devcontainer.json` | zieht dasselbe versionierte Image für VS Code / IntelliJ Dev Containers |

Details zur Versionierung und zum Freigabeprozess: siehe
`docs/DevContainer-Versioning.md`.

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
- **Job `publish`:** Login an `ghcr.io` mit `GITHUB_TOKEN` → Buildx einrichten → nächste Patch-Version anhand der vorhandenen `vX.Y.Z`-Git-Tags ermitteln → Image bauen und pushen → Git-Tag für die neue Version erstellen und pushen
- **Tags:** `:latest` und `:<commit-sha>` (Debugging), sowie `:vX.Y.Z` (die eigentlich massgebliche Version)
- **Job `open-release-pr`** (läuft nach `publish`): schliesst zuerst noch offene, ältere Freigabe-PRs automatisch (nur eine soll gleichzeitig offen sein), ersetzt dann den Image-Tag in `build.yml` und `devcontainer.json` durch die neue `:vX.Y.Z`-Version und eröffnet dafür automatisch einen Pull Request
- **`concurrency`:** verhindert, dass zwei Läufe gleichzeitig um `:latest` bzw. die nächste Versionsnummer konkurrieren

Wichtig: Ein neues Image ist damit *veröffentlicht* (in der GHCR vorhanden),
aber noch nicht *freigegeben* — `build.yml` und `devcontainer.json`
verwenden weiterhin die vorherige Version, bis die Freigabe-PR manuell
geprüft und gemergt wird. Das Mergen ist die offizielle Freigabe. Details:
`docs/DevContainer-Versioning.md`.

## Workflow 2: Build

- **Auslöser:** Push auf einen beliebigen Branch, Pull Request auf `main`, zusätzlich manuell
- **Rechte:** `packages: read` (um das private Image zu ziehen)
- **Umgebung:** Der Job läuft über `container:` direkt im Image aus GHCR, gepinnt auf eine konkrete `vX.Y.Z`-Version (nicht `:latest`)
- **Schritte:** Checkout → `chmod +x ./gradlew` → `./gradlew build --no-daemon` → JaCoCo-HTML-Report als Artifact hochladen

`./gradlew build` löst über `check` auch `test` aus. `build.gradle` hängt
`jacocoTestReport` per `finalizedBy` an `test`, wodurch nach jedem Testlauf
automatisch ein HTML-Coverage-Report unter `build/reports/jacoco/test/html/`
entsteht. Der Upload-Schritt (`actions/upload-artifact@v4`) läuft mit
`if: always()`, damit der Report auch bei fehlgeschlagenen Tests verfügbar
bleibt, und mit `if-no-files-found: error`, damit ein fehlender Report den
Lauf sichtbar scheitern lässt statt still zu bleiben.

Der Push-Trigger ist bewusst nicht mehr auf `main` beschränkt: Jeder Branch
soll unabhängig einen eigenen Coverage-Report erzeugen können (Auftrag 1,
Abnahmekriterium "Report für jeden Branch"). Das führt bei einem offenen
Pull Request zu zwei Läufen für denselben Commit (`push` und
`pull_request`) – akzeptiert als kleine Redundanz, siehe Bekannte
Einschränkungen.

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
automatisch — inklusive Versionierung, seit dessen Erweiterung um Job
`open-release-pr` (siehe `docs/DevContainer-Versioning.md`).

## Änderungen gegenüber der vorherigen Pipeline

| Vorher | Nachher |
|---|---|
| `actions/setup-java@v4` richtete JDK 25 bei jedem Lauf ein | Das JDK kommt aus dem eigenen Image; der Schritt entfiel |
| Umgebung wurde pro Lauf neu zusammengesetzt | Umgebung ist als versioniertes Image festgehalten |
| kein Container-Image | `.devcontainer/Dockerfile` plus Publish-Workflow |
| `.gitignore` schluckte `.devcontainer/` über die Regel `.*` | `!.devcontainer/` ergänzt, damit der Dockerfile versioniert wird |
| Dockerfile mit CRLF | auf LF umgestellt und über `.gitattributes` abgesichert |
| `./gradlew assemble` (nur kompilieren) | `./gradlew build` (kompiliert und testet) + JaCoCo-HTML-Report als Artifact |
| `build.yml` nur auf `main` | `build.yml` auf jedem Branch (push) |
| kein `devcontainer.json` | `devcontainer.json` mit VS-Code-Extensions, zieht das versionierte GHCR-Image |
| nur `:latest`/`:<sha>`, sofort aktiv überall | Semantic-Version-Tags (`vX.Y.Z`) + Freigabe-PR als Gate, siehe `docs/DevContainer-Versioning.md` |

## Bekannte Einschränkungen

1. ~~**Keine garantierte Reihenfolge.**~~ Durch die Versionierung faktisch
   behoben: `build.yml` und `devcontainer.json` sind auf eine konkrete
   `vX.Y.Z`-Version gepinnt und wechseln nur, wenn die Freigabe-PR gemergt
   wird — sie "rennen" nicht mehr hinter einem sich bewegenden `:latest`
   her. Ein Push, der den Dockerfile ändert, startet zwar weiterhin
   `build.yml` (Push-Trigger) parallel zu `dockerimageupdate.yml`, aber
   `build.yml` verwendet dabei einfach die zu diesem Zeitpunkt noch gültige,
   vorherige Version — kein Fehlerfall, sondern das gewünschte Verhalten.
2. **Zwei Läufe pro Pull-Request-Commit.** Weil sowohl `push` (jeder Branch) als auch `pull_request` (auf `main`) auslösen, läuft `build.yml` für denselben Commit zweimal, sobald ein Pull Request offen ist. Funktional unproblematisch, aber unnötiger Ressourcenverbrauch. Liesse sich beheben, indem der `pull_request`-Trigger entfernt wird (der `push`-Trigger deckt denselben Commit bereits ab, solange keine Forks verwendet werden).
3. **Coverage-Wert ist nicht maschinenlesbar.** Der JaCoCo-Report wird nur als HTML erzeugt (`xml.required = false`). Für den Coverage-Vergleich zwischen `main` und Branch (JaCoCo-Auftrag 3) wird zusätzlich ein XML- oder CSV-Report nötig sein.
4. ~~Mehrere offene Freigabe-PRs möglich.~~ **Automatisch behoben:** `open-release-pr` schliesst beim Eröffnen einer neuen Freigabe-PR alle noch offenen älteren automatisch. Details: `docs/DevContainer-Versioning.md`.
