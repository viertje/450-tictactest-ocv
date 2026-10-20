# Testkonzept TicTacTest

## 1. Einleitung

| Feld | Wert |
|---|---|
| Projekt | TicTacTest (`450-tictactest-ocv`) |
| Repository | https://github.com/viertje/450-tictactest-ocv |
| Modul | M450 – Applikationen testen |
| Autor | Olivier |
| Reviewer / Auftraggeber | Dominik Berner (`bernedom`), Lehrperson |
| Version Testkonzept | 0.3 (IST) |
| Stand | 30.09.2026 |
| Referenzstand Code | Branch `main`, Commit `89aa731` |

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
| Systemtest / End-to-End | `GameEndToEndTest` -- volle Spielabläufe, ungültige/böswillige Eingabe, DoS-Grenzfall, alles über simuliertes `stdin`/`stdout` (JUnit Pioneer `@StdIo`) | Siehe Abschnitt 9; braucht keine zusätzliche Infrastruktur, da die Simulation im selben JVM-Prozess läuft |
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
| `TicTacToeMain.main()` | ja | E2E-04: Mensch gegen `GreedyPlayer` über den echten Einstiegspunkt |
| `TicTacToePlayer.Stone.opponent()` | indirekt | wird in TF-04 verwendet |
| `GreedyPlayer.play()` | ja, vollständig | Feldwahl, Farbunabhängigkeit, volles Brett |
| `HumanPlayer.play()` | ja | über JUnit Pioneer `@StdIo` (simuliert `System.in`); kein Refactoring nötig, siehe Abschnitt 9 |

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

Keine offen -- die beiden bisherigen Punkte (`HumanPlayer` ungetestet, nur ein Sieg-/Unentschieden-Szenario
für `play()`) sind mit der E2E-Testsuite abgedeckt; siehe Abschnitt 9, inklusive eines dabei gefundenen und
test-first behobenen Fehlers.

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

`./gradlew build` schliesst über `check.dependsOn e2eTest` automatisch auch die E2E-Tests
(Abschnitt 9) mit ein -- `build.yml` musste dafür nicht angepasst werden, ein roter E2E-Test
lässt den Workflow genauso fehlschlagen wie ein roter Unit-Test.

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
| Tests und Coverage prüfen | bei jedem Push / PR | GitHub Actions | `./gradlew build` (inkl. JaCoCo, inkl. E2E via `check`) |
| E2E-Tests separat ausführen | bei Bedarf, lokal | Olivier | `./gradlew e2eTest` |
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
| E2E-Testklassen | 1 (`GameEndToEndTest`, Paket `e2e`, Tag `"e2e"`) |
| E2E-Testfälle | 11 (siehe Abschnitt 9) |

### Offene Punkte

Diese Punkte beschreiben Lücken im heutigen Zustand. Sie sind bewusst nicht als geplante
Massnahmen formuliert, sondern als das, was aktuell fehlt:

Keine -- die beiden bisherigen Punkte sind mit der E2E-Testsuite abgedeckt (Abschnitt 9).

---

## 9. End-to-End-Tests

### Werkzeug und Ausführung

Die E2E-Tests liegen in `GameEndToEndTest` (Paket `ch.bbw.m450.tictactoe.e2e`), sind mit
`@Tag("e2e")` markiert und mit **JUnit Pioneer** (`org.junit-pioneer:junit-pioneer:2.3.0`)
implementiert: `@StdIo({...})` simuliert `System.in` zeilenweise, ein injizierter `StdOut`-Parameter
zeichnet alles auf, was das Programm über `System.out` ausgibt. Anders als die bisherigen
Unit-/Integrationstests rufen diese Tests nicht nur `TicTacToeMain.play(...)` auf, sondern in einem
Fall den echten Einstiegspunkt `TicTacToeMain.main(...)` -- also genau das, was beim Ausführen des
Programms tatsächlich passiert.

Sie laufen in einem eigenen Gradle-Task (`./gradlew e2eTest`), der dieselbe Test-Source-Set und
denselben Classpath wie `test` verwendet, aber nach dem `"e2e"`-Tag statt nach Klassenname filtert
(`test` schliesst diesen Tag umgekehrt aus, damit der alltägliche `./gradlew test`-Lauf schnell
bleibt). Über `check.dependsOn e2eTest` ist der Task automatisch Teil von `./gradlew build` --
und damit auch automatisch Teil der CI-Pipeline (Abschnitt 6), ohne dass `build.yml` selbst
angepasst werden musste.

### Testfälle

