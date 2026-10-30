"""Render the Aqua Orb layers to PNGs.

Usage: python3 design/render.py <path/to/orb.html> <res-dir>
Writes drawable-nodpi/*.png and mipmap-xxxhdpi/ic_launcher.png under <res-dir>.
"""
import os
import sys
from playwright.sync_api import sync_playwright

SRC = 'file://' + os.path.abspath(sys.argv[1])
RES = sys.argv[2]
FRAME = 380          # CSS size of the orb frame
BUBBLE_FRAMES = 30   # must match the ViewFlipper children in the layout
BUBBLE_PX = 400

draw = os.path.join(RES, 'drawable-nodpi')
mip = os.path.join(RES, 'mipmap-xxxhdpi')
os.makedirs(draw, exist_ok=True)
os.makedirs(mip, exist_ok=True)

clock = '&h=305&m=60&s=180'
jobs = [
    (f'{draw}/dial.png', 'layer=dial', 768),
    (f'{draw}/hand_hour.png', 'layer=hour', 768),
    (f'{draw}/hand_minute.png', 'layer=min', 768),
    (f'{draw}/hand_second.png', 'layer=sec', 768),
    (f'{draw}/overlay.png', 'layer=overlay', 768),
    (f'{draw}/preview.png', 'f=0' + clock, 512),
    (f'{mip}/ic_launcher.png', 'f=0' + clock, 192),
]
jobs += [(f'{draw}/bubbles_{i:02d}.png', f'layer=bubbles&f={i}&n={BUBBLE_FRAMES}', BUBBLE_PX)
         for i in range(BUBBLE_FRAMES)]

with sync_playwright() as p:
    browser = p.chromium.launch()
    for path, query, px in jobs:
        page = browser.new_page(viewport={'width': FRAME, 'height': FRAME},
                                device_scale_factor=px / FRAME)
        page.goto(f'{SRC}?{query}')
        page.wait_for_timeout(100)
        page.locator('#f').screenshot(path=path, omit_background=True)
        page.close()
    browser.close()
