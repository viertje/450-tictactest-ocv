# Testkonzept TicTacTest

## 1. Einleitung

| Feld | Wert |
|---|---|
| Projekt | TicTacTest (`450-tictactest-ocv`) |
| Repository | https://github.com/viertje/450-tictactest-ocv |
| Modul | M450 – Applikationen testen |
| Autor | Olivier |
| Reviewer / Auftraggeber | Dominik Berner (`bernedom`), Lehrperson |
| Version Testkonzept | 0.4 (IST) |
| Stand | 23.10.2026 |
| Referenzstand Code | Branch `main`, Stand 23.10.2026 |

**Kurzbeschreibung:** TicTacTest ist eine Konsolenanwendung, die ein Tic-Tac-Toe-Spiel
zwischen zwei Spielern austrägt. Das Spielbrett ist ein flaches Array mit neun Feldern
(`Stone[9]`, Index = Zeile * 3 + Spalte). Die Spiellogik liegt in `TicTacToeMain`, die
Spieler (`HumanPlayer`, `GreedyPlayer`, `PerfectPlayer`) implementieren das Interface
`TicTacToePlayer`.

**Scope:** Dieses Dokument beschreibt den **IST-Zustand** der Testpraxis im Repository.
Es hält fest, was heute tatsächlich vorhanden ist, und weist Lücken in Abschnitt 8 offen aus.

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
| TZ-08 | Die textuelle Darstellung des Bretts (`toString`) zeigt für jedes Feld den korrekten Inhalt. |
| TZ-09 | Der `PerfectPlayer` verliert nie, gewinnt sofort, wenn möglich, und blockiert sonst einen drohenden Gewinn des Gegners. |
| TZ-10 | Der `HumanPlayer` bleibt bei Fehleingaben (Text, leere Zeile, Zahl ausser Bereich, Steuerzeichen) handlungsfähig und fragt erneut. |
| TZ-11 | Architekturregeln bleiben eingehalten: Alle Klassen im Paket `players` implementieren `TicTacToePlayer`, und nur der `HumanPlayer` liest von `System.in`. |

---

## 3. Teststrategie und Teststufen

### Gewählte Strategie (IST)

Der Schwerpunkt liegt auf **automatisierten Unit-Tests**: schnell, deterministisch, ohne
externe Abhängigkeiten. Darüber liegen wenige Integrationstests, eine kleine
End-to-End-Suite über simulierte Konsole und zwei statische Architekturregeln.

### Einordnung in die Testpyramide

Die Zahlen sind aus dem Quellcode gezählt (Stand 23.10.2026).

| Teststufe | Testfälle | Klassen | Was geprüft wird |
|---|---|---|---|
| Unit | 43 | `TicTacToeMainTest`, `GreedyPlayerTest`, `PerfectPlayerTest` | einzelne Klassen mit vorbereiteten Brettern. `TicTacToeMain.play()` mit `ScriptedPlayer` (Stub) zählt als Unit-Test, weil die Mitspieler ersetzt sind. |
| Integration | 4 | `TicTacToeMainTest`, `PerfectPlayerTest` | `play()` mit **echten** Spielern: zwei `GreedyPlayer`, zwei `PerfectPlayer` (Unentschieden), `PerfectPlayer` gegen `GreedyPlayer` aus beiden Positionen |
| End-to-End | 11 (7 Methoden) | `GameEndToEndTest` | ganze Partien und Fehleingaben über simuliertes `stdin`/`stdout` (JUnit Pioneer `@StdIo`), einmal über den echten Einstiegspunkt `main()` |
| Statische Architekturprüfung | 2 | `ArchitectureTest` | ArchUnit prüft den kompilierten Code, ohne ihn auszuführen (TZ-11) |
| Manuelle Tests | nicht dokumentiert | – | Es existiert kein manuelles Testprotokoll. |

**Abgrenzung:** Massgebend für die Stufe ist, ob die Mitspieler **echt oder ersetzt** sind und
wo der Test einsteigt, nicht wie viele Methoden aufgerufen werden. Derselbe Aufruf
`play(...)` ist ein Unit-Test mit `ScriptedPlayer`, ein Integrationstest mit echten Spielern
und Teil eines E2E-Tests, wenn die Eingabe über die Konsole simuliert wird.
Der exhaustive Test `neverLoses` ruft zusätzlich `TicTacToeMain.isWin()` auf, eine zustandslose
Hilfsfunktion; er wird deshalb als Unit-Test des `PerfectPlayer` gezählt.

