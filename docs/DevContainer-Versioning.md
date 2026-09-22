# DevContainer-Versionierung und Freigabeprozess

Beschreibt das Versionierungs- und Freigabekonzept für das DevContainer-Image
(`ghcr.io/viertje/450-tictactest-ocv`). Stand: 16.09.2026. Ergänzt
`docs/CI-CD-Pipeline.md` um den Teil, der dort nur kurz erwähnt wird.

## Ziel

Auftrag 3 verlangt drei Dinge gleichzeitig:

1. eine sinnvolle Versionierung des DevContainers (`v1.0.0`, `v1.0.1`, ...),
2. einen Mechanismus, der verhindert, dass noch nicht offiziell freigegebene
   Images verwendet werden,
3. dass ein erfolgreicher Push automatisch einen Pull Request eröffnet, der
   die aktualisierte Version einsetzt, und dass CI-Jobs sowie lokale
   Entwicklungsumgebungen die neueste (freigegebene) Version automatisch
   verwenden.

Punkt 2 und 3 lassen sich mit **demselben Mechanismus** lösen: dem
automatisch erstellten Pull Request. Solange dieser nicht gemergt ist, zeigt
nichts im Repository auf die neue Version — das Mergen selbst ist die
offizielle Freigabe.

## Versionierungskonzept

- Semantic Versioning: `vMAJOR.MINOR.PATCH`.
- Jede Änderung an `.devcontainer/Dockerfile`, die auf `main` gepusht wird,
  löst automatisch eine neue **PATCH**-Version aus (`v1.0.0` → `v1.0.1` →
  `v1.0.2` ...).
- Bewusst **kein** automatisches Erkennen von Minor/Major-Änderungen (z. B.
  über Conventional Commits): das würde eine Commit-Konvention voraussetzen,
  die im Projekt nicht konsequent eingehalten wird, und mehr Logik im
  Workflow für einen Fall, der hier selten vorkommt (Wechsel des
  Base-Images, grössere strukturelle Änderungen). Bei einem bewussten
  Breaking Change wird der nächste Git-Tag stattdessen von Hand auf die
  gewünschte Minor/Major-Version gesetzt, bevor der nächste automatische
  Lauf wieder patcht.
- **Git-Tags (`vX.Y.Z`) sind die einzige Quelle der Wahrheit** für die
  zuletzt veröffentlichte Version. Es gibt bewusst keine zusätzliche
  `VERSION`-Datei, die aus dem Takt geraten könnte. Eine Alternative wäre
  gewesen, statt einer fortlaufenden Versionsnummer direkt den Commit-Hash
  als Tag zu verwenden (dafür entstünde keine "nächste Version"-Berechnung
  und damit auch kein denkbarer Kollisionsfall) — bewusst nicht gewählt,
  weil ein roher Hash für Menschen weder lesbar noch geordnet ist und die
  Aufgabenstellung ausdrücklich eine sinnvolle, semantische Versionierung
  verlangt. Der Commit-SHA-Tag existiert trotzdem weiterhin (siehe unten),
  einfach nicht als das, was `build.yml`/`devcontainer.json` referenzieren.
- Das Image wird zusätzlich weiterhin mit `:latest` und der Commit-SHA
  getaggt (Debugging/Nachvollziehbarkeit). Diese beiden Tags werden aber von
  nichts im Repository konsumiert — massgeblich ist ausschliesslich die
  `vX.Y.Z`-Version, die in `build.yml` und `devcontainer.json` gepinnt ist.

## Freigabeprozess: Pull Request als Gate

```
.devcontainer/Dockerfile ändert sich, Push auf main
        |
        v
Workflow "Publish container image", Job "publish"
  - baut das Image
  - ermittelt die nächste Version (Patch-Bump über den vorhandenen Git-Tags)
  - pusht ghcr.io/.../450-tictactest-ocv:latest, :<sha> und :vX.Y.Z
  - erstellt und pusht den Git-Tag vX.Y.Z
        |
        v
Job "open-release-pr"
  - schliesst noch offene, ältere Freigabe-PRs automatisch (siehe unten)
  - ersetzt den Image-Tag in build.yml und devcontainer.json durch :vX.Y.Z
  - eröffnet automatisch einen neuen Pull Request
        |
        v
   Pull Request wird geprüft und gemergt  <-- offizielle Freigabe
        |
        v
build.yml (CI) und devcontainer.json (lokale DevContainer)
verwenden ab jetzt automatisch die neue, freigegebene Version
```

Bis zum Merge bleiben `build.yml` und `devcontainer.json` unverändert auf
der zuvor freigegebenen Version stehen — obwohl das neue Image bereits
erfolgreich gebaut, getaggt und in die GHCR gepusht wurde. Ein Image
existiert also in zwei Zuständen: *veröffentlicht* (in der GHCR vorhanden,
per Tag ansprechbar) und *freigegeben* (von `build.yml`/`devcontainer.json`
tatsächlich referenziert). Nur der zweite Zustand wird automatisch von CI
oder einer neu erstellten lokalen Umgebung verwendet.

## Nur eine offene Freigabe-PR gleichzeitig

Wird `.devcontainer/Dockerfile` mehrfach hintereinander geändert, bevor die
erste Freigabe-PR gemergt wurde, entstünden ohne Gegenmassnahme mehrere
offene PRs gleichzeitig — jede mit einem anderen Versions-Tag, alle auf
dieselben zwei Zeilen in `build.yml`/`devcontainer.json` zielend. Das macht
es für die reviewende Person unnötig leicht, versehentlich eine ältere
Version freizugeben, nachdem bereits eine neuere veröffentlicht wurde.

