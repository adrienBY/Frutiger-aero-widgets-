package fr.adrien.aerolauncher;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Draws an app icon inside a glossy Frutiger Aero glass orb (the same look as the
 * Aero Icons pack, design/icons.html), tinted from the wallpaper palette that best
 * matches the icon's own colours. All coordinates are in a 200-unit box.
 */
final class OrbIcons {

    /** sky, grass, leaf, globe, cloud: taken from the wallpaper. */
    static final int[][] PAL = {
            {0xFFA9DCFF, 0xFF3D86F7, 0xFF0D34CF, 0xFF061C86},
            {0xFFE6FFB8, 0xFF79D94A, 0xFF2F9A22, 0xFF145E17},
            {0xFFF8FFDC, 0xFFD4F07E, 0xFF93C93E, 0xFF4C8A1C},
            {0xFFECFCFF, 0xFF86DCFF, 0xFF1F96E6, 0xFF0A4AA6},
            {0xFFFFFFFF, 0xFFEEF5FC, 0xFFBFD2EA, 0xFF7F9CC6},
    };
    static final int SKY = 0, GRASS = 1, LEAF = 2, GLOBE = 3, CLOUD = 4;

    private OrbIcons() {}

    static Bitmap render(Drawable icon, int size, String seed) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        float u = size / 200f;
        int pal = paletteFor(icon, seed);
        drawOrb(c, 100 * u, 100 * u, 92 * u, PAL[pal], true);

        // the app's own icon, shrunk into the round window of the orb
        c.save();
        Path clip = new Path();
        clip.addCircle(100 * u, 100 * u, 85 * u, Path.Direction.CW);
        c.clipPath(clip);
        int inset = Math.round(size * 0.13f);
        icon.setBounds(inset, inset, size - inset, size - inset);
        icon.draw(c);
        c.restore();

        drawGloss(c, 100 * u, 100 * u, 92 * u);
        drawDew(c, 145.5f * u, 115.5f * u, 7.5f * u);
        return out;
    }

    /** Glass orb body: soft shadow, gradient, bottom glow, inner shading, bezel. */
    static void drawOrb(Canvas c, float cx, float cy, float r, int[] col, boolean shadow) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (shadow) {
            p.setColor(0x59001A5A);
            p.setMaskFilter(new BlurMaskFilter(r * 0.07f, BlurMaskFilter.Blur.NORMAL));
            c.drawCircle(cx, cy + r * 0.035f, r, p);
            p.setMaskFilter(null);
        }
        // CSS: radial-gradient(circle at 50% 28%, a 0%, b 34%, c 72%, d 100%)
        p.setShader(new RadialGradient(cx, cy - r * 0.44f, r * 1.75f,
                new int[]{col[0], col[1], col[2], col[3]}, new float[]{0f, 0.34f, 0.72f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);

        // darker bottom inside the glass
        p.setShader(new LinearGradient(0, cy, 0, cy + r, 0x00001E5A, 0x4D001E5A, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);

        // aqua caustic glow at the bottom
        c.save();
        float gy = cy + r * 0.96f;
        c.scale(1f, 0.43f, cx, gy);
        p.setShader(new RadialGradient(cx, gy, r * 0.59f, 0xB3DCFFFA, 0x00DCFFFA, Shader.TileMode.CLAMP));
        c.drawCircle(cx, gy, r * 0.59f, p);
        c.restore();

        // white bezel
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.027f);
        p.setColor(0x8CFFFFFF);
        c.drawCircle(cx, cy, r + r * 0.013f, p);
    }

    /** The curved highlight across the top of the orb, plus a thin inner rim. */
    static void drawGloss(Canvas c, float cx, float cy, float r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        RectF g = new RectF(cx - r * 0.76f, cy - r * 0.945f, cx + r * 0.76f, cy - r * 0.03f);
        p.setShader(new LinearGradient(0, g.top, 0, g.bottom, 0xD1FFFFFF, 0x0FFFFFFF, Shader.TileMode.CLAMP));
        c.drawOval(g, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.016f);
        p.setColor(0x73FFFFFF);
        c.drawCircle(cx, cy, r - r * 0.008f, p);
    }

    /** A small water drop, like the dew on the wallpaper's leaves. */
    static void drawDew(Canvas c, float cx, float cy, float r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(cx - r * 0.32f, cy - r * 0.4f, r * 1.2f,
                new int[]{0xF2FFFFFF, 0x40FFFFFF, 0x00FFFFFF}, new float[]{0f, 0.32f, 0.6f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(r * 0.18f);
        p.setColor(0x66FFFFFF);
        c.drawCircle(cx, cy, r * 0.9f, p);
    }

    /** Picks the wallpaper palette closest to the icon's dominant hue. */
    static int paletteFor(Drawable icon, String seed) {
        int n = 24;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        icon.setBounds(0, 0, n, n);
        icon.draw(c);
        double hx = 0, hy = 0, wsum = 0, vsum = 0, count = 0;
        float[] hsv = new float[3];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int px = b.getPixel(x, y);
                int a = Color.alpha(px);
                if (a < 128) continue;
                Color.colorToHSV(px, hsv);
                double w = hsv[1] * hsv[2];
                hx += Math.cos(Math.toRadians(hsv[0])) * w;
                hy += Math.sin(Math.toRadians(hsv[0])) * w;
                wsum += w;
                vsum += hsv[2];
                count++;
            }
        }
        b.recycle();
        if (count == 0 || wsum / count < 0.12) {
            // mostly grey or white icons: alternate cloud and sky so the grid stays varied
            return (seed.hashCode() & 1) == 0 ? CLOUD : SKY;
        }
        double hue = (Math.toDegrees(Math.atan2(hy, hx)) + 360) % 360;
        double val = vsum / count;
        if (hue >= 40 && hue < 75) return LEAF;
        if (hue >= 75 && hue < 170) return val > 0.8 ? LEAF : GRASS;
        if (hue >= 170 && hue < 205) return GLOBE;
        if (hue >= 205 && hue < 265) return SKY;
        // reds, oranges, pinks, purples: no warm orb in the set, so pick a blue one
        return (seed.hashCode() & 1) == 0 ? GLOBE : SKY;
    }
}