| ID | Testfall | Erwartetes Verhalten | Tatsächliches Verhalten |
|---|---|---|---|
| E2E-01 | Voller Spielablauf: CROSS gewinnt die oberste Reihe | Rückgabe `CROSS`; Ausgabe enthält die Gewinnmeldung | **Fehler gefunden, behoben** (siehe unten) |
| E2E-02 | Voller Spielablauf: CIRCLE gewinnt die mittlere Reihe | Rückgabe `CIRCLE`; Ausgabe enthält die Gewinnmeldung | **Fehler gefunden, behoben** (siehe unten) |
| E2E-03 | Voller Spielablauf: Unentschieden über alle 9 Felder | Rückgabe `null`; Ausgabe enthält `"it's a draw!"` | **Fehler gefunden, behoben** (siehe unten) |
| E2E-04 | Echter Einstiegspunkt `main()`: Mensch gegen `GreedyPlayer` | Prompt wird ausgegeben; Mensch gewinnt gegen den deterministischen `GreedyPlayer` | **Fehler gefunden, behoben** (siehe unten) |
| E2E-05 | Ungültige Eingabe: nicht-numerischer Text (`"not-a-number"`) | Erneute Aufforderung statt Absturz, danach normale Fortsetzung | **Fehler gefunden, test-first behoben** (siehe unten) |
| E2E-06 | Ungültige Eingabe: leere Zeile | Erneute Aufforderung statt Absturz | wie erwartet (nach Fix) |
| E2E-07 | Ungültige Eingabe: Dezimalzahl (`"4.5"`) | Erneute Aufforderung statt Absturz | wie erwartet (nach Fix) |
| E2E-08 | Ungültige Eingabe: nur Leerzeichen | Erneute Aufforderung statt Absturz | wie erwartet (nach Fix) |
| E2E-09 | Ungültige Eingabe: Zahl grösser als `Integer.MAX_VALUE` | Erneute Aufforderung statt Absturz; kein stiller Überlauf | wie erwartet (nach Fix) |
| E2E-10 | Böswillige Eingabe: Terminal-Escape-Sequenz als "Zug" | Erneute Aufforderung; die Sequenz wird nur als Text behandelt, nicht ausgeführt | wie erwartet |
| E2E-11 | Denial of Service: eine 1'000'000 Zeichen lange Eingabezeile | Ablehnung innerhalb von 5 Sekunden, abgesichert mit `@Timeout` | wie erwartet |

### Gefundener Fehler und Fix 1: nicht-numerische Eingabe (test-first)

Beim Schreiben von E2E-05 zeigte sich: `HumanPlayer.play()` rief `Integer.parseInt(scanner.nextLine())`
genau einmal auf und liess eine `NumberFormatException` bei nicht-numerischer Eingabe ungefangen bis
nach ganz aussen durchschlagen -- ein einzelner Tippfehler beendete die gesamte Partie mit einem rohen
Stacktrace statt einer verständlichen Fehlermeldung.

**Vorgehen:** Zuerst wurde E2E-05 (`nonNumericTextIsRejectedAndRePrompted`) geschrieben und gegen den
damaligen Stand von `HumanPlayer` ausgeführt -- der Test schlug erwartungsgemäss fehl (unbehandelte
`NumberFormatException` statt einer erneuten Aufforderung). Erst danach wurde `HumanPlayer.play()`
angepasst: `Integer.parseInt(...)` läuft jetzt in einer Schleife mit `try`/`catch`, die bei ungültiger
Eingabe eine Fehlermeldung ausgibt und erneut nach einer Zahl fragt, statt das Spiel abzubrechen. Die
bereits bestehende, separate Validierung in `TicTacToeMain.play()` (Feld ausserhalb des Bretts, Feld
bereits belegt) ist davon nicht betroffen und bleibt unverändert -- das war eine bewusste
Entscheidung, um den Fix eng auf das eine gefundene Problem zu beschränken.

### Gefundener Fehler und Fix 2: mehrfache `Scanner`-Instanzen auf demselben `System.in`

Beim Schreiben der Mehrrunden-Testfälle E2E-01 bis E2E-04 zeigte sich ein zweiter, unabhängiger
Fehler: alle vier Tests schlugen mit `java.util.NoSuchElementException: No line found` fehl, obwohl
die simulierte Eingabe (`@StdIo`) noch genügend Zeilen enthielt. Ursache war, dass
`HumanPlayer.play()` bei **jedem** Aufruf ein neues `Scanner(System.in, ...)`-Objekt erzeugte.
`Scanner` liest intern vorausschauend in einen eigenen Puffer und gibt beim Verwerfen des Objekts
nicht gelesene, aber bereits aus dem Strom entnommene Daten nicht zurück.

