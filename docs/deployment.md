# Deployment von TicTacTest (Auftrag 2 und 3)

Stand: 24.11.2026. Die Abschnitte mit "Ergebnis:" werden nach dem jeweiligen Test
mit den eigenen Beobachtungen ergänzt.

## Überblick

```
Browser --> Frontend (S3 Static Website, bettet das Terminal ein)
   |
   v
EC2-Instanz: ttyd (Port 7681) --> java -jar <Jar aus dem Release>
             nginx (Port 80)  --> /version (laufende Backend-Version)
```

**Grundregel: Infrastruktur und Anwendung sind getrennt.**

| | Infrastruktur | Anwendung |
|---|---|---|
| Was | Instanz, feste IP, Buckets, Security Group, Dienst, nginx, Frontend-Konfiguration | Jar und Frontend-Archiv eines Releases |
| Werkzeug | OpenTofu (`infra/`), `tofu apply` nur lokal | Deployment aus dem Release (`deploy.yml`) |
| Änderung | selten, bewusst | bei jedem Release |

Ein neuer Release braucht weder `tofu apply` noch eine neue Instanz.

## Entscheidungen

**Wie kommt das Jar auf die Instanz?** Die Pipeline lädt das Jar aus dem
GitHub-Release herunter und kopiert es in einen privaten S3-Bucket. Per
AWS Systems Manager (SSM) führt die Instanz dann das Skript
`tictactest-deploy <Version>` aus, holt das Jar über ihr Instance Profile aus
dem Bucket und startet den Dienst neu.

- Das Repository ist privat. Die Instanz müsste sich sonst bei GitHub mit einem
  Token anmelden, das dauerhaft auf dem Server läge.
- Es liegen keine Access Keys auf der Instanz: das Instance Profile genügt.
- Verworfene Alternativen: Direktdownload vom GitHub-Release (Token nötig),
  Kopieren per SSH (Port 22 für die Pipeline öffnen, privater Schlüssel in GitHub).

**Warum steht das Jar nicht in `user_data`?** `user_data` läuft nur beim ersten
Start der Instanz. Eine neue Version würde dort nicht ankommen. Mit
`user_data_replace_on_change = true` würde jede Änderung die Instanz löschen und neu
erstellen. Das widerspricht der Grundregel: die laufende Version ginge verloren und
jeder Release bräuchte ein `tofu apply`. Darum installiert `user_data` nur die
Umgebung (Java, ttyd, nginx, Dienst, Deploy-Skript) und enthält keine Version.

**Welche Version läuft nach einem Neustart?** Das Jar liegt auf der Platte der Instanz
(`/opt/tictactest/releases/`), der Link `current.jar` zeigt auf die aktive Version und
`VERSION` enthält die Nummer. Der systemd-Dienst ist aktiviert und startet beim Booten
genau dieses Jar. Nach `tofu destroy` und `tofu apply` ist die Platte leer: dann muss
die gewünschte Version neu deployt werden.

**Security Group: jede offene Port-Regel begründet**

| Port | Wer | Warum |
|---|---|---|
| 7681 | alle | Die Web-Konsole: Spielende verbinden sich mit ihrem Browser |
| 80 | alle | Versionsabfrage (nginx): Frontend und Pipeline lesen die laufende Version |
| 22 | nur eigene IP, standardmässig geschlossen | Nur zur Fehlersuche. Deployments laufen über SSM, nicht über SSH |

**SSM oder SSH für GitHub Actions?**

| | SSM `send-command` | SSH |
|---|---|---|
| Dafür | kein offener Port, kein Schlüssel in GitHub, Zugriff über AWS-Rechte steuerbar | einfach und überall verfügbar |
| Dagegen | braucht den SSM-Agent und passende Rechte der Rolle | Port 22 müsste für wechselnde GitHub-IP-Adressen offen sein, langlebiger privater Schlüssel als Secret |

**Frontend-Konfiguration.** OpenTofu schreibt `config.js` (URL der Web-Konsole und der
Versionsabfrage) in den Bucket, denn nur die Infrastruktur kennt die Adresse. Das
Frontend-Deployment synchronisiert mit `--delete`, aber mit `--exclude config.js`:
ausgeschlossene Dateien werden weder hochgeladen noch gelöscht. Das Archiv enthält
zwar ein `config.js` mit lokalen Werten, es überschreibt die Datei in der Cloud aber nie.

**Was darf nie in den öffentlichen Bucket?** Zugangsdaten, Schlüssel, State-Dateien,
`.tfvars` und das Backend-Jar (liegt im privaten Artefakt-Bucket).

