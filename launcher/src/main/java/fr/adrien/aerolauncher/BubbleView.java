package fr.adrien.aerolauncher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * Bubbles drifting up the whole home screen. Updated 10 times a second on purpose,
 * for the same slightly stepped motion as the Aqua Orb clock.
 */
final class BubbleView extends View {

    private static final int COUNT = 16;
    private static final long FRAME_MS = 100;

    private final float[] x = new float[COUNT], size = new float[COUNT], period = new float[COUNT],
            phase = new float[COUNT], wobble = new float[COUNT];
    private final Bitmap sprite;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Rect dst = new Rect();
    private boolean running;

    BubbleView(Context c) {
        super(c);
        Random r = new Random(7);
        for (int i = 0; i < COUNT; i++) {
            x[i] = r.nextFloat();
            size[i] = Aero.dp(c, 8 + r.nextFloat() * 26);
            period[i] = 9f + r.nextFloat() * 10f;   // seconds to cross the screen
            phase[i] = r.nextFloat();
            wobble[i] = Aero.dp(c, 3 + r.nextFloat() * 6);
        }
        sprite = makeSprite(Aero.dp(c, 64));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setRunning(boolean on) {
        running = on;
        if (on) invalidate();
    }

    private static Bitmap makeSprite(int s) {
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        float r = s / 2f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(r, r, r, new int[]{0x00FFFFFF, 0x00FFFFFF, 0x66C8FFFA},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(r, r, r - 1, p);
        p.setShader(new RadialGradient(r * 0.64f, r * 0.56f, r * 0.8f,
                new int[]{0xF2FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawCircle(r, r, r - 1, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.03f);
        p.setColor(0xB8FFFFFF);
        c.drawCircle(r, r, r - s * 0.03f, p);
        return b;
    }

    @Override
    protected void onDraw(Canvas c) {
        float t = (SystemClock.uptimeMillis() / FRAME_MS) * (FRAME_MS / 1000f); // stepped clock
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < COUNT; i++) {
            float p = ((t / period[i] + phase[i]) % 1f + 1f) % 1f;
            float y = h * 1.05f - p * h * 1.15f;
            float xx = x[i] * w + wobble[i] * (float) Math.sin(p * Math.PI * 4);
            float fade = Math.min(1f, p / 0.12f) * Math.min(1f, (1f - p) / 0.25f);
            int s = Math.round(size[i]);
            dst.set(Math.round(xx - s / 2f), Math.round(y - s / 2f), Math.round(xx + s / 2f), Math.round(y + s / 2f));
            paint.setAlpha(Math.round(200 * fade));
            c.drawBitmap(sprite, null, dst, paint);
        }
        if (running) postInvalidateDelayed(FRAME_MS);
    }
}