Ein erster Fix (`Scanner` als Instanzfeld von `HumanPlayer`, einmal pro Instanz statt einmal pro
Aufruf erzeugt) behob den Fall einer einzelnen `HumanPlayer`-Instanz über mehrere Runden hinweg
(bestätigt durch E2E-04, `main()` gegen `GreedyPlayer`, der danach grün wurde). E2E-01 bis E2E-03
blieben jedoch rot: `TicTacToeMain.play(TicTacToePlayer, TicTacToePlayer)` erlaubt **zwei separate**
`HumanPlayer`-Instanzen (zwei Personen an derselben Tastatur), und zwei unabhängige
`Scanner`-Objekte über demselben `System.in` sind aus demselben Grund unsicher wie ein einzelner, neu
erzeugter `Scanner` pro Aufruf: welche Instanz auch immer zuerst liest, puffert dabei intern Zeilen
vor, die eigentlich für den nächsten Zug der anderen Instanz bestimmt waren.

Dieser Fehler war nicht neu eingeführt, sondern bereits im ursprünglichen Code vorhanden -- er wurde
nur nie sichtbar, weil vor den E2E-Tests kein Test `HumanPlayer.play()` mehrfach hintereinander über
denselben `System.in`-Strom aufgerufen hat (weder mit einer Instanz über mehrere Runden noch mit zwei
`HumanPlayer`-Instanzen im Wechsel).

**Vorgehen:** Da die vier E2E-Tests bereits vor dem Fix geschrieben waren und rot fehlschlugen, diente
ihr Fehlschlag selbst als Nachweis vor der Korrektur (funktional test-first, auch wenn die Tests nicht
gezielt für diesen zweiten Fehler neu geschrieben wurden). Der endgültige Fix hält den `Scanner` als
**prozessweit gemeinsam genutztes** Feld (`static`) statt als Instanzfeld: eine private
`sharedScanner()`-Methode gibt einen gecachten `Scanner` zurück und erzeugt nur dann einen neuen,
wenn sich die aktuelle `System.in`-Referenz seit dem letzten Aufruf geändert hat. Damit teilen sich
alle `HumanPlayer`-Instanzen innerhalb eines Spiels (und damit innerhalb eines Tests, da `@StdIo`
`System.in` für die Dauer eines Testmethodenaufrufs konstant hält) denselben `Scanner`, während ein
Testwechsel -- bei dem `@StdIo` `System.in` auf einen neuen simulierten Strom umstellt -- automatisch
einen frischen `Scanner` erzeugt. In einer echten Partie, in der `System.in` nie wechselt, verhält
sich das exakt wie ein einfaches `static`-Feld: ein einziger `Scanner` für die gesamte Lebensdauer
des Prozesses.

### Denial of Service

`Integer.parseInt` liest die Eingabezeile in einem linearen Durchlauf; selbst eine sehr lange Zeile
(E2E-11: 1 Million Zeichen) wird dadurch innerhalb von Millisekunden abgelehnt. Der Test ist trotzdem
mit `@Timeout(value = 5, unit = SECONDS)` abgesichert: Sollte eine künftige Änderung diesen Pfad
langsamer machen, schlägt der Test kontrolliert fehl, statt die ganze CI-Pipeline unbegrenzt zu
blockieren.

Eine Eingabequelle, die dauerhaft keine Daten sendet (z. B. eine offene, aber stumme Verbindung),
würde `HumanPlayer.play()` unbegrenzt blockieren -- das ist eine inhärente Eigenschaft von
synchronem Konsolen-I/O in Java (`Scanner`/`System.in` kennen kein Lese-Timeout) und wird hier bewusst
nicht behoben. Für eine interaktive Konsolenanwendung mit einem einzigen lokalen Benutzer wird dieses
Risiko als akzeptabel eingestuft; eine vernetzte oder mehrbenutzerfähige Version müsste hierfür einen
expliziten Lese-Timeout einführen.

### Böswillige Eingabe: Einschätzung

E2E-10 prüft eine Terminal-Escape-Sequenz als Eingabe. Sie nimmt denselben Codepfad wie jede andere
nicht-numerische Eingabe (Abschnitt "Gefundener Fehler und Fix 1") und wird deshalb genauso abgelehnt
und in der Fehlermeldung als Text zurückgespiegelt. Eine gezielte Bereinigung von Steuerzeichen wurde
bewusst nicht ergänzt: `HumanPlayer` liest und schreibt ausschliesslich auf das eigene, lokale
Terminal desselben Benutzers -- es gibt keine zweite Partei, die durch eine reflektierte
Escape-Sequenz betroffen sein könnte.
