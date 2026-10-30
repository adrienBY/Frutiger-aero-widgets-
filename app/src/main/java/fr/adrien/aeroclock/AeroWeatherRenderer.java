package fr.adrien.aeroclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

/**
 * Draws the top layer of the Aero Blue weather tile: the text in Selawik, then the
 * glass highlight. Coordinates are in the 380-unit frame used by
 * design/weather.html, so the phone output matches the design.
 */
final class AeroWeatherRenderer {

    private static final float FRAME = 380f;
    private static final int NAVY = 0xFF0A3C8C;

    private static Typeface light, regular, bold;

    private AeroWeatherRenderer() {}

    private static void loadFonts(Context c) {
        if (light != null) return;
        light = Typeface.createFromAsset(c.getAssets(), "fonts/Selawik-Light.ttf");
        regular = Typeface.createFromAsset(c.getAssets(), "fonts/Selawik-Regular.ttf");
        bold = Typeface.createFromAsset(c.getAssets(), "fonts/Selawik-Bold.ttf");
    }

    /** A text shadow in CSS terms: offset, blur and ARGB color. */
    private static final class Shadow {
        final float dx, dy, blur;
        final int color;
        Shadow(float dx, float dy, float blur, int color) {
            this.dx = dx; this.dy = dy; this.blur = blur; this.color = color;
        }
    }

    static Bitmap render(Context c, String scene, boolean night, String temp,
                         String label, String place, int px) {
        loadFonts(c);
        float s = px / FRAME;
        Bitmap out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        Paint img = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Rect dst = new Rect(0, 0, px, px);

        // The scene itself (wx_<scene>_<d|n>) and the animated bubbles are separate
        // widget layers under this bitmap, which holds only the text and the gloss.
        boolean darkText = night || scene.equals("storm");
        boolean dimGloss = night || scene.equals("rain") || scene.equals("drizzle") || scene.equals("storm");

        // Temperature: Selawik Light 118, tracking -3px, top-left of the glass.
        if (darkText) {
            text(cv, temp, light, 118, 42, 50, 1f, -3f, 0xFFFFFFFF, 170, s, false,
                    new Shadow(0, 0, 16, 0x99_96C8FF), new Shadow(0, 2, 6, 0x99_000A3C));
        } else {
            text(cv, temp, light, 118, 42, 50, 1f, -3f, NAVY, 170, s, false,
                    new Shadow(0, 0, 12, 0xD9_FFFFFF), new Shadow(0, 1, 0, 0xE6_FFFFFF));
        }

        // Condition: Selawik Regular 24, under the temperature.
        if (darkText) {
            text(cv, label, regular, 24, 46, 178, 1.2f, 0f, 0xFFD6E8FF, 290, s, false,
                    new Shadow(0, 1, 4, 0x99_000A3C));
        } else {
            text(cv, label, regular, 24, 46, 178, 1.2f, 0f, NAVY, 290, s, false,
                    new Shadow(0, 0, 8, 0xD9_FFFFFF));
        }

        // Place: Selawik Bold 26 on the hill, anchored to the bottom edge (y = 340).
        if (!night && scene.equals("snow")) {
            text(cv, place, bold, 26, 46, 340, 1.2f, 0f, NAVY, 210, s, true,
                    new Shadow(0, 0, 8, 0xE6_FFFFFF));
        } else {
            text(cv, place, bold, 26, 46, 340, 1.2f, 0f, 0xFFFFFFFF, 210, s, true,
                    new Shadow(0, 2, 6, 0xBF_0A3C0F));
        }

        drawRes(c, cv, dimGloss ? "wx_gloss_dim" : "wx_gloss_bright", dst, img);
        return out;
    }

    private static void drawRes(Context c, Canvas cv, String name, Rect dst, Paint p) {
        int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        if (id == 0) return;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;
        Bitmap b = BitmapFactory.decodeResource(c.getResources(), id, o);
        if (b == null) return;
        cv.drawBitmap(b, null, dst, p);
        b.recycle();
    }

    /**
     * Draws one line like a CSS block with the given line-height.
     * y is the box top, or the box bottom when fromBottom is true.
     * Text wider than maxW shrinks (down to 70%), then ellipsizes.
     */
    private static void text(Canvas cv, String str, Typeface tf, float size, float x, float y,
                             float lineHeight, float trackingPx, int color, float maxW, float s,
                             boolean fromBottom, Shadow... shadows) {
        if (str == null || str.isEmpty()) return;
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(tf);
        p.setTextSize(size * s);
        p.setLetterSpacing(trackingPx / size);
        float limit = maxW * s;
        float w = p.measureText(str);
        if (w > limit) {
            float k = Math.max(0.7f, limit / w);
            size *= k;
            p.setTextSize(size * s);
            w = p.measureText(str);
            if (w > limit) {
                str = TextUtils.ellipsize(str, p, limit, TextUtils.TruncateAt.END).toString();
            }
        }
        float box = size * lineHeight * s;
        float top = fromBottom ? y * s - box : y * s;
        Paint.FontMetrics fm = p.getFontMetrics();
        float baseline = top + (box - (fm.descent - fm.ascent)) / 2f - fm.ascent;
        float left = x * s;

        for (Shadow sh : shadows) {
            // CSS blur is about twice the Gaussian sigma; Android's radius is close to the CSS value.
            float r = Math.max(0.5f, sh.blur * 0.85f * s);
            p.setShadowLayer(r, sh.dx * s, sh.dy * s, sh.color);
            p.setColor(color);
            cv.drawText(str, left, baseline, p);
        }
        p.clearShadowLayer();
        p.setColor(color);
        cv.drawText(str, left, baseline, p);
    }
}
