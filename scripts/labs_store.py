#!/usr/bin/env python3
"""Einheitliches Store- und Website-Paket für alle SilentApps-Minimal-Labs-Apps.

Erzeugt je App unter store/: logo.svg, logo-schwarz.svg, logo-weiss.svg, icon-512.png,
feature-1024x500.svg/.png, PLAY-KONSOLE.md und store/website/ (app.json, privacy-de.md, privacy-en.md,
Logos, icon-512.png).
"""
import json, random, shutil, pathlib, sys
import cairosvg
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

HOME = pathlib.Path(__file__).resolve().parent.parent.parent  # Ordner, in dem alle Repo-Klone als labs-<Name> liegen
FONT = HOME / "labs-Minimal/app/src/main/res/font"
SITE = "https://silentapps-minimal-labs.com"
MAIL = "apps@silentapps-minimal-labs.com"
DEV = "SilentApps Minimal Labs"

RING = '<circle cx="54" cy="54" r="18" fill="none" stroke="{c}" stroke-width="4"/>'

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from labs_apps import APPS  # noqa: E402


def motif(app, c):
    sw = app.get("sw", "3")
    inner = app["motif"].replace("{c}", c)
    if not inner:
        return ""
    return f'<g fill="none" stroke="{c}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round">{inner}</g>'


def logo(app, bg, c):
    if bg:
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">'
                f'<rect width="108" height="108" fill="{bg}"/>{RING.format(c=c)}{motif(app, c)}</svg>\n')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="32 32 44 44" width="512" height="512">'
            f'{RING.format(c=c)}{motif(app, c)}</svg>\n')


def text_path(text, ttf, size, x, y, spacing=0.0):
    f = TTFont(str(FONT / ttf))
    gs, cmap, hmtx = f.getGlyphSet(), f.getBestCmap(), f["hmtx"]
    s = size / f["head"].unitsPerEm
    pen = SVGPathPen(gs)
    cx = 0.0
    for ch in text:
        g = cmap[ord(ch)]
        gs[g].draw(TransformPen(pen, (s, 0, 0, -s, x + cx, y)))
        cx += hmtx[g][0] * s + spacing
    return pen.getCommands(), cx - spacing


def width(text, ttf, size, spacing=0.0):
    return text_path(text, ttf, size, 0, 0, spacing)[1]


def stars(w, h, n, seed):
    r = random.Random(seed)
    out = []
    for _ in range(n):
        z = 0.25 + r.random() * r.random() * 0.75
        out.append(f'<circle cx="{r.random()*w:.1f}" cy="{r.random()*h:.1f}" r="{0.6+z*1.6:.2f}" fill="#fff" fill-opacity="{0.15+0.55*z:.2f}"/>')
    return "".join(out)


def feature(app):
    W, H = 1024, 500
    k = 4.2                       # Logo-Maßstab: Ring-Durchmesser 36 → ~151 px
    lx, ly = 250, 250             # Mitte des Rings
    g = (f'<g transform="translate({lx - 54*k},{ly - 54*k}) scale({k})" filter="url(#glow)">'
         f'{RING.format(c="#fff")}{motif(app, "#fff")}</g>')
    name = app["name"]
    size = 64 if len(name) <= 14 else (54 if len(name) <= 18 else 46)
    tx = 390
    d1, _ = text_path(name, "space_grotesk_300.ttf", size, tx, 262)
    d2, _ = text_path(DEV.upper(), "space_grotesk_500.ttf", 15, tx + 2, 302, spacing=3.2)
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">'
            '<defs><filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.6" result="b"/>'
            '<feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>'
            '<filter id="tglow" x="-20%" y="-50%" width="140%" height="200%"><feGaussianBlur stdDeviation="6" result="b"/>'
            '<feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>'
            '<radialGradient id="halo" cx="0.5" cy="0.5" r="0.5"><stop offset="0" stop-color="#fff" stop-opacity="0.08"/>'
            '<stop offset="1" stop-color="#fff" stop-opacity="0"/></radialGradient></defs>'
            f'<rect width="{W}" height="{H}" fill="#000"/>{stars(W, H, 170, hash(app["slug"]) & 0xffff)}'
            f'<circle cx="{lx}" cy="{ly}" r="190" fill="url(#halo)"/>{g}'
            f'<path d="{d1}" fill="#fff" filter="url(#tglow)"/><path d="{d2}" fill="#A8A8B1"/></svg>\n')


