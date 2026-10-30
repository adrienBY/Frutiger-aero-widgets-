import sys
from playwright.sync_api import sync_playwright
src='file://'+sys.argv[1]; out=sys.argv[2]
jobs=[('dial','layer=dial',768),('hand_hour','layer=hour',768),('hand_minute','layer=min',768),
      ('hand_second','layer=sec',768),('overlay','layer=overlay',768),
      ('preview','h=305&m=60&s=180',512),('icon','h=305&m=60&s=180',192)]
with sync_playwright() as p:
    b=p.chromium.launch()
    for name,q,px in jobs:
        pg=b.new_page(viewport={'width':440,'height':440},device_scale_factor=px/440)
        pg.goto(src+'?'+q); pg.wait_for_timeout(150)
        pg.locator('#f').screenshot(path=f'{out}/{name}.png',omit_background=True)
        pg.close()
    b.close()
