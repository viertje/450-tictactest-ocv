# Testkonzept – TicTacTest

Modul 450 – TicTacToe-Projekt

> Dieses Dokument beschreibt den **IST-Zustand** des Projekts: dokumentiert ist
> nur, was heute tatsächlich im Repository vorhanden ist.

## 1. Einleitung

### 1.1 Zweck
Dieses Testkonzept beschreibt die Teststrategie, die Teststruktur und die
konkreten Testziele des TicTacToe-Projekts. Es dient als Übersicht darüber, was
getestet wird und wie die Tests organisiert sind.

### 1.2 Testgegenstand
Getestet wird die Spiellogik der Klasse `TicTacToeMain`
(`src/main/java/ch/bbw/m450/tictactoe/`), insbesondere:
- `isWin(Stone[] board, Stone color)` – Erkennung einer Gewinnstellung
- `play(TicTacToePlayer x, TicTacToePlayer o)` – vollständiger Spielablauf

## 2. Teststrategie

### 2.1 Testart
Es handelt sich ausschliesslich um **automatisierte Unit-Tests** auf Ebene der
Spiellogik. Manuelle Tests, Integrations- oder UI-Tests sind aktuell nicht Teil
des Konzepts.

### 2.2 Verwendete Werkzeuge (IST)
| Werkzeug | Version | Zweck |
|----------|---------|-------|
| JUnit 5 (Jupiter) | 5.11.3 | Test-Framework, Testausführung |
| AssertJ | 3.26.3 | Fluent Assertions |
| Gradle (Wrapper) | 9.1.0 | Build und Testausführung (`./gradlew test`) |
| GitHub Actions | – | CI: Tests bei Push/PR auf `main` |

Konfiguriert in `build.gradle` (`useJUnitPlatform()`), CI in
`.github/workflows/gradle.yml`.

### 2.3 Testausführung
- Lokal: `./gradlew test` oder über die IDE (IntelliJ)
- CI: automatisch bei Push und Pull Request auf `main` (Java 25, Temurin)

## 3. Teststruktur

Die Tests liegen unter `src/test/java/ch/bbw/m450/tictactoe/` und sind in drei
Dateien organisiert:

### 3.1 `TicTacToeFixtures` (Helper & Fixtures)
Zentrale, wiederverwendbare Testbausteine:
- **Helper** `boardOf(String)` – baut ein Spielbrett aus kompakter
  String-Notation (`X` = CROSS, `O` = CIRCLE, `.` = leer), inkl. Längenprüfung.
- **Fixtures** – vordefinierte Board-Zustände:
  `emptyBoard()`, `crossWinningRow()`, `circleWinningColumn()`.
- **Argument-Provider** für Parameterized Tests:
  `winningBoardsForCross()` (8 Gewinnkonstellationen) und
  `nonWinningBoards()` (5 Nicht-Gewinn-Konstellationen).

### 3.2 `DummyTest`
Zwei Tests, die nachweisen, dass JUnit 5 und AssertJ korrekt eingebunden sind.

### 3.3 `TicTacToeMainTest`
Fünf Einzeltests (`@Test`) sowie drei Parameterized Tests (`@ParameterizedTest`)
der Spiellogik. Spieler-Instanzen werden über eine `@BeforeEach setUp()`-Fixture
(`xPlayer`, `oPlayer`) bereitgestellt. Board-Zustände kommen aus
`TicTacToeFixtures`. Die Parameterized Tests nutzen `@MethodSource` (Provider aus
den Fixtures) und `@CsvSource` (inline), um mehrere Board-Konstellationen mit
einer Testmethode zu prüfen. Alle Tests folgen dem GIVEN-WHEN-THEN-Muster.

## 4. Testziele und Testfälle

Testziele sind nummeriert und den konkreten Testfällen zugeordnet.

### Testziel Z1: Test-Infrastruktur ist korrekt aufgesetzt
| ID | Testfall | Datei | Prüft |
|----|----------|-------|-------|
| T1.1 | `junitDummy` | `DummyTest` | JUnit-5-Assertions funktionieren |
| T1.2 | `assertJDummy` | `DummyTest` | AssertJ-Assertions funktionieren |

