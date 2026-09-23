# Testkonzept TicTacTest

## 1. Einleitung

| Feld | Wert |
|---|---|
| Projekt | TicTacTest (`450-tictactest-ocv`) |
| Repository | https://github.com/viertje/450-tictactest-ocv |
| Modul | M450 – Applikationen testen |
| Autor | Olivier |
| Reviewer / Auftraggeber | Dominik Berner (`bernedom`), Lehrperson |
| Version Testkonzept | 0.2 (IST) |
| Stand | 22.09.2026 |
| Referenzstand Code | Branch `main`, Commit `ceed64e` |

**Kurzbeschreibung:** TicTacTest ist eine Konsolenanwendung, die ein Tic-Tac-Toe-Spiel
zwischen zwei Spielern austrägt. Das Spielbrett ist ein flaches Array mit neun Feldern
(`Stone[9]`, Index = Zeile * 3 + Spalte). Die Spiellogik liegt in `TicTacToeMain`, die
Spieler implementieren das Interface `TicTacToePlayer`.

**Scope:** Dieses Dokument beschreibt den **IST-Zustand** der Testpraxis im Repository.
Es hält fest, was heute tatsächlich vorhanden ist – nicht, was künftig geplant ist.
Fehlende Abdeckung wird unter Abschnitt 4 und 8 als offener Punkt ausgewiesen.

---

## 2. Testziele

Die Testziele sind nummeriert, damit die Testfälle in Abschnitt 7 darauf verweisen können.

| ID | Testziel |
|---|---|
| TZ-01 | Die Gewinnerkennung erkennt alle acht Gewinnlinien (3 Zeilen, 3 Spalten, 2 Diagonalen). |
| TZ-02 | Die Gewinnerkennung meldet keinen Gewinn bei unvollständigen, unterbrochenen oder durch den Gegner blockierten Linien. |
| TZ-03 | Die Gewinnregel gilt für beide Farben gleich, und ein Gewinn einer Farbe ist nie ein Gewinn der Gegenfarbe. |
| TZ-04 | Ungültige Spielzüge und ungültige Aufrufe führen zu definiertem Fehlerverhalten (Exception mit aussagekräftiger Meldung). |
| TZ-05 | Ein vollständiger Spielablauf liefert ein korrektes Resultat: Gewinnfarbe oder `null` bei Unentschieden. |
| TZ-06 | Der `GreedyPlayer` wählt deterministisch das erste freie Feld und meldet ein volles Brett als Fehler. |
| TZ-07 | Die Kernfunktionen bleiben bei Änderungen stabil (Regression), da die Testsuite jederzeit wiederholbar ist. |
| TZ-08 | Die textuelle Darstellung des Bretts (`toString`) zeigt für jedes Feld den korrekten Inhalt – die Feldnummer bei einem leeren Feld, sonst den Stein. |

---

## 3. Teststrategie und Teststufen

### Gewählte Strategie (IST)

Der Schwerpunkt liegt auf **automatisierten Unit-Tests**. Sie sind schnell, deterministisch
und laufen ohne externe Abhängigkeiten. Ergänzend prüfen einige Tests das **Zusammenspiel
mehrerer Klassen**.

| Teststufe | Umfang im Projekt | Begründung |
|---|---|---|
| Unit-Test | `TicTacToeMain.isWin()`, `GreedyPlayer.play()` – Prüfung einzelner Methoden isoliert gegen vorbereitete Board-Konstellationen | Schnell, stabil, deckt die Kernlogik feingranular ab |
| Integrationstest (Klassenebene) | `TicTacToeMain.play()` – spielt eine komplette Partie über `TicTacToeMain` **und** zwei `TicTacToePlayer`-Implementierungen | Deckt das Zusammenspiel von Spiellogik und Spielern ab, das ein Unit-Test allein nicht sieht |
| Systemtest / End-to-End | **nicht vorhanden** | Die Anwendung läuft über `stdin`/`stdout`; ein End-to-End-Test wäre nur mit zusätzlicher Infrastruktur möglich |
| Manuelle Tests | **nicht dokumentiert** | Es existiert kein manuelles Testprotokoll |

