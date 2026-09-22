# CI/CD-Pipeline

Kurzübersicht über die Pipeline von TicTacTest. Stand: 22.09.2026.

## Überblick

Die Pipeline besteht aus zwei GitHub-Actions-Workflows, einem eigenen
Container-Image in der GitHub Container Registry (GHCR) und einer
Coverage-Time-Series auf GitHub Pages.

```
.devcontainer/Dockerfile
        |  (Publish container image: baut, taggt vX.Y.Z, oeffnet Freigabe-PR)
        v
ghcr.io/viertje/450-tictactest-ocv:vX.Y.Z   (erst nach Merge der Freigabe-PR aktiv)
        |  (Build)
        v
./gradlew build  ->  JaCoCo-Coverage-Report (HTML, als Artifact)
        |  (nur main, nur push)
        v
coverage-history.csv auf Branch gh-pages  ->  GitHub Pages (Chart.js)
```

| Baustein | Datei | Zweck |
|---|---|---|
| Image-Definition | `.devcontainer/Dockerfile` | Build- und Testumgebung: JDK 25 (Zulu) auf Alpine |
| Image publizieren + versionieren | `.github/workflows/dockerimageupdate.yml` | baut das Image, taggt es (inkl. Semantic Version), lädt es nach GHCR und eröffnet die Freigabe-PR |
| Projekt bauen | `.github/workflows/build.yml` | führt Build und Tests **im** Image aus, erzeugt den JaCoCo-Coverage-Report, pflegt auf `main` zusätzlich die Coverage-Historie |
| Coverage-Historie + Visualisierung | Branch `gh-pages` (`coverage-history.csv`, `index.html`) | von `build.yml` gepflegt, von GitHub Pages ausgeliefert |
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

**Noch offen:** `v1.0.0` wurde noch nie erzeugt, da der Workflow bisher nur
auf Dockerfile-Änderungen reagiert hat. Das erste Mal braucht einen manuellen
`workflow_dispatch`-Lauf ab dem Actions-Tab — siehe
`docs/DevContainer-Versioning.md`. Bis dahin zeigt `build.yml` weiterhin auf
`:latest`, nicht auf eine gepinnte Version.

## Workflow 2: Build

- **Auslöser:** Push auf einen beliebigen Branch ausser `gh-pages`, Pull Request auf `main`, zusätzlich manuell
- **Rechte:** `contents: write` (für den Push der Coverage-Historie nach `gh-pages`), `packages: read` (um das private Image zu ziehen)
- **Umgebung:** Der Job läuft über `container:` direkt im Image aus GHCR — aktuell noch `:latest`; wechselt automatisch auf eine gepinnte `vX.Y.Z`-Version, sobald die erste Freigabe-PR gemergt ist (siehe oben)
- **Schritte:** Checkout → `chmod +x ./gradlew` → `./gradlew build --no-daemon` → JaCoCo-HTML-Report als Artifact hochladen → (nur `main`, nur `push`) Line-Coverage aus dem XML-Report lesen → `gh-pages` auschecken → Zeile an `coverage-history.csv` anhängen, committen, pushen

`./gradlew build` löst über `check` auch `test` aus. `build.gradle` hängt
`jacocoTestReport` per `finalizedBy` an `test`, wodurch nach jedem Testlauf
automatisch ein HTML-Coverage-Report unter `build/reports/jacoco/test/html/`
entsteht. Der Upload-Schritt (`actions/upload-artifact@v4`) läuft mit
`if: always()`, damit der Report auch bei fehlgeschlagenen Tests verfügbar
bleibt, und mit `if-no-files-found: error`, damit ein fehlender Report den
Lauf sichtbar scheitern lässt statt still zu bleiben.

Der Push-Trigger ist bewusst nicht auf `main` beschränkt: Jeder Branch soll
unabhängig einen eigenen Coverage-Report erzeugen können (Auftrag 1,
Abnahmekriterium "Report für jeden Branch"). `gh-pages` ist explizit
ausgenommen (`branches-ignore`) – sonst würde der eigene Commit des
Coverage-Update-Schritts (siehe unten) den Java-Build erneut auslösen, auf
einem Branch ohne `gradlew`. Zusätzlich läuft `build.yml` bei einem offenen
Pull Request zweimal für denselben Commit (`push` und `pull_request`) –
akzeptiert als kleine Redundanz, siehe Bekannte Einschränkungen.

Zwei Details, die leicht Fehler verursachen:

- `runs-on: ubuntu-latest` bleibt bestehen. `container:` ersetzt den Runner nicht, sondern startet das Image darauf.
- Das Image setzt `USER dev`, der Runner legt das Arbeitsverzeichnis aber als `root` an. Ohne `options: --user root` bricht der Checkout mit „permission denied“ ab.

## Coverage-Time-Series auf GitHub Pages (Auftrag 2 / 2.5)

Design (Auftrag 2) und Umsetzung (Auftrag 2.5) der Zeitreihe:

- **Metrik:** Line Coverage (JaCoCo-`LINE`-Counter), weil sie sich am einfachsten
  interpretieren lässt ("X von Y Zeilen ausgeführt") – im Gegensatz z. B. zur
  bytecode-basierten Instruction Coverage.