### Testziel Z2: Gewinnstellungen werden korrekt erkannt
| ID | Testfall | Datei | Prüft |
|----|----------|-------|-------|
| T2.1 | `isWin_detectsWinningRow` | `TicTacToeMainTest` | Drei Kreuze in einer Reihe = Sieg |
| T2.2 | `isWin_detectsWinningColumn` | `TicTacToeMainTest` | Drei Kreise in einer Spalte = Sieg |
| T2.3 | `isWin_returnsFalseForEmptyBoard` | `TicTacToeMainTest` | Leeres Brett = kein Sieg |

### Testziel Z3: Der Spielablauf verhält sich korrekt
| ID | Testfall | Datei | Prüft |
|----|----------|-------|-------|
| T3.1 | `play_twoGreedyPlayersResultInCrossWinner` | `TicTacToeMainTest` | Vollständiges Spiel liefert CROSS als Sieger |
| T3.2 | `play_rejectsIdenticalPlayers` | `TicTacToeMainTest` | Gleiche Spieler-Instanz wirft `IllegalArgumentException` |

### Testziel Z4: Alle Board-Konstellationen werden korrekt bewertet (Parameterized)
| ID | Testfall | Datei | Prüft |
|----|----------|-------|-------|
| T4.1 | `isWin_detectsAllWinningConstellations` (`@MethodSource`) | `TicTacToeMainTest` | Alle 8 Gewinnlinien (Reihen, Spalten, Diagonalen) für CROSS werden als Sieg erkannt |
| T4.2 | `isWin_returnsFalseForNonWinningConstellations` (`@MethodSource`) | `TicTacToeMainTest` | 5 Konstellationen ohne Gewinnlinie liefern `false` |
| T4.3 | `isWin_detectsEachWinningLineForCross` (`@CsvSource`) | `TicTacToeMainTest` | Jede der 8 Gewinnlinien (inline als CSV) wird als Sieg erkannt |

## 5. Testfälle im Detail (GIVEN-WHEN-THEN)

Die vollständige GIVEN-WHEN-THEN-Beschreibung aller Testfälle ist in
[`TESTS.md`](TESTS.md) dokumentiert. Jeder Testfall dort ist mit der passenden
Testziel-ID (`T1.1` … `T4.3`) markiert, sodass sich die Bezüge zwischen Zielen
und Testfällen in beide Richtungen nachvollziehen lassen.

## 6. Abgrenzung (nicht getestet)

Aktuell nicht durch Tests abgedeckt (IST-Zustand):
- `HumanPlayer` (liest von der Konsole, nicht automatisiert getestet)
- Konsolenausgabe / `toString(Stone[])`
- Ungültige Board-Grössen ausserhalb der Helper-Prüfung


## 7. Feedback Timeo Lutz

Insgesamt ein sehr gutes Testkonzept. Die Struktur ist klar gegliedert, der IST-Zustand wird konsequent und ehrlich beschrieben, und die Bezüge zwischen den Testzielen (TZ-01–07) und den Testfällen (TF-01–12) sind durchgehend nachvollziehbar.
 
Stärken:
 
Vollständige, professionelle Kapitelstruktur mit übersichtlichen Tabellen
Testziele nummeriert und lückenlos mit den Testfällen verknüpft
Konsequente IST-Beschreibung — bestehende Lücken (ungetesteter HumanPlayer, CI ohne Testausführung, keine Coverage) werden offen benannt statt versteckt
Gute methodische Tiefe: saubere Abgrenzung Unit- vs. Integrationstest, Grenzwertanalyse, Einsatz von Test-Doubles (ScriptedPlayer) und Parameterized Tests
Verbesserungspotenzial:
 
Nur 3 von 12 Testfällen sind im Detail beschrieben — hier wäre mehr Vollständigkeit wünschenswert
Die Abdeckung von play() ist mit nur zwei Spielverläufen dünn (z.B. fehlt ein Sieg durch CIRCLE)
Kleine Ungenauigkeit bei der Versionsangabe (JUnit Jupiter "6.1.3")
Bewertung: 5.5 — eine überdurchschnittliche Arbeit, die alle Anforderungen erfüllt und methodisch überzeugt. Für die Bestnote fehlen lediglich vollständige Testfall-Details und eine etwas breitere play()-Abdeckung.