def privacy(app, lang):
    p = app["privacy"][lang]
    de = lang == "de"
    rows = "\n".join(f"| {a} | {b} | {c} |" for a, b, c in p["perms"]) or ("| – | keine | – |" if de else "| – | none | – |")
    stored = "\n".join(f"- {x}" for x in p["stored"])
    net = p.get("net") or (f"{app['name']} hat **keine** Internet-Berechtigung und baut keinerlei Verbindung nach außen auf."
                           if de else f"{app['name']} has **no** internet permission and never connects to anything outside your phone.")
    if de:
        return f"""# Datenschutz – {app['name']}

**Kurz:** {app['name']} erhebt keine personenbezogenen Daten, hat keine Server und gibt nichts an Dritte weiter. Kein Konto, keine Werbung, kein Tracking, keine Analyse- oder Absturz-SDKs.

## Was auf deinem Handy gespeichert wird

{stored}

Nichts davon verlässt das Gerät, außer du teilst es selbst über das Android-Teilen-Menü. Ist die Android-Gerätesicherung eingeschaltet, sichert Android die App-Daten wie bei jeder anderen App – das steuerst du in den Android-Einstellungen.

## Internet

{net}

## Berechtigungen

| Berechtigung | Wofür | Pflicht? |
|---|---|---|
{rows}

## Löschen

Deinstallieren oder „Daten löschen“ in den Android-App-Einstellungen entfernt alles vollständig.

## Kontakt

{DEV} · {MAIL} · {SITE}
"""
    return f"""# Privacy – {app['name']}

**In short:** {app['name']} collects no personal data, has no servers and shares nothing with third parties. No account, no ads, no tracking, no analytics or crash SDKs.

## What is stored on your phone

{stored}

None of it leaves the device unless you share it yourself via the Android share menu. If Android device backup is turned on, Android backs up the app data like for any other app – you control that in the Android settings.

## Internet

{net}

## Permissions

| Permission | What for | Required? |
|---|---|---|
{rows}

## Deleting

Uninstalling the app or “Clear data” in the Android app settings removes everything completely.

## Contact

{DEV} · {MAIL} · {SITE}
"""


def konsole(app):
    pr = "Kostenlos" if app["price"] == 0 else f"{app['price']:.2f} €".replace(".", ",")
    extra = "\n".join(f"- {x}" for x in app.get("declarations", [])) or "- keine besonderen Erklärungen nötig"
    return f"""# {app['name']} – Play Console (Kurzfassung)

| Feld | Wert |
|---|---|
| Paketname | `{app['package']}` |
| Entwicklername | {DEV} |
| Preis | {pr} (einmalig, keine In-App-Käufe, keine Werbung) |
| Kategorie | {app['category']} |
| Kontakt-E-Mail | {MAIL} |
| Website | {SITE}/app/{app['slug']} |
| Datenschutz-URL | {SITE}/app/{app['slug']}/privacy |
| Upload | `{app['artifact']}-1.0.<Nr>.aab` aus dem neuesten Actions-Artefakt |

## Grafiken (dieser Ordner)
- App-Symbol: `icon-512.png`
- Titelgrafik: `feature-1024x500.png`
- Screenshots: **echte Bildschirmfotos vom Handy** (mind. 2, besser 4–6, Hochformat)

## Datensicherheit
- Werden Daten erhoben oder geteilt? **Nein** (alles bleibt auf dem Gerät)
- Verschlüsselung bei der Übertragung: entfällt (keine Übertragung)
- Daten löschen: durch Deinstallieren / App-Daten löschen

## Inhaltsbewertung (IARC)
- Kategorie: Hilfsprogramm, Produktivität, Kommunikation oder Sonstiges
- Gewalt, Sexualität, Sprache, Drogen, Glücksspiel: nein · Nutzer interagieren / teilen Inhalte: nein
- Ergebnis erwartet: USK 0 / PEGI 3

## Zielgruppe und Werbung
- Zielgruppe 13+ (nicht für Kinder konzipiert) · Werbung: nein

## Besondere Erklärungen
{extra}

## Texte
Titel, Kurzbeschreibung und Beschreibung DE/EN: siehe die übrigen Textdateien in diesem Ordner.
"""


