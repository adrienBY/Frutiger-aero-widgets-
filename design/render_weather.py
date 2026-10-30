"""Render the Aero Weather scene backgrounds and gloss layers.

Usage: python3 design/render_weather.py <path/to/weather.html> <res-dir>
"""
import os
import sys
from playwright.sync_api import sync_playwright
from PIL import Image

SRC = 'file://' + os.path.abspath(sys.argv[1])
draw = os.path.join(sys.argv[2], 'drawable-nodpi')
os.makedirs(draw, exist_ok=True)
CONDS = ['clear', 'partly', 'cloudy', 'fog', 'drizzle', 'rain', 'snow', 'storm']
PX = 600

jobs = [(f'{draw}/wx_{c}_{"n" if n else "d"}.png', f'cond={c}&night={n}&layer=bg', PX)
        for c in CONDS for n in (0, 1)]
jobs += [(f'{draw}/wx_gloss_bright.png', 'cond=clear&night=0&layer=gloss', PX),
         (f'{draw}/wx_gloss_dim.png', 'cond=clear&night=1&layer=gloss', PX),
         (f'{draw}/wx_preview.png', 'cond=clear&night=0&layer=all', 512)]
# Bubble frames for the widget's ViewFlipper (10 fps, 3 s loop), like the clock's.
BUBBLE_FRAMES = 30
jobs += [(f'{draw}/wx_bub_{i:02d}.png', f'layer=bubbles&f={i}&n={BUBBLE_FRAMES}', 400)
         for i in range(BUBBLE_FRAMES)]

with sync_playwright() as p:
    b = p.chromium.launch()
    for path, q, px in jobs:
        pg = b.new_page(viewport={'width': 380, 'height': 380}, device_scale_factor=px / 380)
        pg.goto(f'{SRC}?{q}')
        pg.wait_for_timeout(150)
        pg.locator('#f').screenshot(path=path, omit_background=True)
        pg.close()
    b.close()

# Lossy WebP with alpha keeps the APK small (~0.5 MB for every scene).
for path, _, _ in jobs:
    Image.open(path).save(path[:-4] + '.webp', 'WEBP', quality=92, method=6)
    os.remove(path)