**Outputs: vertraulich oder nicht?** Keiner der Werte ist ein Secret. Adressen und
Namen (Spiel-URL, Konsolen-URL, Elastic IP) sieht jede Besucherin ohnehin. Bucket-Namen
und Instanz-ID sind nicht geheim, aber auch nicht für die Öffentlichkeit gedacht. Sie
liegen deshalb in GitHub **Variables**, nicht in der Dokumentation oder im Code.

## Deployment von Hand (Auftrag 2, Teil 3)

Das Skript `scripts/deploy-manual.ps1` führt dieselben Schritte aus wie später die
Pipeline. Voraussetzungen: Lab gestartet, `aws-school` aktiv, `gh` angemeldet.

| Schritt | Befehl |
|---|---|
| 0. Infrastruktur erstellen (einmalig) | `tofu init`, `tofu plan`, `tofu apply` im Ordner `infra` |
| 1. Release-Dateien herunterladen | `gh release download v<Version> --repo viertje/450-tictactest-ocv --dir <Ordner>` |
| 2. Jar in den privaten Bucket | `aws s3 cp tictactest-<Version>.jar s3://<artifact_bucket>/releases/<Version>/` |
| 3. Instanz deployt das Jar | `aws ssm send-command --instance-ids <id> --document-name AWS-RunShellScript --parameters file://params.json` (Befehl: `/usr/local/bin/tictactest-deploy <Version>`) |
| 4. Auf das Ende warten, Ausgabe lesen | `aws ssm wait command-executed ...`, `aws ssm get-command-invocation ...` |
| 5. Version prüfen | Abfrage der Versionsadresse (`version_url`) liefert `<Version>` |
| 6. Frontend entpacken und hochladen | `aws s3 sync <Ordner> s3://<frontend_bucket> --delete --exclude "config.js"` |
| 7. Frontend prüfen | `<game_url>/version.txt` liefert `<Version>` |

Aufruf: `.\scripts\deploy-manual.ps1 -Version 1.1.0`, nur Frontend mit `-SkipBackend`.

**Release A** (Version): Ergebnis: _(eintragen: Version, ob die richtigen Versionen im Browser angezeigt werden)_

**Release B** (Version, sichtbare Änderung im Frontend): Ergebnis: _(eintragen)_ `tofu plan`
zeigt danach: _(eintragen: "No changes")_

## Robustheit (Auftrag 2, Teil 4)

1. **Session beenden und neu starten.** Ergebnis: _(läuft die App wieder? gleiche URL? gleiche Version?)_
2. **Einrichtung der Instanz ändern** (z. B. `max_clients` in `user_data`). Ergebnis: _(was zeigt `tofu plan`, was passiert mit Instanz, IP und Version?)_
   Hintergrund: Mit `user_data_replace_on_change = false` wird die Instanz nicht ersetzt,
   das neue `user_data` läuft aber nicht erneut (cloud-init führt es nur beim ersten Start aus).
3. **`tofu destroy`, dann `tofu apply`.** Ergebnis: _(welche Werte ändern sich: IP, Bucket-Namen, Instanz-ID? Was ist nötig, bis wieder dieselbe Version läuft?)_

## Secrets-Konzept (Auftrag 3, Teil 1)

**Welche Zugangsdaten braucht die Pipeline?**

| Zugangsdaten | Woher | Gültigkeit |
|---|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | AWS Learner Lab, "AWS Details" | bis zum Ende der Lab-Session (etwa 4 Stunden) |
| `GITHUB_TOKEN` | wird von GitHub pro Lauf automatisch erzeugt | nur für die Dauer des Laufs, nur Lesezugriff auf Inhalte |

**Wohin gehört was?**

| | GitHub Secrets | GitHub Variables |
|---|---|---|
| Inhalt | verschlüsselt, nach dem Speichern nicht mehr lesbar, in Logs maskiert | normaler Klartext, einsehbar |
| Unsere Werte | die drei AWS-Zugangsdaten | `AWS_REGION`, `INSTANCE_ID`, `ARTIFACT_BUCKET`, `FRONTEND_BUCKET`, `VERSION_URL`, `FRONTEND_URL` |

**Abgelaufene Credentials.** Die Zugangsdaten laufen nach jeder Session ab.

- Nach "Start Lab" setzt `scripts/refresh-aws-secrets.ps1` die drei Secrets neu (aus
  `~/.aws-school/credentials`).
- Löst ein Release mit abgelaufenen Zugangsdaten das Deployment aus, schlägt der Schritt
  "Check AWS credentials" mit einer klaren Meldung fehl. Der Release selbst existiert dann
  trotzdem. Nach dem Auffrischen der Secrets wird die Version mit dem Workflow "Deploy"
  (Run workflow, Version eingeben) nachgeholt.
- Ergebnis des Tests mit abgelaufenen Credentials: _(eintragen)_

**Wer sieht oder nutzt die Secrets?**

