"""Render the Aero icon pack layers.

Usage: python3 design/render_icons.py <path/to/icons.html> <res-dir>
"""
import os
import sys
from playwright.sync_api import sync_playwright
from PIL import Image

SRC = 'file://' + os.path.abspath(sys.argv[1])
RES = sys.argv[2]
COLORS = ['sky', 'grass', 'leaf', 'globe', 'cloud']
draw = os.path.join(RES, 'drawable-nodpi')
mip = os.path.join(RES, 'mipmap-xxxhdpi')
os.makedirs(draw, exist_ok=True)
os.makedirs(mip, exist_ok=True)

jobs = [(f'{draw}/iconback_{c}.png', f'layer=back&c={c}', 256) for c in COLORS]
jobs += [(f'{draw}/iconmask.png', 'layer=mask', 256),
         (f'{draw}/iconupon.png', 'layer=upon', 256),
         (f'{mip}/ic_launcher.png', 'layer=sample&c=globe', 192)]
samples = []
with sync_playwright() as p:
    b = p.chromium.launch()
    for path, q, px in jobs + [(f'/tmp/sample_{c}.png', f'layer=sample&c={c}', 192) for c in COLORS]:
        pg = b.new_page(viewport={'width': 200, 'height': 200}, device_scale_factor=px / 200)
        pg.goto(f'{SRC}?{q}')
        pg.wait_for_timeout(80)
        pg.locator('#f').screenshot(path=path, omit_background=True)
        pg.close()
    b.close()

# Preview strip for the pack's own screen and the README.
sheet = Image.new('RGBA', (len(COLORS) * 210 + 30, 252), (0, 0, 0, 0))
for i, c in enumerate(COLORS):
    sheet.alpha_composite(Image.open(f'/tmp/sample_{c}.png'), (30 + i * 210, 30))
sheet.save(f'{draw}/pack_preview.png')
