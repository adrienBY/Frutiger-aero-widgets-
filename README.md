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

## Aero Météo (2×2)
Current weather in the Aero Blue glass tile: live temperature and conditions from
[Open-Meteo](https://open-meteo.com), your neighborhood name, and a scene for each
weather type (clear, partly cloudy, cloudy, fog, drizzle, rain, snow, storm) in day
and night versions. Refreshes every 30 minutes; tap it to refresh with your current
position. Open **Aero Météo** once to allow location. Scenes come from
`design/weather.html` (`python3 design/render_weather.py design/weather.html app/src/main/res`);
the text is drawn on the phone in Selawik (OFL, `app/src/main/assets/fonts`).

## Aero Launcher
A Frutiger Aero home screen (set it in Settings → Apps → Default apps → Home):
- every app as a glass orb, tinted from the wallpaper palette that matches its icon
- a glass shelf dock with a Start orb in the middle; swipe up or tap it for the
  frosted Start menu with search
- your widgets (long-press empty space → *Ajouter un widget*), plus a built-in
  draggable fidget spinner with haptic ticks
- bubbles drifting up behind everything at 10 fps, Selawik labels
- long-press anything to move or remove it; swipe down for notifications