**Abgrenzung Unit- gegen Integrationstest:** Ein Unit-Test prüft eine einzelne Einheit
isoliert; alles Umliegende wird durch Testdaten oder Test-Doubles ersetzt. Ein
Integrationstest prüft bewusst das Zusammenwirken mehrerer echter Einheiten. In diesem
Projekt ist `isWin()` ein Unit-Test (nur eine statische Methode, Eingabe ist ein Array),
`play()` dagegen ein Integrationstest: dort laufen Spiellogik, Rundenwechsel und zwei
Spielerobjekte zusammen.

### Teststrukturen

| Struktur | Umsetzung im Projekt |
|---|---|
| Helper (Testdaten) | `TestBoards.boardFrom("XXX", "OO.", "...")` erzeugt ein Board aus einem lesbaren String-Layout; `TestBoards.emptyBoard()` liefert ein leeres Brett |
| Helper (Assertions) | `assertWins(...)` / `assertDoesNotWin(...)` kapseln die wiederkehrende Prüfung und geben bei einem Fehlschlag das Brett aus |
| Fixture | `@BeforeEach setUp()` baut vor jedem Test ein leeres Brett und zwei frische Spieler neu auf, damit sich Tests nicht gegenseitig beeinflussen |
| Test-Double (Stub) | `ScriptedPlayer` spielt eine fest vorgegebene Zugfolge und ersetzt den `HumanPlayer`, der sonst auf `stdin` blockieren würde |
| Parameterized Tests | `@MethodSource`, `@CsvSource`, `@EnumSource`, `@ValueSource` – eine Testmethode, viele Datensätze |

### Testdaten

Alle Testdaten sind **literal im Testcode** hinterlegt, entweder als String-Layout
(`"XXX", "OO.", "..."`) oder als Zugfolge (`new ScriptedPlayer(0, 2, 3, 7, 8)`).
Es gibt keine externen Testdatendateien; `src/test/resources` ist leer.

Die Auswahl folgt drei Kriterien:

- **Positivfälle:** je ein Vertreter pro Gewinnlinie (vollständige Abdeckung der acht Bedingungen in `isWin`)
- **Negativfälle:** unvollständige, unterbrochene und blockierte Linien
- **Grenzwerte:** Feldpositionen `-1` und `9` direkt ausserhalb des gültigen Bereichs 0–8, ergänzt um `Integer.MIN_VALUE` und `Integer.MAX_VALUE`; die gültigen Ränder 0 und 8 werden über `GreedyPlayerTest` abgedeckt

---

## 4. Testobjekte und Testabdeckung

### Testobjekte

| Klasse / Methode | Getestet | Bemerkung |
|---|---|---|
| `TicTacToeMain.isWin()` | ja, vollständig | alle 8 Gewinnlinien, beide Farben, Negativfälle |
| `TicTacToeMain.play()` | ja | Sieg, Unentschieden, alle Fehlerpfade |
| `TicTacToeMain.toString()` | ja, direkt | zwei Golden-Output-Tests (leeres Brett, teilweise belegtes Brett) prüfen den kompletten String inkl. ANSI-Farbcodes |
| `TicTacToeMain.main()` | nein | reiner Einstiegspunkt, startet ein interaktives Spiel |
| `TicTacToePlayer.Stone.opponent()` | indirekt | wird in TF-04 verwendet |
| `GreedyPlayer.play()` | ja, vollständig | Feldwahl, Farbunabhängigkeit, volles Brett |
| `HumanPlayer.play()` | **nein** | liest über `Scanner` von `stdin` und ist ohne Refactoring nicht automatisiert testbar |

### Abdeckung

