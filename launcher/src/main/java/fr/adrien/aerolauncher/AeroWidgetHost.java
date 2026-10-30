package fr.adrien.aerolauncher;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/** Hosts other apps' widgets. Host views report long presses so widgets can be moved or removed. */
final class AeroWidgetHost extends AppWidgetHost {

    AeroWidgetHost(Context c) {
        super(c, 0xAE20);
    }

    @Override
    protected AppWidgetHostView onCreateView(Context c, int id, AppWidgetProviderInfo info) {
        return new HostView(c);
    }

    static final class HostView extends AppWidgetHostView {
        private final Runnable longPress = this::fireLongPress;
        private boolean fired;
        private float downX, downY;
        private final int slop;

        HostView(Context c) {
            super(c);
            slop = ViewConfiguration.get(c).getScaledTouchSlop();
        }

        private void fireLongPress() {
            fired = true;
            performLongClick();
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    fired = false;
                    downX = e.getX();
                    downY = e.getY();
                    postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.hypot(e.getX() - downX, e.getY() - downY) > slop) removeCallbacks(longPress);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    removeCallbacks(longPress);
                    break;
                default:
                    break;
            }
            return fired;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) removeCallbacks(longPress);
            return fired || super.onTouchEvent(e);
        }
    }
}