**Beurteilung:** Die Basis ist breit, die Mitte dünn: Es gibt mehr E2E- als Integrationsfälle.
Zudem laufen die meisten E2E-Tests über `TicTacToeMain.play()` und nicht über `main()`.

**Qualitätsmasse, keine Teststufen:** JaCoCo (Coverage) und PIT (Mutation Testing) prüfen nicht
die Anwendung, sondern wie gut die Tests sind. Sie gelten für alle Teststufen (Abschnitt 4).

### Teststrukturen

| Struktur | Umsetzung im Projekt |
|---|---|
| Helper (Testdaten) | `TestBoards.boardFrom("XXX", "OO.", "...")` erzeugt ein Board aus einem lesbaren String-Layout; `TestBoards.emptyBoard()` liefert ein leeres Brett |
| Helper (Assertions) | `winFor(color)` ist eine AssertJ-`Condition`; `assertThat(board).has(winFor(CROSS))` liest sich wie ein Satz und meldet bei einem Fehlschlag das Brett |
| Fixture | `@BeforeEach setUp()` baut vor jedem Test Spieler und Brett neu auf, damit sich Tests nicht beeinflussen |
| Test-Double (Stub) | `ScriptedPlayer` spielt eine fest vorgegebene Zugfolge; ohne Mocking-Framework von Hand geschrieben |
| Parameterized Tests | `@MethodSource`, `@CsvSource`, `@EnumSource`, `@ValueSource` – eine Methode, viele Datensätze |
| Exhaustive Test | `neverLoses` spielt alle Partien durch, in denen der Gegner jeden legalen Zug versucht |

### Testdaten

Alle Testdaten stehen **literal im Testcode** (String-Layouts wie `"XXX", "OO.", "..."` oder
Zugfolgen wie `new ScriptedPlayer(0, 2, 3, 7, 8)`); `src/test/resources` ist leer.

- **Positivfälle:** je ein Vertreter pro Gewinnlinie
- **Negativfälle:** unvollständige, unterbrochene und blockierte Linien
- **Grenzwerte:** Feldpositionen `-1` und `9` direkt ausserhalb des Bereichs 0–8, dazu `Integer.MIN_VALUE` und `Integer.MAX_VALUE`; bei der Eingabe eine Zahl, die nicht in einen `int` passt

---

## 4. Testobjekte und Testabdeckung

### Testobjekte

| Klasse / Methode | Getestet | Bemerkung |
|---|---|---|
| `TicTacToeMain.isWin()` | ja, vollständig | alle 8 Gewinnlinien, beide Farben, Negativfälle |
| `TicTacToeMain.play()` | ja | Sieg, Unentschieden, alle Fehlerpfade |
| `TicTacToeMain.toString()` | ja, direkt | zwei Golden-Output-Tests inkl. ANSI-Farbcodes |
| `TicTacToeMain.main()` | ja | E2E-04: Mensch gegen `GreedyPlayer` |
| `GreedyPlayer.play()` | ja, vollständig | Feldwahl, Farbunabhängigkeit, volles Brett |
| `PerfectPlayer.play()` | ja | feste Stellungen (Gewinnen, Blockieren), Tie-Break, Brett bleibt unverändert, volles Brett, exhaustiv "verliert nie" |
| `HumanPlayer.play()` | ja | nur über E2E-Tests (`@StdIo`) |
| Paketstruktur `players` | ja | `ArchitectureTest` (TZ-11) |

### Abdeckung

Drei Zahlen messen Verschiedenes und dürfen nicht verwechselt werden:

| Mass | Werkzeug | Stand | Was es aussagt |
|---|---|---|---|
| **Branch Coverage** | JaCoCo, über Unit- **und** E2E-Tests | **Gate: mindestens 90 %**; der aktuelle Wert steht im Chart auf GitHub Pages | welche Verzweigungen die Tests durchlaufen |
| **Mutation Coverage** | PIT 1.30.1, nur Unit-Tests | 92 %; Gate: mindestens 90 % | ob die Tests Änderungen am Code bemerken |
| **Test Strength** | PIT | 100 % | Anteil erkannter Mutanten in dem Code, den die Tests ausführen |

