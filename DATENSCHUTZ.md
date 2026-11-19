# Datenschutz bei Minimal

**Kurz:** Minimal erhebt keine personenbezogenen Daten, hat keine Server und gibt nichts an Dritte weiter. Alles bleibt auf deinem Handy – die App hat nicht einmal Internetzugriff.

## Was auf dem Handy gespeichert wird

Nur deine Einstellungen – lokal im App-Speicher:

- Lieblings-Apps und Ordner, Umbenennungen, Symbole, ausgeblendete und als ablenkend markierte Apps
- Gesten, Schrift, Anzeige-Optionen

Nichts davon verlässt das Gerät. Wenn du in Android die Geräte-Sicherung (Google-Backup) eingeschaltet hast, sichert Android diese Einstellungen wie bei jeder anderen App mit – das steuerst du in den Android-Einstellungen.

## Internet

Minimal hat **keine** Internet-Berechtigung und baut keinerlei Verbindung nach außen auf.

Kein Analyse-SDK, keine Werbung, keine Absturzberichte, keine Telemetrie.

## Berechtigungen

| Berechtigung | Wofür | Pflicht? |
|---|---|---|
| Apps auflisten (`<queries>` für Startbildschirm-Apps) | Damit die App-Liste alle Apps zeigen kann | ja |
| Bedienungshilfe „Minimal – Doppeltipp sperrt“ | Nur um per Doppeltipp den Bildschirm zu sperren – liest keine Bildschirminhalte | nein, musst du selbst in den Bedienungshilfen einschalten |
| `PACKAGE_USAGE_STATS` (Nutzungszugriff) | Bildschirmzeit heute und Nutzungszeit im Pause-Bildschirm – wird **nur auf dem Gerät** ausgewertet | nein, musst du selbst in den Android-Einstellungen erlauben |
| `EXPAND_STATUS_BAR` | Wischgeste „Benachrichtigungen herunterziehen“ | ja (ohne Nachfrage, harmlos) |
| `REQUEST_DELETE_PACKAGES` | „Deinstallieren“ im App-Menü – Android fragt trotzdem jedes Mal nach | ja (ohne Nachfrage) |

Minimal hat **keinen** Zugriff auf Kontakte, Standort, Kamera, Mikrofon, Dateien, SMS oder Benachrichtigungsinhalte.

## Nachprüfbar

Jede APK wird per GitHub Actions direkt aus dem Quellcode gebaut.

Fragen: an den Entwickler.
