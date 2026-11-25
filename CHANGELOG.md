# Changelog

Alle relevanten Änderungen an TicTacTest. Format und Versionierung: siehe `RELEASE.md`.

## 1.1.4 - 2026-11-25

### Changed
- Backend: Begrüssung beim Start des Spiels

## 1.1.3 - 2026-11-25

### Changed
- Frontend: Seitentitel erweitert

## 1.1.2 - 2026-11-24

### Changed
- Frontend: Seitentitel angepasst

## 1.1.1 - 2026-11-24

### Changed
- Frontend: neue Akzentfarbe

## 1.1.0 - 2026-11-18

### Added
- Web-Konsole-Frontend (`frontend/`) und Release-Asset `tictactest-frontend-1.1.0.zip`

## 1.0.1 - 2026-10-20

### Fixed
- Ungültige Eingaben (z. B. Buchstaben statt einer Zahl) beenden das Spiel nicht mehr mit einem Absturz; es wird erneut nach einem Feld gefragt
- Mehrere `HumanPlayer` teilen sich einen gemeinsamen `Scanner`, damit keine Eingaben verloren gehen

### Added
- End-to-End-Tests, die ganze Spiele über simulierte Konsoleneingabe durchspielen

## 1.0.0 - 2026-10-20

### Added
- Erste veröffentlichte Version: TicTacToe als Konsolenspiel, als ausführbares JAR (`java -jar tictactest-1.0.0.jar`, benötigt Java 25 oder neuer)
- Zwei Spielertypen: `HumanPlayer` (Eingabe über die Konsole) und `GreedyPlayer`
- Automatisierte Tests (JUnit, AssertJ, parametrisierte Tests) mit JaCoCo-Coverage-Report
- Mutation Testing mit PIT
- CI-Pipeline mit GitHub Actions in einem versionierten Dev-Container-Image (GHCR)
- Zeitlicher Verlauf der Testabdeckung auf GitHub Pages
- Release-Prozess: Release nur per Git-Tag, JAR als GitHub Release
