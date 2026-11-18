# Datenschutz bei Minimal

**Kurz:** Minimal erhebt keine personenbezogenen Daten, hat keine Server und gibt nichts an Dritte weiter. Bis auf eine Update-Abfrage bei GitHub bleibt alles auf deinem Handy.

## Was auf dem Handy gespeichert wird

Nur deine Einstellungen – lokal im App-Speicher:

- Lieblings-Apps und Ordner, Umbenennungen, Symbole, ausgeblendete und als ablenkend markierte Apps
- Gesten, Schrift, Anzeige-Optionen
- Zeitpunkt der letzten Update-Prüfung und die zuletzt gesehene Versionsnummer

Nichts davon verlässt das Gerät. Wenn du in Android die Geräte-Sicherung (Google-Backup) eingeschaltet hast, sichert Android diese Einstellungen wie bei jeder anderen App mit – das steuerst du in den Android-Einstellungen.

## Internet

Minimal hat genau **eine** Verbindung nach außen:

| Wohin | Wann | Was wird übertragen |
|---|---|---|
| `api.github.com` (Repository `phlpjo/Minimalist-Phone-APK`, Release „latest“) | beim Öffnen, höchstens alle 6 Stunden, oder wenn du in den Einstellungen „prüfen“ antippst | eine normale HTTPS-Anfrage: deine IP-Adresse und die Kennung `Minimal-Launcher` – keine Geräte-ID, keine Einstellungen, keine Nutzungsdaten |

Für diese Anfrage gelten die [Datenschutzhinweise von GitHub](https://docs.github.com/de/site-policy/privacy-policies/github-general-privacy-statement). Wenn du die APK über den Link herunterlädst, öffnet sich dafür dein Browser.

Kein Analyse-SDK, keine Werbung, keine Absturzberichte, keine Telemetrie.

## Berechtigungen

| Berechtigung | Wofür | Pflicht? |
|---|---|---|
| Apps auflisten (`<queries>` für Startbildschirm-Apps) | Damit die App-Liste alle Apps zeigen kann | ja |
| `INTERNET` | Nur für die Update-Abfrage oben | ja |
| `PACKAGE_USAGE_STATS` (Nutzungszugriff) | Bildschirmzeit heute und Nutzungszeit im Pause-Bildschirm – wird **nur auf dem Gerät** ausgewertet | nein, musst du selbst in den Android-Einstellungen erlauben |
| `EXPAND_STATUS_BAR` | Wischgeste „Benachrichtigungen herunterziehen“ | ja (ohne Nachfrage, harmlos) |
| `REQUEST_DELETE_PACKAGES` | „Deinstallieren“ im App-Menü – Android fragt trotzdem jedes Mal nach | ja (ohne Nachfrage) |

Minimal hat **keinen** Zugriff auf Kontakte, Standort, Kamera, Mikrofon, Dateien, SMS oder Benachrichtigungsinhalte.

## Nachprüfbar

Der gesamte Quellcode liegt in diesem Repository, und jede veröffentlichte APK wird von GitHub Actions direkt aus diesem Code gebaut – der Build-Verlauf ist unter *Actions* öffentlich einsehbar.

Fragen: über die *Issues* dieses Repositories.
