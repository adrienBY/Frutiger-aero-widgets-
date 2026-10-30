package fr.adrien.aerolauncher;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

/** Shared look: Selawik fonts, colors, dp helper. */
final class Aero {

    static final int NAVY = 0xFF0A3C8C;
    static final int SHADOW = 0x99000A3C;

    static Typeface light, regular, bold;

    private Aero() {}

    static void init(Context c) {
        if (regular != null) return;
        AssetManager a = c.getAssets();
        light = Typeface.createFromAsset(a, "fonts/Selawik-Light.ttf");
        regular = Typeface.createFromAsset(a, "fonts/Selawik-Regular.ttf");
        bold = Typeface.createFromAsset(a, "fonts/Selawik-Bold.ttf");
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** White label with a soft navy shadow, like Vista desktop icons. */
    static void styleLabel(TextView t, boolean onGlass) {
        t.setTypeface(regular);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        if (onGlass) {
            t.setTextColor(NAVY);
            t.setShadowLayer(4, 0, 0, 0xCCFFFFFF);
        } else {
            t.setTextColor(0xFFFFFFFF);
            t.setShadowLayer(5, 0, 1.5f, SHADOW);
        }
    }
}
