# TicTacTest

Tic-Tac-Toe als Konsolenspiel in Java. Übungsprojekt für die Module M450 (Applikationen testen)
und M324 (DevOps) an der BBW.

## Spiel starten

Voraussetzung: **Java 25 oder neuer**. Das fertige JAR liegt auf der Seite *Releases* dieses
Repositories:

```bash
java -jar tictactest-<version>.jar
```

## Bauen und testen

```bash
./gradlew build     # Unit-, E2E- und Architekturtests, Coverage-Gate (mindestens 90 % Branch Coverage)
./gradlew pitest    # Mutation Testing (mindestens 90 % Mutation Coverage)
```

Die CI führt beides bei jedem Push und Pull Request aus; die Reports liegen als Artifacts am
jeweiligen Lauf. Der Verlauf der Coverage steht auf https://viertje.github.io/450-tictactest-ocv/.

## Dokumentation

| Dokument | Inhalt |
|---|---|
| `docs/Testkonzept.md` | Testziele, Teststrategie, Testfälle, offene Punkte |
| `docs/CI-CD-Pipeline.md` | die GitHub-Actions-Workflows |
| `docs/DevContainer-Versioning.md` | Dev-Container-Image und seine Versionierung |
| `RELEASE.md` | Release-Prozess und Versionierung |
| `CHANGELOG.md` | Änderungen pro Version |