Das Coverage-Gate (`jacocoTestCoverageVerification`) ist Teil von `check`: Liegt die Branch
Coverage unter 90 %, schlägt `./gradlew build` fehl. Der Report und das Gate lesen die
Coverage-Daten von `test` und `e2eTest` gemeinsam, sonst wäre alles unsichtbar, was nur die
E2E-Tests ausführen (z. B. die Eingabeprüfung im `HumanPlayer`). Die Differenz zwischen
92 % Mutation Coverage und 100 % Test Strength erklärt sich damit: Die nicht erkannten
Mutanten liegen alle im `HumanPlayer`, den nur die E2E-Tests ausführen und PIT bewusst nicht
laufen lässt.

Verlauf: Auf GitHub Pages zeigt ein Chart die Branch Coverage von `main`, ein Punkt pro
Merge (Beschriftung: PR-Nummer), mit der 90-%-Linie. Eine frühere Version hat Line Coverage
gemessen (84.4 %, später 73.1 % nach dem Merge der E2E-Tests). Der Fall zeigt, dass eine
Messgrösse irreführen kann, wenn nicht klar ist, was sie erfasst: Der neue `HumanPlayer`-Code
galt als ungetestet, weil der Report die E2E-Tests nicht einbezog.

### Abdeckung der Testziele

| Testziel | Abdeckung |
|---|---|
| TZ-01 bis TZ-03 | vollständig |
| TZ-04 | gut – doppelter Spieler, besetztes Feld, Positionen ausserhalb des Bretts |
| TZ-05 | gut – Sieg beider Farben, Unentschieden, Spiele mit echten und simulierten Spielern |
| TZ-06, TZ-08 | vollständig |
| TZ-07 | vollständig – die Suite läuft bei jedem Push und Pull Request in der CI |
| TZ-09 | stark – exhaustiver Nachweis "verliert nie" für beide Farben |
| TZ-10 | gut – sieben Fehleingabe-Varianten |
| TZ-11 | vollständig für die zwei Regeln |

---

## 5. Testrahmen und Erfolgskriterien

### Rollen

| Rolle | Person | Aufgabe |
|---|---|---|
| Entwickler und Tester | Olivier | schreibt Produktiv- und Testcode, führt die Tests aus |
| Reviewer | Dominik Berner (`bernedom`) | hat die Pull Requests der Unterrichtsaufgaben geprüft |

Entwicklung und Test liegen in derselben Hand. Das birgt das Risiko der Betriebsblindheit:
Wer den Code geschrieben hat, testet tendenziell das, was er ohnehin erwartet. Die späteren
Pull Requests hat Olivier selbst gemergt; ein zweites Augenpaar fehlt dort (Abschnitt 8).
Als Gegenmassnahme dienen der automatische Build, das Coverage-Gate und PIT.

### Zeitfenster

- **Lokal:** vor jedem Commit über `./gradlew build` (Tests, E2E-Tests, Coverage-Gate)
- **Pull Request / Push:** Workflows `Build` und `Mutation testing`, Details in `docs/CI-CD-Pipeline.md`
- **Merge nach `main`:** nur per Pull Request (Branch Protection) mit grünem Status-Check `Build & Test`

### Erfolgskriterien (Pass/Fail)

| Kriterium | Schwelle |
|---|---|
| Bestanden | `./gradlew build` endet mit `BUILD SUCCESSFUL`: alle Testfälle grün, Branch Coverage mindestens 90 %; zusätzlich Mutation Coverage mindestens 90 % im Workflow `Mutation testing` |
| Nicht bestanden | mindestens ein Testfall schlägt fehl, oder eines der beiden Gates wird unterschritten |
| Abbruch | der Build bricht bereits beim Kompilieren ab; die Tests werden dann gar nicht ausgeführt |

Bei einem roten Build wird nicht nach `main` gemergt.

---

## 6. Testumgebung und Testinfrastruktur

### Technischer Rahmen