Deshalb sucht der Job `open-release-pr`, bevor er einen neuen Pull Request
öffnet, über die GitHub-CLI (`gh pr list`) nach noch offenen PRs, deren
Branch mit `chore/devcontainer-` beginnt, kommentiert dort kurz, wodurch sie
ersetzt wurden, und schliesst sie (`gh pr close`). Am Ende existiert dadurch
garantiert höchstens eine offene Freigabe-PR gleichzeitig — die neueste.
Keine zusätzlichen Rechte nötig: `pull-requests: write`, das der Job für das
Erstellen der PR ohnehin schon hat, deckt Auflisten, Kommentieren und
Schliessen mit ab.

## Wie CI und lokale Umgebungen automatisch aktualisieren

- **CI (`build.yml`):** `container.image` ist direkt auf `ghcr.io/.../450-tictactest-ocv:vX.Y.Z`
  gepinnt. Nach dem Merge der Freigabe-PR verwendet jeder nachfolgende
  Workflow-Lauf automatisch die neue Version — ohne manuellen Eingriff.
- **Lokal (`devcontainer.json`):** Das `image`-Feld ist auf denselben Tag
  gepinnt. Nach einem `git pull` auf `main` zieht ein "Rebuild Container"
  (VS Code) bzw. der entsprechende Neuaufbau in IntelliJs Dev-Containers-
  Unterstützung automatisch die neue Version.

Beide Dateien werden vom selben `sed`-Schritt im Workflow aktualisiert, weil
beide denselben `ghcr.io/viertje/450-tictactest-ocv:<tag>`-String enthalten
— ein Ersetzungsschritt reicht für beide Konsument:innen.

## Erstes Bootstrapping

Der Workflow startet automatisch nur bei einer Änderung an
`.devcontainer/Dockerfile`. Für die allererste Version (`v1.0.0`) ist daher
einmalig ein manueller Start über `workflow_dispatch` (Tab *Actions* im
Repository) nötig. Dieser Lauf findet noch keinen `v*.*.*`-Tag, setzt
`v1.0.0` an und eröffnet die erste Freigabe-PR.

## Voraussetzungen im Repository

- *Settings → Actions → General → Workflow permissions*: "Allow GitHub
  Actions to create and approve pull requests" muss aktiviert sein, sonst
  schlägt das Öffnen des Pull Requests ab.
- Der `GITHUB_TOKEN` braucht `contents: write` (Git-Tag pushen im
  `publish`-Job) und `pull-requests: write` (PRs auflisten/kommentieren/
  schliessen im `open-release-pr`-Job) — im Workflow explizit pro Job
  gesetzt, nicht global.
- **Repo-Secret `RELEASE_PAT`** (classic Personal Access Token, Scopes
  `public_repo` + `workflow`): notwendig, weil die Freigabe-PR
  `build.yml` selbst ändert (den Image-Tag). GitHub verweigert das für
  `GITHUB_TOKEN` **grundsätzlich** — es gibt keinen `permissions:`-Key, der
  Schreibzugriff auf `.github/workflows/*` freischaltet (auch nicht ein
  vermeintliches `workflows: write`, das ist keine gültige Property und
  wird von der IDE-Schema-Validierung zurückgewiesen). Der einzige Ausweg
  ist ein Token, das nicht `GITHUB_TOKEN` ist: ein klassischer PAT mit
  `workflow`-Scope. Erstellen unter
  [github.com/settings/tokens](https://github.com/settings/tokens) →
  "Generate new token (classic)" → Scopes `public_repo` + `workflow`
  ankreuzen, dann unter *Settings → Secrets and variables → Actions → New
  repository secret* als `RELEASE_PAT` hinterlegen. Nur der Schritt
  "Create pull request" in `open-release-pr` verwendet ihn (via
  `token: ${{ secrets.RELEASE_PAT }}`), alle anderen Schritte bleiben bei
  `GITHUB_TOKEN`.

## Bekannte Einschränkungen

1. **Immer Patch-Bump**, unabhängig von der tatsächlichen Grösse der
   Änderung — bewusste Vereinfachung, siehe oben.
2. **"Veröffentlicht" heisst nicht "im Einsatz".** Git-Tags und GHCR-Images
   entstehen unabhängig davon, ob die zugehörige Freigabe-PR jemals gemergt
   wird — ein Image kann veröffentlicht sein, ohne je freigegeben zu werden
   (z. B. wenn seine PR automatisch durch eine neuere ersetzt wird, siehe
   oben). Das alte Image bleibt trotzdem unter seinem eigenen Tag in der
   GHCR abrufbar.
3. **Kein Merge-Loop:** Der Publish-Workflow löst nur bei Änderungen an
   `.devcontainer/Dockerfile` aus. Das Mergen der Freigabe-PR ändert nur
   `build.yml` und `devcontainer.json`, triggert den Publish-Workflow also
   nicht erneut.
4. **Rollback:** Bei einem fehlerhaften Release genügt ein Revert des
   gemergten Freigabe-PRs (oder das manuelle Zurücksetzen von
   `build.yml`/`devcontainer.json` auf die vorherige Version). Das alte
   Image bleibt unter seinem eigenen Tag in der GHCR weiterhin verfügbar,
   da Tags nie überschrieben oder gelöscht werden.
5. **`RELEASE_PAT` ist ein klassischer PAT, kein feingranularer.** Läuft
   irgendwann ab bzw. muss manuell rotiert werden (GitHub erzwingt bei
   klassischen PATs kein Ablaufdatum, aber ein selbst gesetztes ist
   empfehlenswert). Läuft er ab, schlägt nur der "Create pull request"-
   Schritt fehl (401/403) — `publish` (Image bauen/taggen) bleibt davon
   unberührt.
