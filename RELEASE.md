# Release-Prozess TicTacTest

Ein Release ist eine bewusst veröffentlichte, nachvollziehbare Version von
TicTacTest: ein Git-Tag auf einem bestimmten Commit, ein ausführbares JAR und
ein Changelog-Eintrag. Die Frage, die dieser Prozess beantwortet: *Wie entsteht
aus einem bestimmten Stand des Repositories reproduzierbar und kontrolliert eine
veröffentlichte Version?* — Antwort: durch einen Tag auf diesem Stand. Der Tag
ist der einzige Auslöser und gleichzeitig die einzige Quelle der Versionsnummer.

## Überblick

```
Änderungen per Pull Request nach main
        |
CHANGELOG.md aktualisieren (Eintrag "## X.Y.Z - Datum") und nach main mergen
        |
Version festlegen -> Tag vX.Y.Z auf dem aktuellen main-Commit erstellen und pushen
        |
GitHub Actions "Release" startet (nur wegen des Tags)
        |  Job verify:  Commit liegt auf main? Changelog-Eintrag vorhanden?
        |  Job build:   alle Tests, danach JAR bauen (im gepinnten Dev-Container-Image)
        |  Job release: GitHub Release mit JAR und Release Notes erstellen
        v
JAR von der Release-Seite herunterladen und starten
```

## Fragen und Antworten

**Wann wird ein Release erstellt?**
Wenn sich auf `main` ein sinnvoller, in sich abgeschlossener Stand angesammelt
hat (neue Funktion, Fehlerbehebung) und `main` grün ist. Nicht nach jedem Merge.

**Wer beziehungsweise was startet einen Release?**
Nur eine Person, durch das Pushen eines Tags `vMAJOR.MINOR.PATCH`. Der Workflow
`.github/workflows/release.yml` reagiert ausschliesslich darauf.

**Warum löst nicht jeder Push auf `main` einen Release aus?**
Ein Release ist eine Aussage ("diese Version ist bereit"), kein Nebenprodukt von
CI. Würde jeder Push einen Release erzeugen, gäbe es Dutzende Versionen ohne
Bedeutung, und Zwischenstände mit halbfertigen Funktionen wären öffentlich
herunterladbar. Pushes auf `main` werden von `build.yml` getestet, mehr nicht.

**Wie wird die Versionsnummer festgelegt?**
Semantic Versioning `MAJOR.MINOR.PATCH`, angewendet auf TicTacTest:

| Stufe | Wann | Beispiel |
|---|---|---|
| **PATCH** | Fehlerbehebung, Spielverhalten und Bedienung bleiben gleich | Absturz bei ungültiger Eingabe behoben |
| **MINOR** | neue Funktion, bisherige Bedienung funktioniert weiterhin | neuer Spielertyp |
| **MAJOR** | Bedienung oder Regeln ändern sich so, dass bisherige Nutzung nicht mehr passt | neues Eingabeformat für Züge, anderer Start-Befehl |

Die Versionsnummer steht an genau einer Stelle: im **Git-Tag**. Der Workflow
leitet daraus `-PreleaseVersion=X.Y.Z` für Gradle ab; `build.gradle` enthält
bewusst keine fest eingetragene Version, die mit dem Tag auseinanderlaufen
könnte. Lokale Builds ohne diesen Parameter heissen `0.0.0-SNAPSHOT`.

**Vor jedem Release zu prüfen**

- [ ] Alle vorgesehenen Änderungen sind auf `main` gemergt.
- [ ] Der letzte Build auf `main` ist grün (Tab *Actions*).
- [ ] `CHANGELOG.md` enthält einen Eintrag `## X.Y.Z - JJJJ-MM-TT` und liegt auf `main`.
- [ ] Die Versionsnummer ist gemäss obiger Tabelle gewählt und noch nicht vergeben (`git tag -l`).

Die letzten beiden Punkte prüft der Job `verify` zusätzlich automatisch.

**Wie wird ein Release-Tag erstellt?**

```bash
git checkout main
git pull
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

**Welcher Commit gehört zu einem Release?**
Der Commit, auf den der Tag zeigt (`git rev-parse v1.0.0^{commit}`). Auf der
GitHub-Release-Seite steht dieser Commit ebenfalls, zusätzlich in den Release
Notes. Der Job `verify` bricht ab, falls dieser Commit nicht auf `main` liegt.

**Wie wird das JAR gebaut?**
Vom Workflow im selben Dev-Container-Image wie die normale CI (Job `build`):
`./gradlew build -PreleaseVersion=X.Y.Z`. Das Gradle-`jar`-Target erzeugt
`build/libs/tictactest-X.Y.Z.jar` mit `Main-Class` im Manifest. Lokal ohne
Version: `./gradlew jar`.

**Voraussetzung zum Starten: Java 25 oder neuer.** Das Projekt wird mit dem
Java-25-Toolchain kompiliert (`gradle/gradle-daemon-jvm.properties`); auf einer
älteren Java-Version startet das JAR mit `UnsupportedClassVersionError`.

**Wo kann das JAR heruntergeladen werden?**
Auf der Seite *Releases* des GitHub-Repositories, beim jeweiligen Release unter *Assets*.

**Was enthält das Changelog?**
Pro Version einen Abschnitt `## X.Y.Z - Datum` mit den Rubriken *Added*,
*Changed* und *Fixed*, geschrieben aus Sicht von Spielenden und Entwickelnden —
keine Liste von Commit-Meldungen. Der Abschnitt wird automatisch zu den Release
Notes des GitHub Releases.

**Was passiert, wenn beim Release ein Test fehlschlägt?**
Der Job `build` bricht ab, der Job `release` läuft nicht (`needs`). Es entsteht
kein GitHub Release und kein JAR. Der Tag bleibt jedoch bestehen. Korrigieren:

```bash
git push origin --delete v1.0.0     # Tag auf GitHub entfernen
git tag -d v1.0.0                   # Tag lokal entfernen
# Fehler beheben, nach main mergen, dann Tag neu setzen und pushen
```

## Release testen

Ein Release gilt erst als erfolgreich, wenn das veröffentlichte JAR funktioniert:
JAR von der Release-Seite herunterladen und ohne Quellcode starten (mit
installiertem Java 25 oder neuer).

```bash
java -jar tictactest-1.0.0.jar
```

## Abgrenzung zu den DevContainer-Tags

Das Dev-Container-Image hat eine eigene, automatische Versionierung
(`docs/DevContainer-Versioning.md`). Seine Git-Tags heissen
`devcontainer-vX.Y.Z` und lösen **keinen** Release aus. Applikations-Releases
verwenden `vX.Y.Z`. Wegen der getrennten Namensräume stören sich die beiden
Versionsfolgen nicht.
