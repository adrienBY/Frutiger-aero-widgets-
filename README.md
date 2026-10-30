# Frutiger Aero widgets

Glossy Frutiger Aero home-screen widgets for Android 12+.

## Aqua Orb (2×2)
A sky-blue glass orb analog clock with rising bubbles and an aqua second hand.
It fills its 2×2 cell edge to edge; the bubbles loop every 3 s (30 frames).
The system draws and animates the hands; tapping it opens the clock app.

Every push to `main` builds the APK and publishes it under **Releases**.

`design/` holds the source the clock images are rendered from
(`python3 design/render.py "$PWD/design/orb.html" <out-dir>`).

## Aero Icons (icon pack)
Wraps every app's own icon in a glossy orb (sky, grass, leaf, globe or cloud,
colors taken from the wallpaper) with a gloss highlight and a dew drop on top.
It uses the launcher's fallback layers (`iconback` / `iconmask` / `iconupon`),
so apps installed later are themed too. Works in Nova Launcher and other
icon-pack launchers. Layers are rendered from `design/icons.html`
(`python3 design/render_icons.py design/icons.html iconpack/src/main/res`).
