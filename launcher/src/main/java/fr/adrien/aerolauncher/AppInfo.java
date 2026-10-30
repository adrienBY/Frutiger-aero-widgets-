package fr.adrien.aerolauncher;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.view.View;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/** One launchable app. The glass orb icon is rendered on first use and cached. */
final class AppInfo {

    final ComponentName component;
    final UserHandle user;
    final String label;
    final Drawable icon;
    private Bitmap orb;
    private int orbSize;

    private AppInfo(LauncherActivityInfo i) {
        component = i.getComponentName();
        user = i.getUser();
        CharSequence l = i.getLabel();
        label = l == null ? component.getPackageName() : l.toString();
        icon = i.getIcon(0);
    }

    String key() {
        return component.flattenToString();
    }

    Bitmap orb(int size) {
        if (orb == null || orbSize != size) {
            orb = OrbIcons.render(icon, size, component.getPackageName());
            orbSize = size;
        }
        return orb;
    }

    static List<AppInfo> load(Context c) {
        LauncherApps la = c.getSystemService(LauncherApps.class);
        List<AppInfo> out = new ArrayList<>();
        for (LauncherActivityInfo i : la.getActivityList(null, Process.myUserHandle())) {
            if (i.getComponentName().getPackageName().equals(c.getPackageName())) continue;
            out.add(new AppInfo(i));
        }
        Collator col = Collator.getInstance();
        out.sort((a, b) -> col.compare(a.label, b.label));
        return out;
    }

    void launch(View from) {
        LauncherApps la = from.getContext().getSystemService(LauncherApps.class);
        Rect r = new Rect();
        from.getGlobalVisibleRect(r);
        try {
            la.startMainActivity(component, user, r, ActivityOptions.makeScaleUpAnimation(
                    from, 0, 0, from.getWidth(), from.getHeight()).toBundle());
        } catch (Exception e) {
            Toast.makeText(from.getContext(), R.string.cant_open, Toast.LENGTH_SHORT).show();
        }
    }

    void openInfo(View from) {
        LauncherApps la = from.getContext().getSystemService(LauncherApps.class);
        try {
            la.startAppDetailsActivity(component, user, null, null);
        } catch (Exception ignored) {
        }
    }
}