Die Codeabdeckung wird mit **JaCoCo** (`build.gradle`, `toolVersion 0.8.15`) gemessen.
`./gradlew build` führt die Tests aus und erzeugt anschliessend über
`jacocoTestReport` einen HTML-Report (`build/reports/jacoco/test/html/`) sowie einen
maschinenlesbaren XML-Report. In der CI läuft das bei jedem Push und Pull Request; der
HTML-Report wird als Artifact hochgeladen, und für jeden Push auf `main` wird der
Zeilen-Abdeckungswert zusätzlich in `coverage-history.csv` auf dem Branch `gh-pages`
fortgeschrieben und dort als Zeitreihe visualisiert (siehe Abschnitt 6). Aktueller Stand:
**84.4 % Line Coverage**.

| Testziel | Abdeckung |
|---|---|
| TZ-01 | vollständig – alle acht Bedingungen in `isWin` haben je einen Testfall |
| TZ-02 | gut – sechs Negativkonstellationen plus leeres Brett |
| TZ-03 | vollständig – über `@EnumSource` für beide Farben |
| TZ-04 | gut – doppelter Spieler, besetztes Feld, Positionen ausserhalb des Bretts |
| TZ-05 | teilweise – je ein Sieg- und ein Unentschieden-Szenario, keine weiteren Spielverläufe |
| TZ-06 | vollständig |
| TZ-07 | vollständig – die Suite läuft automatisiert bei jedem Push und Pull Request in der CI |

### Offene Risiken

| Risiko | Auswirkung |
|---|---|
| `HumanPlayer` ist ungetestet | Eine nicht-numerische Eingabe löst eine `NumberFormatException` aus, die nirgends abgefangen wird; das Spiel stürzt ab |
| Nur ein Sieg- und ein Unentschieden-Szenario für `play()` | Andere Spielverläufe (z. B. Sieg erst in der letzten Runde) sind nicht abgedeckt |

---

## 5. Testrahmen und Erfolgskriterien

### Rollen

| Rolle | Person | Aufgabe |
|---|---|---|
| Entwickler und Tester | Olivier | schreibt Produktiv- und Testcode, führt die Tests aus |
| Reviewer | Dominik Berner (`bernedom`) | prüft Code und Tests im Pull Request |

Entwicklung und Test liegen in derselben Hand. Das ist für ein Schulprojekt dieser Grösse
vertretbar, birgt aber das bekannte Risiko der Betriebsblindheit: Wer den Code geschrieben
hat, testet tendenziell das, was er ohnehin erwartet. Das Review im Pull Request ist die
Gegenmassnahme.

### Zeitfenster

- **Lokal:** vor jedem Commit über `./gradlew test` in IntelliJ oder auf der Kommandozeile
- **Pull Request:** Review durch `bernedom` vor dem Merge nach `main`
- **CI:** bei jedem Push und Pull Request auf `main` – vollständiger Testlauf inkl. JaCoCo-Coverage-Report

### Erfolgskriterien (Pass/Fail)

| Kriterium | Schwelle |
|---|---|
| Bestanden | `./gradlew test` endet mit `BUILD SUCCESSFUL`, 0 Fehler und 0 Errors über alle 35 Testfälle |
| Nicht bestanden | mindestens ein Testfall schlägt fehl oder bricht mit einem Error ab |
| Abbruch | der Build bricht bereits beim Kompilieren ab; die Tests werden dann gar nicht ausgeführt |

Bei einem roten Test wird nicht nach `main` gemergt.

---

## 6. Testumgebung und Testinfrastruktur

### Technischer Rahmen

| Komponente | Version / Wert | Festgelegt in |
|---|---|---|
| Sprache / Runtime | Java, Azul (Zulu) 25 | `gradle/gradle-daemon-jvm.properties` |
| Build-Tool | Gradle 9.7.0 (über Wrapper, mit SHA-256-Prüfsumme) | `gradle/wrapper/gradle-wrapper.properties` |
| Testframework | JUnit Jupiter 6.1.3 (inkl. `junit-jupiter-params`) | `build.gradle` |
| Test-Launcher | JUnit Platform Launcher | `build.gradle` |
| Assertion-Bibliothek | AssertJ 3.27.7 | `build.gradle` |
| Coverage-Werkzeug | JaCoCo 0.8.15 (`jacocoTestReport`, `finalizedBy`/`dependsOn test`) | `build.gradle` |
| Testplattform-Aktivierung | `test { useJUnitPlatform() }` | `build.gradle` |

