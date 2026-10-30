package fr.adrien.aerolauncher;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * The Aqua Glass fidget spinner (canvas draft S1), built into the launcher so it can
 * follow the finger: drag to turn it, flick to spin, it slows down by itself, and
 * each arm passing gives a haptic tick that gets lighter as it slows.
 * Drawn in a 440-unit box like the draft.
 */
final class SpinnerView extends View {

    private static final float C = 220, D = 104;
    private static final float[] ANG = {-90, 30, 150};

    private final Path body = new Path();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Vibrator vibrator;
    private final boolean primitives;
    private final int slop;

    private float deg, vel;            // degrees, degrees per second
    private long lastFrame;
    private int segment;
    private long lastTick;

    // touch
    private boolean dragging, moved;
    private float lastAngle, downX, downY;
    private long lastMoveTime;
    private final Runnable longPress = () -> {
        if (!moved) {
            dragging = false;
            performLongClick();
        }
    };

    SpinnerView(Context c) {
        super(c);
        VibratorManager vm = c.getSystemService(VibratorManager.class);
        vibrator = vm != null ? vm.getDefaultVibrator() : null;
        primitives = vibrator != null && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK);
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        setContentDescription(c.getString(R.string.spinner));
        setHapticFeedbackEnabled(true);

        // body: three lobes, the hub and the arms, merged into one outline
        Path p = new Path();
        p.addCircle(C, C, 58, Path.Direction.CW);
        for (float a : ANG) {
            double r = Math.toRadians(a);
            float lx = (float) (C + D * Math.cos(r)), ly = (float) (C + D * Math.sin(r));
            Path lobe = new Path();
            lobe.addCircle(lx, ly, 62, Path.Direction.CW);
            p.op(lobe, Path.Op.UNION);
            Path arm = new Path();
            arm.addRect(new RectF(C, C - 27, C + D, C + 27), Path.Direction.CW);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setRotate(a, C, C);
            arm.transform(m);
            p.op(arm, Path.Op.UNION);
        }
        body.set(p);
        shadow.setColor(0x73001A5A);
        shadow.setMaskFilter(new BlurMaskFilter(10, BlurMaskFilter.Blur.NORMAL));
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 440f;
        c.save();
        c.translate((getWidth() - 440 * s) / 2f, (getHeight() - 440 * s) / 2f);
        c.scale(s, s);

        // body turns with the spinner; its shadow stays below it
        c.save();
        c.translate(0, 7);
        c.rotate(deg, C, C);
        c.drawPath(body, shadow);
        c.restore();
        c.save();
        c.rotate(deg, C, C);
        paint.setShader(null);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFEAF7FF);
        c.drawPath(body, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0x99FFFFFF);
        c.drawPath(body, paint);
        c.restore();

        // bearings and cap are drawn upright at their rotated positions, so the light stays on top
        for (float a : ANG) {
            double r = Math.toRadians(a + deg);
            float bx = (float) (C + D * Math.cos(r)), by = (float) (C + D * Math.sin(r));
            OrbIcons.drawOrb(c, bx, by, 42, OrbIcons.PAL[OrbIcons.GLOBE], false);
            OrbIcons.drawGloss(c, bx, by, 42);
        }
        drawChrome(c, C, C, 34);
        c.restore();

        step();
    }

    private void drawChrome(Canvas c, float cx, float cy, float r) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx - r * 0.2f, cy - r * 0.4f, r * 1.6f,
                new int[]{0xFFFFFFFF, 0xFFE4ECF4, 0xFF9FB1C6, 0xFF6F8299}, new float[]{0f, 0.3f, 0.7f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, paint);
        paint.setShader(new RadialGradient(cx, cy + r * 0.28f, r * 0.9f,
                new int[]{0xFFF4F8FC, 0xFFC4D2E0, 0xFF93A6BC}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r * 0.7f, paint);
        OrbIcons.drawGloss(c, cx, cy, r);
        paint.setShader(null);
    }

    /** Free spin: friction slows it down; keeps redrawing until it stops. */
    private void step() {
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        if (!dragging && Math.abs(vel) > 0.5f) {
            float sg = Math.signum(vel);
            deg += vel * dt;
            vel -= sg * (45 + Math.abs(vel) * 0.5f) * dt;
            if (Math.signum(vel) != sg) vel = 0;
            tick();
            postInvalidateOnAnimation();
        } else {
            lastFrame = 0;
        }
    }

    /** One haptic tick each time an arm passes (every 120°). */
    private void tick() {
        int seg = (int) Math.floor(deg / 120f);
        if (seg == segment) return;
        segment = seg;
        long now = SystemClock.uptimeMillis();
        if (now - lastTick < 28) return;
        lastTick = now;
        float strength = Math.max(0.25f, Math.min(1f, Math.abs(vel) / 1200f));
        if (dragging) strength = 0.6f;
        if (primitives) {
            vibrator.vibrate(VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, strength).compose());
        } else {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        }
    }

    private float angleOf(MotionEvent e) {
        return (float) Math.toDegrees(Math.atan2(e.getY() - getHeight() / 2f, e.getX() - getWidth() / 2f));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                dragging = true;
                moved = false;
                vel = 0;
                downX = e.getX();
                downY = e.getY();
                lastAngle = angleOf(e);
                lastMoveTime = e.getEventTime();
                postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_MOVE: {
                if (!dragging) return true;
                if (!moved && Math.hypot(e.getX() - downX, e.getY() - downY) > slop) {
                    moved = true;
                    removeCallbacks(longPress);
                }
                float a = angleOf(e);
                float d = a - lastAngle;
                if (d > 180) d -= 360;
                if (d < -180) d += 360;
                float dt = Math.max(0.008f, (e.getEventTime() - lastMoveTime) / 1000f);
                vel = d / dt;
                lastAngle = a;
                lastMoveTime = e.getEventTime();
                deg += d;
                tick();
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(longPress);
                if (!dragging) return true;
                dragging = false;
                if (!moved) {
                    vel = 900;   // a tap flicks it
                } else if (e.getEventTime() - lastMoveTime > 80) {
                    vel = 0;     // finger stopped before lifting
                } else {
                    vel = Math.max(-2600, Math.min(2600, vel));
                }
                lastFrame = 0;
                postInvalidateOnAnimation();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }
}
