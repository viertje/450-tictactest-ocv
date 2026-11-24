# Web-Konsole für TicTacTest

Stand: 18.11.2026. Teil von Auftrag 1. Weitere Abschnitte (Versionsabfrage,
Frontend, Release) folgen.

## Werkzeug: ttyd

Gewählt wurde **ttyd**. Gründe:

- Ein einzelnes, statisches Programm. Es lässt sich auf einem Server einfach und
  reproduzierbar installieren (feste Version, Prüfsumme).
- Es ist ein echtes Terminal im Browser (xterm.js ist eingebaut). Die ANSI-Farben
  des Spielfelds funktionieren ohne zusätzlichen Code.
- Es wird aktiv gepflegt.

| Werkzeug | Beurteilung |
|---|---|
| ttyd | gewählt: volles Terminal, ein Binary |
| GoTTY | ähnlich, aber weniger gepflegt |
| websocketd | überträgt nur Textzeilen, kein echtes Terminal; eigene Webseite und Behandlung der ANSI-Codes nötig |

## Lokal starten

Voraussetzung: Docker und das Jar aus dem GitHub-Release (nicht lokal gebaut).
Das Jar liegt im Ordner `webconsole-test`, der in den Container eingebunden wird.

```powershell
docker run --rm -it -p 7681:7681 -v "<Pfad>\webconsole-test:/work" -w /work ghcr.io/viertje/450-tictactest-ocv:v1.0.2 bash
```

Im Container ttyd mit fester Version und Prüfsumme installieren und starten:

```bash
V=1.7.7
curl -fsSLO https://github.com/tsl0922/ttyd/releases/download/$V/ttyd.x86_64
curl -fsSLO https://github.com/tsl0922/ttyd/releases/download/$V/SHA256SUMS
grep ' ttyd.x86_64$' SHA256SUMS | sha256sum -c -     # muss "OK" ausgeben
chmod +x ttyd.x86_64
./ttyd.x86_64 -W -p 7681 java -jar tictactest-1.0.1.jar
```

Danach ist das Spiel unter `http://localhost:7681` im Browser spielbar.

| Option | Bedeutung |
|---|---|
| `-W` | Eingaben im Browser erlauben. Ohne diese Option ist das Terminal schreibgeschützt |
| `-p 7681` | Port der Web-Konsole |
| `-m <n>` | Höchstzahl gleichzeitiger Verbindungen (siehe unten) |

## Beobachtungen

| Frage | Beobachtung |
|---|---|
| Wie viele Java-Prozesse bei zwei Tabs? | Zwei. ttyd startet pro Verbindung einen eigenen `java`-Prozess |
| Wie viel Speicher braucht das? | Etwa 40 MB pro Java-Prozess, ttyd selbst etwa 17 MB |
| Was passiert bei Spielende? | Das Programm beendet sich nach Sieg oder Unentschieden. Der Prozess endet und die Verbindung wird geschlossen |
| Wie startet man ein neues Spiel? | Seite neu laden: ttyd startet einen neuen Prozess mit einem leeren Spielfeld |
| Ungültige Eingabe wie `abc`? | Das Spiel meldet "not a valid number" und fragt erneut |
| Zahl ausserhalb von 0 bis 8 oder besetztes Feld? | Das Spiel bricht mit einer Fehlermeldung ab (`IllegalStateException`). Neu laden startet ein neues Spiel |

## Folgen für einen Server mit 1 GB RAM

- Nach Abzug von Betriebssystem und Diensten bleiben etwa 600 MB frei. Bei
  40 MB pro Spiel sind das rund 10 bis 15 gleichzeitige Spiele.
- Ohne Begrenzung genügt eine Person mit vielen offenen Tabs, um den
  Arbeitsspeicher zu füllen. Dann fällt das Spiel für alle aus.
- Massnahmen: Verbindungslimit in ttyd (`-m 10`), automatischer Neustart des
  Dienstes bei einem Absturz und bei Bedarf eine grössere Instanz.

## Sicherheit: nur das Jar starten, niemals eine Shell

Die Web-Konsole ist für alle erreichbar, die die URL kennen, und verlangt keine
Anmeldung. Wenn ttyd statt `java -jar` eine Shell (`bash`, `sh`, `login`)
starten würde, bekäme jede Person im Internet eine Shell auf dem Server. Sie
könnte:

- Dateien lesen und verändern und eigene Programme starten
- den Server für Angriffe auf andere missbrauchen
- auf einer EC2-Instanz die Zugangsdaten der Instanz abrufen und damit auf das
  AWS-Konto zugreifen

Darum gilt:

- Der Befehl ist fest eingestellt: `java -jar <Jar>`. Die Benutzerin oder der
  Benutzer kann keine Parameter mitgeben.
- Der Dienst läuft unter einem eigenen Benutzer, nicht als root.
- Die Anzahl Verbindungen ist begrenzt und nur die nötigen Ports sind offen.

## Frontend (Ordner `frontend/`)

