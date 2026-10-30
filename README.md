# Frutiger Aero widgets

Glossy Frutiger Aero home-screen widgets for Android 12+.

## Aqua Orb (2×2)
A sky-blue glass orb analog clock with rising bubbles and an aqua second hand.
It fills its 2×2 cell edge to edge; the bubbles loop every 3 s (30 frames).
The system draws and animates the hands; tapping it opens the clock app.

Every push to `main` builds the APK and publishes it under **Releases**.

`design/` holds the source the clock images are rendered from
(`python3 design/render.py "$PWD/design/orb.html" <out-dir>`).