### Umgebungen

| Umgebung | Beschreibung |
|---|---|
| DEV (lokal) | Windows-Arbeitsplatz, IntelliJ IDEA, JDK über den Gradle-Daemon festgelegt |
| CI | GitHub Actions, eigenes Container-Image `ghcr.io/viertje/450-tictactest-ocv:v1.0.1` (Zulu 25, siehe DevContainer-Versioning.md), definiert in `.github/workflows/build.yml` |
| PROD | **nicht vorhanden** – das Projekt wird nicht produktiv betrieben |

Der wesentliche Unterschied zwischen DEV und CI: Die lokale Maschine hat eine gewachsene
Konfiguration, die CI startet bei jedem Lauf von null. Damit beide dasselbe Ergebnis
liefern, sind Gradle-Version, Prüfsumme und JDK-Anforderung im Repository festgeschrieben
und laufen in der CI zusätzlich im selben versionierten Container-Image wie der lokale
Devcontainer.

### CI-Pipeline (IST)

Die Datei `.github/workflows/build.yml` definiert einen Job `build`, der bei Push (ausser
auf `gh-pages`) und bei Pull Requests auf `main` läuft. Schritte: Checkout, `gradlew`
ausführbar machen, `./gradlew build --no-daemon` (kompiliert **und führt die Tests aus**,
inkl. JaCoCo-Report über `finalizedBy jacocoTestReport`), Upload des HTML-Coverage-Reports
als Artifact.

Nur bei einem Push auf `main` laufen zusätzlich drei weitere Schritte: Der
Zeilen-Abdeckungswert wird aus dem JaCoCo-XML-Report extrahiert, der Branch `gh-pages`
wird ausgecheckt, und der Wert wird als neue Zeile an `coverage-history.csv` angehängt und
zurück auf `gh-pages` gepusht. Der Branch `gh-pages` enthält zusätzlich eine `index.html`,
die diese CSV per Chart.js als Zeitreihe visualisiert und über GitHub Pages veröffentlicht
ist.

> **Für den IST-Zustand:** Die automatisierte Testausführung findet nicht mehr nur lokal
> statt – jeder Push und jeder Pull Request führt die vollständige Suite in der CI aus,
> und ein roter Test lässt den Workflow fehlschlagen.

---

## 7. Testfallbeschreibungen

### Übersicht

Die Suite besteht aus **14 Testmethoden**, die durch parametrisierte Tests **35 Testfälle**
ergeben.

| ID | Testmethode | Klasse | Zielbezug | Fälle |
|---|---|---|---|---|
| TF-01 | `detectsEveryWinningLine` | `TicTacToeMainTest` | TZ-01 | 8 |
| TF-02 | `detectsNoWinOnIncompleteLines` | `TicTacToeMainTest` | TZ-02 | 6 |
| TF-03 | `emptyBoardHasNoWinnerAtAll` | `TicTacToeMainTest` | TZ-02 | 1 |
| TF-04 | `aWinForOneColorIsNeverAWinForTheOpponent` | `TicTacToeMainTest` | TZ-03 | 2 |
| TF-05 | `twoGreedyPlayersLetTheStartingPlayerWin` | `TicTacToeMainTest` | TZ-05 | 1 |
| TF-06 | `aFullBoardWithoutThreeInALineIsADraw` | `TicTacToeMainTest` | TZ-05 | 1 |
| TF-07 | `theSamePlayerTwiceIsRejected` | `TicTacToeMainTest` | TZ-04 | 1 |
| TF-08 | `playingOutsideTheBoardIsRejected` | `TicTacToeMainTest` | TZ-04 | 4 |
| TF-09 | `playingToAnOccupiedFieldIsRejected` | `TicTacToeMainTest` | TZ-04 | 1 |
| TF-10 | `picksTheFirstFreeField` | `GreedyPlayerTest` | TZ-06 | 5 |
| TF-11 | `ignoresTheOwnColor` | `GreedyPlayerTest` | TZ-06 | 2 |
| TF-12 | `throwsWhenTheBoardIsFull` | `GreedyPlayerTest` | TZ-06 | 1 |
| TF-13 | `emptyBoardShowsEveryFieldIndex` | `TicTacToeMainTest` | TZ-08 | 1 |
| TF-14 | `occupiedFieldsShowTheirStoneInsteadOfTheIndex` | `TicTacToeMainTest` | TZ-08 | 1 |