- **Datenquelle:** `build.gradle` erzeugt seit Auftrag 2.5 zusätzlich zum
  HTML-Report einen XML-Report (`xml.required = true`, vorher `false`). Der
  Workflow liest daraus den **letzten** `<counter type="LINE" .../>`-Eintrag
  in `jacocoTestReport.xml` – das ist der Gesamt-Counter auf Report-Ebene
  (nach allen Package-Countern), nicht ein einzelnes Package.
- **Speicherort/-format:** `coverage-history.csv` auf einem eigenen,
  verwaisten (orphan) Branch `gh-pages` – bewusst getrennt vom
  Quellcode-Verlauf auf `main`. Format `Datum,Commit,Coverage`, wie im
  Auftrag vorgeschlagen.
- **Aktualisierung:** Ein Schritt am Ende von `build.yml`, nur bei
  `push` auf `main`, checkt `gh-pages` in einen Unterordner aus, hängt eine
  Zeile an, committet mit der Identität `github-actions[bot]` und pusht.
- **Visualisierung:** `index.html` auf `gh-pages`, lädt `coverage-history.csv`
  per `fetch` und zeichnet sie mit Chart.js (CDN) als Liniendiagramm; der
  Commit-Hash erscheint im Tooltip.
- **Veröffentlichung:** GitHub Pages, Quelle "Deploy from a branch" →
  `gh-pages` → `/ (root)`. **Aktiviert und bestätigt live** (2026-09-22):
  https://viertje.github.io/450-tictactest-ocv/ zeigt bereits einen echten
  Messwert (84.4 % für Commit `986f373`).

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
`open-release-pr` (siehe `docs/DevContainer-Versioning.md`). Die erste
Version (`v1.0.0`) muss dafür trotzdem einmal manuell angestossen werden —
siehe "Noch offen" oben.

Ebenfalls von Hand: der Branch `gh-pages` wurde einmalig mit einer leeren
`coverage-history.csv` (nur Kopfzeile) und `index.html` bootstrapped, weil
`build.yml` einen bestehenden `gh-pages`-Branch zum Auschecken erwartet.
Ab dem ersten Push auf `main` danach pflegt der Workflow die Datei selbst —
bestätigt funktionierend seit dem Merge von Auftrag 2.5.

## Änderungen gegenüber der vorherigen Pipeline

| Vorher | Nachher |
|---|---|
| `actions/setup-java@v4` richtete JDK 25 bei jedem Lauf ein | Das JDK kommt aus dem eigenen Image; der Schritt entfiel |
| Umgebung wurde pro Lauf neu zusammengesetzt | Umgebung ist als versioniertes Image festgehalten |
| kein Container-Image | `.devcontainer/Dockerfile` plus Publish-Workflow |
| `.gitignore` schluckte `.devcontainer/` über die Regel `.*` | `!.devcontainer/` ergänzt, damit der Dockerfile versioniert wird |
| Dockerfile mit CRLF | auf LF umgestellt und über `.gitattributes` abgesichert |
| `./gradlew assemble` (nur kompilieren) | `./gradlew build` (kompiliert und testet) + JaCoCo-HTML-Report als Artifact |
| `build.yml` nur auf `main` | `build.yml` auf jedem Branch ausser `gh-pages` (push) |
| Coverage nur als HTML (`xml.required = false`) | zusätzlich XML (`xml.required = true`) als Datenquelle für die Zeitreihe |
| keine Coverage-Historie | `coverage-history.csv` auf `gh-pages`, gepflegt bei jedem Push auf `main` |
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
   (Gilt vollständig erst, sobald `v1.0.0` gebootstrapped ist, siehe oben.)
2. **Zwei Läufe pro Pull-Request-Commit.** Weil sowohl `push` (jeder Branch ausser `gh-pages`) als auch `pull_request` (auf `main`) auslösen, läuft `build.yml` für denselben Commit zweimal, sobald ein Pull Request offen ist. Funktional unproblematisch, aber unnötiger Ressourcenverbrauch. Liesse sich beheben, indem der `pull_request`-Trigger entfernt wird (der `push`-Trigger deckt denselben Commit bereits ab, solange keine Forks verwendet werden).
3. ~~Coverage-Wert ist nicht maschinenlesbar.~~ **Behoben:** `xml.required = true` seit Auftrag 2.5, wird für die Coverage-Historie ausgewertet. Für den Coverage-Vergleich zwischen `main` und Branch (JaCoCo-Auftrag 3) reicht dieselbe XML-Quelle.
4. ~~Mehrere offene Freigabe-PRs möglich.~~ **Automatisch behoben:** `open-release-pr` schliesst beim Eröffnen einer neuen Freigabe-PR alle noch offenen älteren automatisch. Details: `docs/DevContainer-Versioning.md`.
5. ~~GitHub Pages musste manuell aktiviert werden.~~ **Erledigt und bestätigt** (2026-09-22) — siehe oben.
6. ~~Kein grüner Actions-Lauf für den Coverage-Update-Schritt bestätigt.~~ **Bestätigt funktionierend**: erster echter Lauf hat `coverage-history.csv` korrekt mit einer realen Zeile befüllt.
7. **`v1.0.0` (Versionierung) noch nicht gebootstrapped.** Braucht einen einmaligen manuellen `workflow_dispatch`-Lauf, siehe "Noch offen" oben und `docs/DevContainer-Versioning.md`.
