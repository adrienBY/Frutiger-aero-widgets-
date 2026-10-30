package fr.adrien.aerolauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** The glass shelf at the bottom: two apps, the Start orb, two apps. */
final class DockView extends FrameLayout {

    final LinearLayout row;
    final StartOrb start;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF shelf = new RectF();

    DockView(Context c) {
        super(c);
        setWillNotDraw(false);
        setClipChildren(false);
        row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        start = new StartOrb(c);
    }

    /** Rebuilds the row: slots 0-1, the Start orb, slots 2-3. Empty slots stay as gaps. */
    void setSlots(View[] slots) {
        row.removeAllViews();
        for (int i = 0; i < 5; i++) {
            View v;
            if (i == 2) {
                v = start;
            } else {
                View s = slots[i < 2 ? i : i - 1];
                v = s != null ? s : new View(getContext());
            }
            if (v.getParent() != null) ((android.view.ViewGroup) v.getParent()).removeView(v);
            row.addView(v, new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, i == 2 ? 1.15f : 1f));
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float d = getResources().getDisplayMetrics().density;
        shelf.set(10 * d, getHeight() * 0.22f, getWidth() - 10 * d, getHeight() - 6 * d);
        float rad = 26 * d;
        Path clip = new Path();
        clip.addRoundRect(shelf, rad, rad, Path.Direction.CW);

        // glass body
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, shelf.top, 0, shelf.bottom,
                new int[]{0x70FFFFFF, 0x2EFFFFFF, 0x52A8E4FF}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRoundRect(shelf, rad, rad, paint);

        // top-half gloss
        c.save();
        c.clipPath(clip);
        RectF g = new RectF(shelf.left, shelf.top, shelf.right, shelf.top + shelf.height() * 0.5f);
        paint.setShader(new LinearGradient(0, g.top, 0, g.bottom, 0x8CFFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(g, paint);
        // aqua glow along the bottom edge
        paint.setShader(new RadialGradient(shelf.centerX(), shelf.bottom, shelf.width() * 0.45f,
                0x6680F0FF, 0x0080F0FF, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, 0.25f, shelf.centerX(), shelf.bottom);
        c.drawCircle(shelf.centerX(), shelf.bottom, shelf.width() * 0.45f, paint);
        c.restore();
        c.restore();

        // bright rim
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * d);
        paint.setColor(0xA6FFFFFF);
        c.drawRoundRect(shelf, rad, rad, paint);
        super.onDraw(c);
    }

    /** The glossy Start orb: an aqua glass ball with a little cluster of white bubbles. */
    static final class StartOrb extends View {
        StartOrb(Context c) {
            super(c);
            setClickable(true);
            setContentDescription(c.getString(R.string.start));
            setHapticFeedbackEnabled(true);
        }

        @Override
        public void setPressed(boolean pressed) {
            boolean was = isPressed();
            super.setPressed(pressed);
            if (was == pressed) return;
            animate().cancel();
            if (pressed) {
                animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start();
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            } else {
                animate().scaleX(1f).scaleY(1f).setDuration(280).setInterpolator(new OvershootInterpolator(3f)).start();
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() * 0.5f;
            float r = Math.min(getWidth(), getHeight()) * 0.42f;
            OrbIcons.drawOrb(c, cx, cy, r, OrbIcons.PAL[OrbIcons.GLOBE], true);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xF2FFFFFF);
            p.setShadowLayer(r * 0.08f, 0, r * 0.03f, 0x66002A70);
            c.drawCircle(cx - r * 0.14f, cy + r * 0.12f, r * 0.26f, p);
            c.drawCircle(cx + r * 0.24f, cy - r * 0.08f, r * 0.17f, p);
            c.drawCircle(cx + r * 0.16f, cy + r * 0.34f, r * 0.11f, p);
            OrbIcons.drawGloss(c, cx, cy, r);
        }
    }
}
