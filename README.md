# Minimal

Schlichter Android-Launcher in Schwarz-Weiß, angelehnt an „Minimalist Phone“.

- **Startbildschirm:** Uhrzeit, Datum, Akku, Bildschirmzeit heute, bis zu 8 Lieblings-Apps als Text
- **Alle Apps:** nach oben wischen → alphabetische Liste mit Suche (Enter öffnet den ersten Treffer)
- **Ablenkende Apps:** vor dem Öffnen eine Pause (5–30 Sek.) mit Atemkreis und heutiger Nutzungszeit
- **Lange drücken** auf eine App: Startbildschirm, ablenkend, App-Info, Deinstallieren
- **Lange drücken** auf den Startbildschirm: Einstellungen

## Installieren

1. Auf dem Handy **Releases → neueste Version → `Minimal.apk`** öffnen (bei GitHub angemeldet, das Repo ist privat).
2. Antippen, „Unbekannte Apps installieren“ für den Browser erlauben, installieren.
3. Home-Taste → **Minimal** als Standard wählen (oder in Minimal: Einstellungen → „Als Standard-Startbildschirm festlegen“).
4. Für die Bildschirmzeit einmal **Nutzungszugriff** erlauben (Einstellungen → Anzeige).

Neue Versionen installieren sich über die alte (gleicher Schlüssel).

## Bauen

Jeder Push auf `main` baut per GitHub Actions eine signierte APK und veröffentlicht sie als Release.
Der Signierschlüssel liegt als Secret `KEYSTORE_B64` (Base64 der PKCS12-Datei). Ohne Secret entsteht nur ein unsignierter Testbuild.
Schlägt ein Build fehl, landet das Log im Branch `build-log`.

Kotlin, Jetpack Compose (ohne Material), minSdk 26, targetSdk 35.