| Komponente | Version | Festgelegt in |
|---|---|---|
| Sprache / Runtime | Java, Azul (Zulu) 25 | `gradle/gradle-daemon-jvm.properties` |
| Build-Tool | Gradle 9.8.2 (Wrapper, mit SHA-256-Prüfsumme) | `gradle/wrapper/gradle-wrapper.properties` |
| Testframework | JUnit Jupiter 6.1.3 (inkl. Parameterized Tests) | `build.gradle` |
| Assertions | AssertJ 3.27.7 | `build.gradle` |
| Konsole simulieren | JUnit Pioneer 2.3.0 (`@StdIo`) | `build.gradle` |
| Architekturregeln | ArchUnit 1.5.1 (nur Core, ohne eigene Test-Engine) | `build.gradle` |
| Coverage | JaCoCo 0.8.15 | `build.gradle` |
| Mutation Testing | PIT 1.30.1 über `gradle-pitest-plugin` 1.19.0 (JUnit-5-Plugin 1.2.3) | `build.gradle` |

Die Versionen wurden am 23.10.2026 gegen Maven Central und Gradle geprüft; AssertJ 4.0.0
existiert nur als Milestone und wird deshalb nicht verwendet.

### Umgebungen

| Umgebung | Beschreibung |
|---|---|
| DEV (lokal) | Windows-Arbeitsplatz, IntelliJ IDEA, JDK über den Gradle-Daemon festgelegt |
| CI | GitHub Actions in einem eigenen Container-Image (`ghcr.io/viertje/450-tictactest-ocv:v1.0.2`, Zulu 25), versioniert und automatisch aktualisiert (`docs/DevContainer-Versioning.md`) |
| PROD | **nicht vorhanden** – das Projekt wird nicht produktiv betrieben; als Ersatz dient das veröffentlichte JAR eines Releases (`RELEASE.md`) |

DEV und CI unterscheiden sich vor allem in der Historie: Die lokale Maschine ist über Jahre
gewachsen, die CI startet bei jedem Lauf von null. Damit beide dasselbe Ergebnis liefern, sind
Gradle-Version, Prüfsumme und JDK-Anforderung im Repository festgeschrieben, und die CI läuft im
selben versionierten Image wie der lokale Devcontainer.

### CI-Pipeline (IST)

Die Pipeline besteht aus vier Workflows (Details in `docs/CI-CD-Pipeline.md`):

| Workflow | Auslöser | Zweck |
|---|---|---|
| `build.yml` (`Build & Test`) | Push, Pull Request auf `main` | `./gradlew build` (Unit-, E2E-Tests, Coverage-Gate), Upload von JaCoCo- und Test-Report; auf `main` zusätzlich Eintrag in die Coverage-Zeitreihe |
| `mutationtesting.yml` | Push, Pull Request auf `main` | PIT mit Gate, Upload des PIT-Reports |
| `release.yml` | Tag `vX.Y.Z` | Tests, JAR bauen, GitHub Release (`RELEASE.md`) |
| `dockerimageupdate.yml` | Änderung am Dockerfile auf `main` | Image bauen und veröffentlichen, Versionen automatisch nachziehen |

Ein roter Test, ein unterschrittenes Gate oder ein roter E2E-Test lassen den Workflow
fehlschlagen.

---

## 7. Testfallbeschreibungen

### Übersicht

Die Unit-, Integrations- und Architekturtests bestehen aus **24 Testmethoden** mit
**49 Testfällen**; dazu kommen 7 E2E-Methoden mit 11 Fällen (Abschnitt 9).

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
| TF-15 | `findsTheOnlyMoveThatDoesNotThrowTheGameAway` | `PerfectPlayerTest` | TZ-09 | 4 |
| TF-16 | `playsTheFirstFieldWhenAllMovesAreEquallyGood` | `PerfectPlayerTest` | TZ-09 | 1 |
| TF-17 | `doesNotModifyTheBoardItIsGiven` | `PerfectPlayerTest` | TZ-09 | 1 |
| TF-18 | `throwsWhenTheBoardIsFull` | `PerfectPlayerTest` | TZ-09 | 1 |
| TF-19 | `neverLoses` | `PerfectPlayerTest` | TZ-09 | 2 |
| TF-20 | `twoPerfectPlayersEndInADraw` | `PerfectPlayerTest` | TZ-09 | 1 |
| TF-21 | `beatsTheGreedyPlayerWhenStarting` / `...WhenSecond` | `PerfectPlayerTest` | TZ-09 | 2 |
| TF-22 | `everyClassInThePlayersPackageIsAPlayer`, `onlyTheHumanPlayerReadsStandardInput` | `ArchitectureTest` | TZ-11 | 2 |

Alle Methoden tragen über TZ-07 zur Regressionssicherung bei.