- Wer Schreibrechte im Repository hat, kann Workflows ändern, die ein Secret verwenden.
  Gelesen werden kann es nach dem Speichern nicht.
- In Logs werden Secrets maskiert (`***`). Trotzdem werden sie nie mit `echo` ausgegeben,
  und die Logs werden nach jedem Lauf geprüft.
- Pull Requests aus Forks erhalten keine Secrets. Das Deployment läuft ausserdem nur bei
  einem Tag-Push, nicht bei Pull Requests.

**Schaden bei einem Leak.** Mit den Zugangsdaten könnte jemand bis zum Ende der Session
im Lern-Konto Ressourcen anlegen oder löschen und das Budget aufbrauchen. Die Folgen sind
begrenzt, weil das Konto eine Sandbox ist und die Zugangsdaten bald ablaufen.
Reaktion: Lab-Session beenden (das macht die Zugangsdaten ungültig), Secrets in GitHub
löschen oder ersetzen, Konto auf unbekannte Ressourcen prüfen.

**Wie macht man es in einem echten Unternehmen?** Mit **OIDC** (OpenID Connect): GitHub
Actions weist sich bei AWS mit einem kurzlebigen Token aus und übernimmt damit eine
IAM-Rolle, deren Vertrauensrichtlinie auf Repository, Branch oder Environment
eingeschränkt ist. Es gibt keine gespeicherten, langlebigen Schlüssel. Im Learner Lab
geht das nicht, weil dort keine IAM-Rollen erstellt werden dürfen.

## Änderungen zwischen Releases erkennen (Auftrag 3, Teil 2)

Das Skript `scripts/detect-changes.sh` vergleicht den neuen Tag mit dem **vorherigen
Release-Tag**:

| Pfad | Gehört zu |
|---|---|
| `src/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `gradlew` | Backend (Jar) |
| `frontend/` | Frontend |
| `.github/workflows/release.yml`, `.github/workflows/deploy.yml` | beides: sie ändern, wie deployt wird, und nur ein vollständiges Deployment beweist, dass sie noch funktionieren |
| `docs/`, `CHANGELOG.md`, `README.md`, andere Skripte | nichts |

- **Warum der vorherige Tag und nicht der letzte Commit?** Zwischen zwei Releases liegen
  meist mehrere Commits. Der letzte Commit könnte nur eine Doku-Änderung sein und eine
  frühere Frontend-Änderung verdecken. Der Release muss alles ausliefern, was seit dem
  letzten Release geändert wurde.
- **Erster Release:** Es gibt keinen vorherigen Tag, also gilt alles als neu und beide
  Teile werden deployt.
- **Warum hilft der `paths`-Filter von GitHub Actions nicht?** Er prüft die Dateien der
  gepushten Commits und wird bei einem Tag-Push nicht ausgewertet. Er kennt den
  vorherigen Release nicht.
- Das Skript wurde mit einer Testhistorie geprüft: nur Doku, nur Backend, nur Frontend,
  beides, Frontend plus spätere Doku-Änderung, Workflow-Änderung und erster Release.

## Backend und Frontend deployen (Auftrag 3, Teil 3 und 4)

**Backend:** Jar in den Bucket, SSM-Befehl, dann Warten auf das Ende des Befehls und
Prüfen der Versionsabfrage. Meldet die Instanz die neue Version nicht innerhalb von
2 Minuten, schlägt der Workflow fehl. Das Skript auf der Instanz startet das neue Jar
vorher kurz: lässt es sich nicht starten, bleibt die laufende Version aktiv.

**Gleichzeitige Deployments** verhindert `concurrency: group: deploy-tictactest`: ein
zweiter Lauf wartet, bis der erste fertig ist.

**Was passiert mit Spielenden beim Neustart?** Der Dienst wird neu gestartet, die
Verbindungen brechen ab und laufende Partien gehen verloren. Verhindern könnte man das
mit einem sanften Wechsel: neue Version auf einem zweiten Port starten, neue Verbindungen
dorthin leiten und die alte Version beenden, wenn ihre letzten Partien fertig sind.
Einfacher: nur ausserhalb der Spielzeiten deployen. (Nur beschrieben, nicht umgesetzt.)

**Frontend:** Der Inhalt des Archivs ersetzt das bisherige Frontend (`--delete` entfernt
nicht mehr vorhandene Dateien), `config.js` bleibt erhalten. Danach prüft die Pipeline,
dass `version.txt` die neue Version liefert und `config.js` noch die Konsolen-URL enthält.

**Was passiert, wenn das Deployment `config.js` löscht?** Das Frontend kennt die URL der
Web-Konsole nicht mehr: es zeigt eine Fehlermeldung und kein Terminal. Verhindert wird das
durch `--exclude config.js`. Die Prüfung nach dem Deployment würde es zusätzlich melden.

## Gezielt eine Version deployen (Auftrag 3, Teil 5)

Der Workflow "Deploy" lässt sich unter *Actions* mit "Run workflow" und der Versionsnummer
starten. Er deployt Backend und Frontend dieser Version.

- **Rollback** (v1.3.0 läuft, v1.2.0 wird deployt): Ergebnis: _(eintragen)_
- **Kaputter Release:** Ergebnis: _(eintragen: schlägt die Prüfung fehl? wie lange bis wieder eine funktionierende Version läuft?)_
- **Nach `tofu destroy` und `tofu apply`:** Ergebnis: _(eintragen: GitHub Variables mit `scripts/set-github-variables.ps1` neu setzen, Secrets auffrischen, aktuelle Version mit "Deploy" einspielen)_

## Nachweis der Trennung (Auftrag 3, Teil 6)

| Fall | Erwartung | Workflow-Lauf |
|---|---|---|
| nur Frontend geändert | Frontend-Version neu, Backend-Version gleich | _(Link oder Screenshot)_ |
| nur Backend geändert | Backend-Version neu, Frontend nicht neu deployt | _(Link oder Screenshot)_ |
| beides geändert | beide neu | _(Link oder Screenshot)_ |
| Push auf `main` ohne Tag | es wird nichts deployt | _(Link oder Screenshot)_ |

## Reflexion und Ausblick: Umgebungen und Promotion (Auftrag 3, Teil 7)

**Weg einer Änderung bis in die Umgebung**

```
Backend:   Commit -> Pull Request -> Tests grün -> Merge nach main
           -> CHANGELOG-Eintrag + Tag vX.Y.Z -> Workflow "Release": verify, build (Tests + Jar), release
           -> changes (Backend geändert) -> deploy: Jar -> S3 -> SSM -> Dienst neu gestartet -> Versionsprüfung