Eine statische Webseite in reinem HTML, CSS und JavaScript, ohne Framework und
ohne Build-Schritt. Sie zeigt Titel und Spielregeln, bettet das Terminal der
Web-Konsole per `<iframe>` ein und zeigt die Versionen von Backend und Frontend.

| Datei | Aufgabe |
|---|---|
| `index.html` | Aufbau der Seite; lädt zuerst `config.js`, dann `app.js` |
| `config.js` | Einstellungen pro Umgebung: URL der Web-Konsole (`consoleUrl`) und der Versionsabfrage (`versionUrl`) |
| `app.js` | Setzt die iframe-URL aus `config.js` und lädt die beiden Versionen |
| `version.txt` | Frontend-Version. Im Repository `0.0.0-SNAPSHOT`; der Release schreibt die Tag-Version ins Archiv |
| `style.css` | Gestaltung |

**Die URL der Web-Konsole steht nicht im Code.** Nur `config.js` kennt sie.
Lokal zeigt die Datei auf `localhost`. In der Cloud schreibt die Infrastruktur
(OpenTofu) eine eigene `config.js` in den Bucket, und das Frontend-Deployment
darf sie nicht überschreiben.

### Lokal testen (drei getrennte Server)

1. Web-Konsole (im Container, siehe oben): `ttyd` auf Port 7681.
2. Versionsabfrage (auf dem Computer): `python scripts/dev-version-server.py 1.0.1`
   antwortet auf `http://localhost:8081/version`. Das Skript ersetzt lokal den
   nginx des Servers.
3. Frontend (auf dem Computer, im Ordner `frontend`): `python -m http.server 8000`

Danach `http://localhost:8000` öffnen. Das Frontend und die Versionsabfrage
liegen auf verschiedenen Ports und damit auf verschiedenen Origins. Der Browser
liest die Antwort der Versionsabfrage nur, wenn sie den Header
`Access-Control-Allow-Origin` enthält (CORS). Fehlt er, meldet die
Entwickler-Konsole des Browsers (F12) einen CORS-Fehler und das Frontend zeigt
"nicht verfügbar".

## Versionsabfrage

Das Jar bietet keine Versionsabfrage an und wird nicht verändert. Die Version
kommt deshalb von aussen und stammt immer aus dem Release-Tag, nie aus einer
von Hand gepflegten Datei.

| Teil | Woher kommt die Version? | Wie wird sie abgefragt? |
|---|---|---|
| Backend (Jar) | Beim Deployment wird die Versionsnummer des Release-Tags als Textdatei neben das Jar geschrieben | Ein Webserver (nginx vor der Web-Konsole) liefert sie unter `/version` aus |
| Frontend | Der Release schreibt die Tag-Version in `version.txt` im Frontend-Archiv | Das Frontend liest `version.txt` neben der Seite |

Gegen andere Quellen sprach: Der Dateiname des Jars enthält zwar die Version,
ist aber eine zweite Kopie derselben Information und müsste auf dem Server
ausgelesen werden. Das Manifest im Jar enthält die Version nicht, weil
`build.gradle` sie bewusst nicht kennt. Der Release-Tag ist die einzige Quelle.

Die Antwort von `/version` braucht den Header `Access-Control-Allow-Origin`,
weil das Frontend auf einem anderen Origin liegt als die Versionsabfrage.
Lokal ersetzt `scripts/dev-version-server.py` den nginx.

## Release: Jar und Frontend-Archiv

Ein Tag `vX.Y.Z` erzeugt weiterhin einen GitHub-Release. Er enthält jetzt zwei
Dateien:

- `tictactest-X.Y.Z.jar`: das Backend
- `tictactest-frontend-X.Y.Z.zip`: das Frontend mit der Tag-Version in `version.txt`

Das Archiv enthält auch `config.js` mit den lokalen Standardwerten, damit sich
alles allein aus den Release-Dateien lokal starten lässt. In der Cloud darf das
Deployment diese Datei nicht überschreiben (siehe Auftrag 2).

**Gehören Start-Skript und systemd-Unit zum Release?** Nein, sie gehören zur
Infrastruktur. Sie beschreiben, wie und wo die Anwendung auf einem bestimmten
Server läuft (Benutzer, Neustart, Ports), nicht was die Anwendung ist. Sie
ändern sich unabhängig von einer neuen Spielversion und werden von OpenTofu
eingerichtet. Ein Release enthält nur die Artefakte.

**Warum lohnt sich das Frontend als Release-Asset, obwohl es nur aus ein paar
Dateien besteht?**

- Es gibt eine feste, nummerierte Version, die sich genau so wieder ausliefern
  lässt (Rollback).
- Deployt wird dasselbe Archiv, das zum Release gehört, nicht ein neu
  zusammengestellter Stand. So weiss man, was läuft.
- Backend und Frontend haben dieselbe Versionsnummer und denselben Ursprung im
  Tag. Das macht Fehlersuche und Nachvollziehbarkeit einfach.
- Die Pipeline in Auftrag 3 kann alles aus einer Quelle holen, ohne Neubau.
