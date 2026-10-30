package fr.adrien.aerolauncher;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A glass orb icon with its label. Presses squish the orb and give a light haptic. */
final class AppIconView extends LinearLayout {

    final AppInfo app;
    final ImageView orb;
    final TextView label;

    AppIconView(Context c, AppInfo app, int orbDp, boolean showLabel, boolean onGlass) {
        super(c);
        this.app = app;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setClipChildren(false);
        setClipToPadding(false);
        int size = Aero.dp(c, orbDp);

        orb = new ImageView(c);
        orb.setImageBitmap(app.orb(size));
        orb.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(orb, new LayoutParams(size, size));

        label = new TextView(c);
        label.setText(app.label);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);
        Aero.styleLabel(label, onGlass);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Aero.dp(c, 3);
        if (showLabel) addView(label, lp);

        setContentDescription(app.label);
        setClickable(true);
        setLongClickable(true);
        setHapticFeedbackEnabled(true);
        setOnClickListener(v -> {
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            app.launch(orb);
        });
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean was = isPressed();
        super.setPressed(pressed);
        if (was == pressed) return;
        orb.animate().cancel();
        if (pressed) {
            orb.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).start();
        } else {
            orb.animate().scaleX(1f).scaleY(1f).setDuration(260)
                    .setInterpolator(new OvershootInterpolator(3f)).start();
        }
    }
}