Frontend:  Commit -> Pull Request -> Tests grün -> Merge nach main
           -> CHANGELOG-Eintrag + Tag vX.Y.Z -> Workflow "Release": ... Frontend-Archiv im Release
           -> changes (Frontend geändert) -> deploy: Archiv -> S3-Bucket (ohne config.js) -> Prüfung
```

**Automatisch:** Tests, Jar und Archiv bauen, Release erstellen, Änderungen erkennen,
deployen, Version prüfen. **Noch von Hand:** `tofu apply`, Secrets nach jedem Lab-Start
auffrischen, GitHub Variables nach neuer Infrastruktur setzen, CHANGELOG und Tag setzen.

**Dieselbe Infrastruktur dreimal (dev, staging, prod)**

- Der Code bleibt derselbe, die Umgebung wird zur Variable (`environment`, z. B. über
  `name_prefix` und eine `.tfvars`-Datei pro Umgebung). Jede Umgebung braucht ihren
  eigenen State, am besten in einem Remote-Backend.
- Gleich: Aufbau (Instanz, Dienst, nginx, Buckets, Deploy-Skript).
- Unterschiedlich: Namen, Adressen, Instanzgrösse und Client-Limit, Zugriffsregeln
  (prod strenger), eigene Secrets und Variables pro Umgebung (GitHub Environments).

**Promotion-Strategie.** Ein Release wird **einmal** gebaut. Dieselben Artefakte (Jar,
Frontend-Archiv) wandern von dev über staging nach prod, es wird nicht pro Umgebung neu
gebaut. Sonst testet man in staging etwas anderes als das, was in prod läuft, und der
Satz "bei mir funktioniert es" kehrt zurück.

| Umgebung | Auslöser | Voraussetzung |
|---|---|---|
| dev | Tag-Push (automatisch) | Build und Tests grün |
| staging | automatisch nach erfolgreichem Test in dev | Smoke-Test: Versionsabfrage stimmt, ein Testspiel über die Web-Konsole läuft durch |
| prod | manuell | Test in staging bestanden und Freigabe durch eine zweite Person |

- **Geeignete Prüfung vor der nächsten Umgebung:** ein End-to-End- oder Smoke-Test gegen die
  laufende Umgebung, nicht nur Unit-Tests.
- **Wer darf nach prod?** GitHub Environments mit **Required Reviewers** schützen die
  Umgebung `prod`: der Workflow wartet auf die Freigabe.
- **Änderungserkennung bei mehreren Umgebungen:** Sie dürfte nicht gegen den vorherigen
  Release-Tag vergleichen, sondern gegen die Version, die in **dieser Umgebung** gerade
  läuft (die Versionsabfrage liefert sie). Staging und prod können unterschiedlich weit
  hinter dev zurückliegen, und beim Nachziehen müssen alle Änderungen dazwischen
  mitgenommen werden.