### Detaillierte Beschreibungen (Auswahl)

**TF-01 – Gewinnerkennung über alle Linien**

- **Zielbezug:** TZ-01
- **Voraussetzung:** ein Board mit drei gleichen Steinen auf einer der acht Gewinnlinien, erzeugt über `TestBoards.boardFrom(...)`
- **Testschritte:** `TicTacToeMain.isWin(board, Stone.CROSS)` aufrufen
- **Erwartetes Ergebnis:** `true` für jede der acht Linien
- **Datenquelle:** `@MethodSource("winningLinesForCross")`, acht benannte Board-Konstellationen

**TF-08 – Grenzwerte ausserhalb des Bretts**

- **Zielbezug:** TZ-04
- **Voraussetzung:** ein `ScriptedPlayer`, dessen erster Zug auf eine ungültige Position zeigt
- **Testschritte:** `TicTacToeMain.play(...)` mit `-1`, `9`, `Integer.MIN_VALUE` und `Integer.MAX_VALUE`
- **Erwartetes Ergebnis:** `IllegalStateException` mit der Meldung `cannot play to position <Position>`
- **Bemerkung:** `-1` und `9` sind die Grenzwerte direkt neben dem gültigen Bereich 0–8; die Extremwerte zeigen, dass die Prüfung nicht überläuft

**TF-15 – `PerfectPlayer` findet den einzigen vernünftigen Zug**

- **Zielbezug:** TZ-09
- **Voraussetzung:** vier Stellungen, in denen genau ein Zug nicht sofort verliert: eigener Gewinn (X und O) oder Blockieren eines drohenden Gegnergewinns (X und O)
- **Testschritte:** `player.play(board, colorToPlay)`
- **Erwartetes Ergebnis:** Feld 2 in jeder der vier Stellungen

**TF-19 – `PerfectPlayer` verliert nie (exhaustiv)**

- **Zielbezug:** TZ-09
- **Voraussetzung:** Der `PerfectPlayer` spielt einmal als X und einmal als O. Der Gegner probiert in jedem Zug **jedes** legale Feld.
- **Testschritte:** rekursives Durchspielen aller Partien; jede vom Gegner gewonnene Partie wird als Zugfolge gesammelt
- **Erwartetes Ergebnis:** Die Liste der verlorenen Partien ist leer. Zusätzlich wird geprüft, dass mehr als 100 Partien gespielt wurden, damit der Test nicht leer durchläuft.
- **Bemerkung:** Das Spielfeld ist klein genug für einen vollständigen Nachweis. Stichproben mit festen Brettern würden nur einzelne Stellungen belegen.

---

## 8. Testplan und Offene Punkte

### Ablauf

| Schritt | Zeitpunkt | Verantwortlich | Werkzeug |
|---|---|---|---|
| Testfälle schreiben und erweitern | während der Entwicklung | Olivier | IntelliJ, JUnit |
| Testsuite lokal ausführen | vor jedem Commit | Olivier | `./gradlew build` |
| Tests, Coverage und Mutation prüfen | bei jedem Push / PR | GitHub Actions | `Build & Test`, `Mutation testing` |
| Merge nach `main` | nach grünem Build | Olivier | Pull Request, Branch Protection |

### Aktueller Teststand

| Kennzahl | Wert |
|---|---|
| Testklassen | 5 (`TicTacToeMainTest`, `GreedyPlayerTest`, `PerfectPlayerTest`, `ArchitectureTest`, `GameEndToEndTest`) |
| Hilfsklassen im Testcode | 2 (`TestBoards`, `ScriptedPlayer`) |
| Testmethoden | 31 (24 Unit/Integration/Architektur, 7 E2E) |
| Testfälle | 60 (49 und 11) |
| Branch Coverage | Gate 90 %; aktueller Wert im Chart (GitHub Pages) |
| Mutation Coverage / Test Strength | 92 % / 100 % (23.10.2026) |

Die Zahlen sind aus dem Quellcode gezählt; die tagesaktuellen Werte stehen im Build-Bericht
der CI (Tab *Actions*).

### Offene Punkte

Lücken im heutigen Zustand, bewusst nicht als geplante Massnahmen formuliert:

1. **PIT führt die E2E-Tests nicht aus.** Die nicht erkannten Mutanten im `HumanPlayer` sind die Folge; ein Vergleich Unit gegen E2E steht aus.
2. **Dünne Integrationsebene:** nur vier Testfälle; die E2E-Tests laufen mehrheitlich über `play()`, nur E2E-04 über `main()`.
3. **`PerfectPlayer` bevorzugt keine schnelleren Siege.** Alle gewinnenden Züge zählen gleich, der erste gewinnt. Das Ergebnis bleibt optimal, der Sieg kommt nur nicht immer am schnellsten.
4. **`PerfectPlayer` ist im Spiel nicht wählbar:** `main()` spielt Mensch gegen `GreedyPlayer`. Der Originalcode wurde bewusst nicht erweitert.
5. **Kein zweites Augenpaar** bei den späteren Pull Requests (Abschnitt 5).
6. **`HumanPlayer` blockiert unbegrenzt**, wenn nichts mehr eingegeben wird (Java-`Scanner` kennt kein Lese-Timeout). Für eine lokale Einbenutzer-Konsolenanwendung akzeptiert.

---

## 9. End-to-End-Tests

### Werkzeug und Ausführung

Die E2E-Tests liegen in `GameEndToEndTest` (Paket `e2e`, `@Tag("e2e")`) und nutzen
**JUnit Pioneer**: `@StdIo({...})` simuliert `System.in` zeilenweise, `StdOut` zeichnet die
Ausgabe auf. Sie laufen im eigenen Gradle-Task `./gradlew e2eTest`, damit der alltägliche
`./gradlew test` schnell bleibt, und sind über `check.dependsOn e2eTest` trotzdem Teil von
`./gradlew build` und damit der CI.

### Testfälle

| ID | Testfall | Erwartetes Verhalten |
|---|---|---|
| E2E-01 | Voller Spielablauf: CROSS gewinnt die oberste Reihe | Rückgabe `CROSS`; Ausgabe enthält die Gewinnmeldung |
| E2E-02 | Voller Spielablauf: CIRCLE gewinnt die mittlere Reihe | Rückgabe `CIRCLE`; Ausgabe enthält die Gewinnmeldung |
| E2E-03 | Voller Spielablauf: Unentschieden über alle 9 Felder | Rückgabe `null`; Ausgabe `it's a draw!` |
| E2E-04 | Echter Einstiegspunkt `main()`: Mensch gegen `GreedyPlayer` | Prompt wird ausgegeben, der Mensch gewinnt |
| E2E-05 | Nicht-numerischer Text | Fehlermeldung und erneute Aufforderung statt Absturz |
| E2E-06 | Parametrisiert, fünf Varianten: leere Zeile, Dezimalzahl, nur Leerzeichen, Zahl grösser als `Integer.MAX_VALUE`, Terminal-Escape-Sequenz | erneute Aufforderung; die Sequenz wird nur als Text behandelt, nicht ausgeführt |
| E2E-07 | Eine Eingabezeile mit 1'000'000 Zeichen | Ablehnung innerhalb von 5 Sekunden (`@Timeout`) |

### Gefundene Fehler

Beide Fehler im `HumanPlayer` fanden sich beim Schreiben der E2E-Tests; sie waren im
ursprünglichen Code bereits vorhanden und wurden erst durch diese Tests sichtbar.

1. **Absturz bei Fehleingabe.** `Integer.parseInt` warf bei nicht-numerischem Text eine
   unbehandelte `NumberFormatException`, ein Tippfehler beendete die ganze Partie. E2E-05 wurde
   zuerst geschrieben und schlug erwartungsgemäss fehl (test-first); danach wurde `play()` so
   angepasst, dass es in einer Schleife erneut fragt.
2. **Verlorene Eingaben durch mehrere `Scanner`.** Ein `Scanner` liest in einen eigenen Puffer
   voraus; mehrere Instanzen über demselben `System.in` verschlucken sich gegenseitig Zeilen. Alle
   `HumanPlayer` teilen sich jetzt einen gemeinsamen `Scanner`, der neu erzeugt wird, wenn sich
   `System.in` ändert (nötig, weil `@StdIo` den Strom pro Test ersetzt).

Beide Korrekturen waren ein Eingriff in den Originalcode (`HumanPlayer`, rund 35 Zeilen). Sie sind
nötig, weil der Code sonst nicht testbar war und abstürzte; die Spielregeln und die Bedienung
bleiben gleich.