Alle vierzehn Methoden tragen über TZ-07 zur Regressionssicherung bei.

### Detaillierte Beschreibungen

**TF-01 – Gewinnerkennung über alle Linien**

- **ID:** TF-01
- **Zielbezug:** TZ-01
- **Voraussetzung:** Ein Board mit drei gleichen Steinen auf einer der acht Gewinnlinien, erzeugt über `TestBoards.boardFrom(...)`
- **Testschritte:** `TicTacToeMain.isWin(board, Stone.CROSS)` aufrufen
- **Erwartetes Ergebnis:** `true` für jede der acht Linien
- **Datenquelle:** `@MethodSource("winningLinesForCross")`, acht benannte Board-Konstellationen

**TF-06 – Unentschieden bei vollem Brett**

- **ID:** TF-06
- **Zielbezug:** TZ-05
- **Voraussetzung:** Zwei `ScriptedPlayer` mit den Zugfolgen `0, 2, 3, 7, 8` (CROSS) und `1, 4, 5, 6` (CIRCLE)
- **Testschritte:** `TicTacToeMain.play(scriptedX, scriptedO)` aufrufen
- **Erwartetes Ergebnis:** Rückgabe `null`; das Brett ist nach neun Runden voll, ohne dass eine Linie zustande kommt

**TF-08 – Grenzwerte ausserhalb des Bretts**

- **ID:** TF-08
- **Zielbezug:** TZ-04
- **Voraussetzung:** Ein `ScriptedPlayer`, dessen erster Zug auf eine ungültige Position zeigt
- **Testschritte:** `TicTacToeMain.play(...)` mit den Positionen `-1`, `9`, `Integer.MIN_VALUE` und `Integer.MAX_VALUE` aufrufen
- **Erwartetes Ergebnis:** `IllegalStateException` mit der Meldung `cannot play to position <Position>`
- **Bemerkung:** `-1` und `9` sind die beiden Grenzwerte direkt neben dem gültigen Bereich 0–8; die Extremwerte belegen zusätzlich, dass die Prüfung nicht überläuft

---

## 8. Testplan und Zuständigkeiten

### Ablauf

| Schritt | Zeitpunkt | Verantwortlich | Werkzeug |
|---|---|---|---|
| Testfälle schreiben und erweitern | während der Entwicklung | Olivier | IntelliJ, JUnit |
| Testsuite lokal ausführen | vor jedem Commit | Olivier | `./gradlew test` |
| Tests und Coverage prüfen | bei jedem Push / PR | GitHub Actions | `./gradlew build` (inkl. JaCoCo) |
| Code- und Test-Review | vor dem Merge | `bernedom` | Pull Request auf GitHub |

### Aktueller Teststand

| Kennzahl | Wert |
|---|---|
| Testklassen | 2 (`TicTacToeMainTest`, `GreedyPlayerTest`) |
| Hilfsklassen im Testcode | 2 (`TestBoards`, `ScriptedPlayer`) |
| Testmethoden | 14 |
| Testfälle | 35 |
| Letzter lokaler Testlauf | 08.09.2026, `TicTacToeMainTest`: 25 Tests, 0 Fehler, 0 Errors |
| Line Coverage (JaCoCo, CI) | 84.4 % (Stand 22.09.2026) |

### Offene Punkte

Diese Punkte beschreiben Lücken im heutigen Zustand. Sie sind bewusst nicht als geplante
Massnahmen formuliert, sondern als das, was aktuell fehlt:

1. `HumanPlayer` ist nicht getestet; ungültige Eingaben sind nicht abgefangen.
2. Für `play()` existieren nur zwei Spielverläufe (ein Sieg, ein Unentschieden).