def vector(app):
    """Adaptives Launcher-Symbol aus denselben Daten wie das Logo (Ring exakt gleich)."""
    import re
    sw = app.get("sw", "3")
    def circ(cx, cy, r):
        return f"M{cx:g},{cy-r:g} A{r:g},{r:g} 0 1,1 {cx-0.01:g},{cy-r:g} Z"
    def rrect(x, y, w, h, rx):
        return (f"M{x+rx:g},{y:g} h{w-2*rx:g} a{rx:g},{rx:g} 0 0 1 {rx:g},{rx:g} v{h-2*rx:g} a{rx:g},{rx:g} 0 0 1 -{rx:g},{rx:g} "
                f"h-{w-2*rx:g} a{rx:g},{rx:g} 0 0 1 -{rx:g},-{rx:g} v-{h-2*rx:g} a{rx:g},{rx:g} 0 0 1 {rx:g},-{rx:g} z")
    out = []
    for t, a in re.findall(r'<(path|circle|rect)([^>]*)/>', app["motif"]):
        A = dict(re.findall(r'([\w-]+)="([^"]*)"', a))
        if t == "path": d = A["d"]
        elif t == "circle": d = circ(float(A["cx"]), float(A["cy"]), float(A["r"]))
        else: d = rrect(*(float(A[k]) for k in ("x", "y", "width", "height", "rx")))
        if A.get("fill") not in (None, "none"):
            out.append(f'    <path android:fillColor="#FFFFFFFF" android:pathData="{d}" />')
        else:
            out.append(f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="{sw}"\n'
                       f'        android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{d}" />')
    body = "\n".join(out)
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {app['name']} – erzeugt von labs_store.py. Ring exakt wie alle Minimal-Apps (Mitte 54/54, r 18, Linie 4); nur das Motiv im Ring ist app-spezifisch. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="4"
        android:pathData="M54,36 A18,18 0 1,1 53.99,36 Z" />
{body}
</vector>
"""


def build(app):
    repo = HOME / app["repo"]
    st = repo / "store"
    web = st / "website"
    web.mkdir(parents=True, exist_ok=True)
    files = {
        "logo.svg": logo(app, "#000", "#fff"),
        "logo-schwarz.svg": logo(app, None, "#000"),
        "logo-weiss.svg": logo(app, None, "#fff"),
        "feature-1024x500.svg": feature(app),
    }
    for n, c in files.items():
        (st / n).write_text(c)
    for old in ["logo-transparent.svg", "logo-hell.svg", "logo-ring.svg", "icon-512.svg"]:
        (st / old).unlink(missing_ok=True)
    cairosvg.svg2png(bytestring=files["logo.svg"].encode(), write_to=str(st / "icon-512.png"), output_width=512, output_height=512)
    cairosvg.svg2png(bytestring=files["feature-1024x500.svg"].encode(), write_to=str(st / "feature-1024x500.png"), output_width=1024, output_height=500)
    (st / "PLAY-KONSOLE.md").write_text(konsole(app))
    (repo / "app/src/main/res/drawable/ic_launcher_foreground.xml").write_text(vector(app))
    meta = {k: app[k] for k in ["slug", "name", "package", "price", "category", "languages"]}
    meta.update({"status": "soon", "play_url": f"https://play.google.com/store/apps/details?id={app['package']}",
                 "developer": DEV, "contact": MAIL,
                 "logo": {"black": "logo-schwarz.svg", "white": "logo-weiss.svg", "icon": "icon-512.png"},
                 "privacy": {"de": "privacy-de.md", "en": "privacy-en.md"},
                 "en": app["web"]["en"], "de": app["web"]["de"]})
    (web / "app.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n")
    (web / "privacy-de.md").write_text(privacy(app, "de"))
    (web / "privacy-en.md").write_text(privacy(app, "en"))
    for n in ["logo-schwarz.svg", "logo-weiss.svg", "icon-512.png"]:
        shutil.copy(st / n, web / n)
    return repo


if __name__ == "__main__":
    only = sys.argv[1:]
    for a in APPS:
        if not only or a["slug"] in only:
            build(a)
            print("ok", a["slug"])
