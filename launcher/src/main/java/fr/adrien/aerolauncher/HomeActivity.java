package fr.adrien.aerolauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Aero Launcher home screen: grid of apps and widgets over the wallpaper, glass dock, Start menu. */
public class HomeActivity extends Activity implements DrawerView.Host {

    private static final int REQ_BIND = 1, REQ_CONFIG = 2;
    private static final int COLS = 4, ROWS = 6;

    private AppWidgetManager awm;
    private AeroWidgetHost host;
    private HomeStore store;

    private List<AppInfo> apps = new ArrayList<>();
    private final Map<String, AppInfo> byKey = new HashMap<>();
    private final List<HomeStore.Item> items = new ArrayList<>();
    private final Map<HomeStore.Item, View> views = new IdentityHashMap<>();
    private String[] dock = new String[4];

    private FrameLayout root;
    private BubbleView bubbles;
    private HomeGrid grid;
    private DockView dockView;
    private DrawerView drawer;
    private View anchor;
    private boolean drawerOpen;
    private HomeStore.Item moving;
    private int pendingWidget = -1;
    private LauncherApps.Callback appsCallback;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Aero.init(this);
        getWindow().setDecorFitsSystemWindows(false);

        awm = AppWidgetManager.getInstance(this);
        host = new AeroWidgetHost(getApplicationContext());
        store = new HomeStore(this);

        root = new FrameLayout(this);
        bubbles = new BubbleView(this);
        root.addView(bubbles, match());

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        grid = new HomeGrid(this);
        int pad = Aero.dp(this, 10);
        grid.setPadding(pad, pad, pad, 0);
        column.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        dockView = new DockView(this);
        column.addView(dockView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Aero.dp(this, 112)));
        root.addView(column, match());

        anchor = new View(this);
        root.addView(anchor, new FrameLayout.LayoutParams(1, 1));

        drawer = new DrawerView(this, this);
        drawer.setVisibility(View.GONE);
        root.addView(drawer, match());
        setContentView(root);

        root.setOnApplyWindowInsetsListener((v, ins) -> {
            Insets sys = ins.getInsets(WindowInsets.Type.systemBars());
            column.setPadding(0, sys.top, 0, sys.bottom);
            drawer.setPadding(0, sys.top, 0, 0);
            drawer.panel.setPadding(drawer.panel.getPaddingLeft(), drawer.panel.getPaddingTop(),
                    drawer.panel.getPaddingRight(), sys.bottom);
            return ins;
        });
        WindowInsetsController wic = getWindow().getInsetsController();
        if (wic != null) {
            wic.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }

        grid.setOnLongClickListener(v -> {
            showHomeMenu();
            return true;
        });
        grid.setOnClickListener(v -> {
            if (moving != null) moveTo(grid.downX, grid.downY);
        });
        dockView.start.setOnClickListener(v -> openDrawer());

        reloadApps();
        if (store.isFirstRun()) firstRun();
        else items.addAll(store.items());
        dock = store.dock();
        buildHome();
        buildDock();

        appsCallback = new LauncherApps.Callback() {
            @Override public void onPackageRemoved(String p, UserHandle u) { refreshApps(); }
            @Override public void onPackageAdded(String p, UserHandle u) { refreshApps(); }
            @Override public void onPackageChanged(String p, UserHandle u) { refreshApps(); }
            @Override public void onPackagesAvailable(String[] p, UserHandle u, boolean r) { refreshApps(); }
            @Override public void onPackagesUnavailable(String[] p, UserHandle u, boolean r) { refreshApps(); }
        };
        getSystemService(LauncherApps.class).registerCallback(appsCallback, new Handler(Looper.getMainLooper()));
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appsCallback != null) getSystemService(LauncherApps.class).unregisterCallback(appsCallback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            host.startListening();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            host.stopListening();
        } catch (Exception ignored) {
        }
        if (drawerOpen) closeDrawer(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bubbles.setRunning(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        bubbles.setRunning(false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home
        if (drawerOpen) closeDrawer(true);
        moving = null;
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) closeDrawer(true);
        else moving = null;
        // never finish: this is the home screen
    }

    // ---------------------------------------------------------------- apps

    private void reloadApps() {
        apps = AppInfo.load(this);
        byKey.clear();
        for (AppInfo a : apps) byKey.put(a.key(), a);
        drawer.setApps(apps);
    }

    private void refreshApps() {
        reloadApps();
        buildHome();
        buildDock();
    }

    private void firstRun() {
        String[] d = new String[4];
        d[0] = resolve(new Intent(Intent.ACTION_DIAL));
        d[1] = resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING));
        d[2] = resolve(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER));
        d[3] = resolve(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
        store.saveDock(d);

        HomeStore.Item spin = new HomeStore.Item();
        spin.type = HomeStore.SPINNER;
        spin.x = 2;
        spin.y = ROWS - 2;
        spin.sx = 2;
        spin.sy = 2;
        items.add(spin);
        store.saveItems(items);
    }

    /** The launchable activity of the app that handles this intent by default. */
    private String resolve(Intent i) {
        ResolveInfo r = getPackageManager().resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
        if (r == null || r.activityInfo == null) return null;
        String pkg = r.activityInfo.packageName;
        if ("android".equals(pkg)) return null; // the chooser: no default set
        for (AppInfo a : apps) if (a.component.getPackageName().equals(pkg)) return a.key();
        return null;
    }

    // ---------------------------------------------------------------- home grid

    private void buildHome() {
        grid.removeAllViews();
        views.clear();
        for (HomeStore.Item it : items) {
            View v = createView(it);
            if (v == null) continue;
            views.put(it, v);
            grid.addView(v, new CellLayout.LP(it.x, it.y, it.sx, it.sy));
        }
        grid.post(this::sizeWidgets);
    }

    private View createView(HomeStore.Item it) {
        View v;
        switch (it.type) {
            case HomeStore.APP: {
                AppInfo a = byKey.get(it.component);
                if (a == null) return null;
                v = new AppIconView(this, a, 58, true, false);
                break;
            }
            case HomeStore.WIDGET: {
                AppWidgetProviderInfo info = awm.getAppWidgetInfo(it.widgetId);
                if (info == null) return null;
                AppWidgetHostView hv = host.createView(this, it.widgetId, info);
                hv.setPadding(0, 0, 0, 0);
                v = hv;
                break;
            }
            case HomeStore.SPINNER:
                v = new SpinnerView(this);
                break;
            default:
                return null;
        }
        v.setOnLongClickListener(x -> {
            itemMenu(it, x);
            return true;
        });
        return v;
    }

    @SuppressWarnings("deprecation")
    private void sizeWidgets() {
        float d = getResources().getDisplayMetrics().density;
        for (Map.Entry<HomeStore.Item, View> e : views.entrySet()) {
            if (!(e.getValue() instanceof AppWidgetHostView)) continue;
            HomeStore.Item it = e.getKey();
            int w = Math.round(it.sx * grid.cellW() / d), h = Math.round(it.sy * grid.cellH() / d);
            ((AppWidgetHostView) e.getValue()).updateAppWidgetSize(new Bundle(), w, h, w, h);
        }
    }

    private void placeAnchor(View inside, float x, float y) {
        int[] loc = new int[2];
        inside.getLocationInWindow(loc);
        anchor.setX(loc[0] + x);
        anchor.setY(loc[1] + y);
    }

    private void showHomeMenu() {
        placeAnchor(grid, grid.downX, grid.downY);
        PopupMenu m = new PopupMenu(this, anchor, Gravity.NO_GRAVITY);
        Menu menu = m.getMenu();
        menu.add(0, 1, 0, R.string.add_widget);
        menu.add(0, 2, 1, R.string.add_spinner);
        menu.add(0, 3, 2, R.string.wallpaper);
        menu.add(0, 4, 3, R.string.home_settings);
        m.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1: showWidgetPicker(); break;
                case 2: addSpinner(); break;
                case 3:
                    try {
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),
                                getString(R.string.wallpaper)));
                    } catch (Exception ignored) {
                    }
                    break;
                case 4:
                    try {
                        startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                    } catch (Exception ignored) {
                    }
                    break;
                default:
                    break;
            }
            return true;
        });
        m.show();
    }

    private void itemMenu(HomeStore.Item it, View v) {
        PopupMenu m = new PopupMenu(this, v);
        m.getMenu().add(0, 1, 0, R.string.move);
        m.getMenu().add(0, 2, 1, R.string.remove);
        if (HomeStore.APP.equals(it.type)) m.getMenu().add(0, 3, 2, R.string.app_info);
        m.setOnMenuItemClickListener(mi -> {
            if (mi.getItemId() == 1) {
                moving = it;
                Toast.makeText(this, R.string.tap_free_cell, Toast.LENGTH_SHORT).show();
            } else if (mi.getItemId() == 2) {
                if (HomeStore.WIDGET.equals(it.type)) host.deleteAppWidgetId(it.widgetId);
                items.remove(it);
                store.saveItems(items);
                buildHome();
            } else if (mi.getItemId() == 3) {
                AppInfo a = byKey.get(it.component);
                if (a != null) a.openInfo(v);
            }
            return true;
        });
        m.show();
    }

    private void moveTo(float px, float py) {
        HomeStore.Item it = moving;
        moving = null;
        int[] cell = grid.cellAt(px, py);
        int x = Math.min(cell[0], COLS - it.sx), y = Math.min(cell[1], ROWS - it.sy);
        if (!grid.isFree(x, y, it.sx, it.sy, views.get(it))) {
            Toast.makeText(this, R.string.cell_taken, Toast.LENGTH_SHORT).show();
            return;
        }
        it.x = x;
        it.y = y;
        store.saveItems(items);
        buildHome();
    }

    private boolean addItem(HomeStore.Item it) {
        int[] spot = grid.findFree(it.sx, it.sy);
        if (spot == null) {
            Toast.makeText(this, R.string.no_room, Toast.LENGTH_SHORT).show();
            return false;
        }
        it.x = spot[0];
        it.y = spot[1];
        items.add(it);
        store.saveItems(items);
        buildHome();
        return true;
    }

    private void addSpinner() {
        HomeStore.Item it = new HomeStore.Item();
        it.type = HomeStore.SPINNER;
        it.sx = 2;
        it.sy = 2;
        addItem(it);
    }

    // ---------------------------------------------------------------- widgets

    private void showWidgetPicker() {
        List<AppWidgetProviderInfo> list = awm.getInstalledProvidersForProfile(Process.myUserHandle());
        PackageManager pm = getPackageManager();
        List<AppWidgetProviderInfo> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> a.loadLabel(pm).compareToIgnoreCase(b.loadLabel(pm)));
        String[] labels = new String[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            AppWidgetProviderInfo w = sorted.get(i);
            String app;
            try {
                app = pm.getApplicationLabel(pm.getApplicationInfo(w.provider.getPackageName(), 0)).toString();
            } catch (Exception e) {
                app = w.provider.getPackageName();
            }
            labels[i] = w.loadLabel(pm) + "  ·  " + app;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_widget)
                .setItems(labels, (dlg, which) -> pickWidget(sorted.get(which)))
                .show();
    }

    private void pickWidget(AppWidgetProviderInfo info) {
        int id = host.allocateAppWidgetId();
        if (awm.bindAppWidgetIdIfAllowed(id, info.getProfile(), info.provider, null)) {
            configureOrAdd(id);
        } else {
            pendingWidget = id;
            Intent i = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.getProfile());
            startActivityForResult(i, REQ_BIND);
        }
    }

    private void configureOrAdd(int id) {
        AppWidgetProviderInfo info = awm.getAppWidgetInfo(id);
        if (info == null) {
            host.deleteAppWidgetId(id);
            return;
        }
        boolean needsConfig = info.configure != null
                && (info.widgetFeatures & AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) == 0;
        if (needsConfig) {
            pendingWidget = id;
            try {
                host.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIG, null);
                return;
            } catch (Exception ignored) {
                // fall through and add it unconfigured
            }
        }
        addWidget(id);
    }

    private void addWidget(int id) {
        AppWidgetProviderInfo info = awm.getAppWidgetInfo(id);
        if (info == null) {
            host.deleteAppWidgetId(id);
            return;
        }
        int sx = info.targetCellWidth, sy = info.targetCellHeight;
        if (sx <= 0) sx = (int) Math.ceil(info.minWidth / Math.max(1f, grid.cellW()));
        if (sy <= 0) sy = (int) Math.ceil(info.minHeight / Math.max(1f, grid.cellH()));
        HomeStore.Item it = new HomeStore.Item();
        it.type = HomeStore.WIDGET;
        it.widgetId = id;
        it.sx = Math.max(1, Math.min(COLS, sx));
        it.sy = Math.max(1, Math.min(ROWS, sy));
        if (!addItem(it)) host.deleteAppWidgetId(id);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        int id = pendingWidget;
        if (data != null) id = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        if (id < 0) return;
        if (req == REQ_BIND) {
            if (res == RESULT_OK) configureOrAdd(id);
            else host.deleteAppWidgetId(id);
        } else if (req == REQ_CONFIG) {
            if (res == RESULT_OK) addWidget(id);
            else host.deleteAppWidgetId(id);
        }
    }

    // ---------------------------------------------------------------- dock

    private void buildDock() {
        View[] slots = new View[4];
        for (int i = 0; i < 4; i++) {
            AppInfo a = dock[i] == null ? null : byKey.get(dock[i]);
            if (a == null) continue;
            AppIconView v = new AppIconView(this, a, 54, false, false);
            final int slot = i;
            v.setOnLongClickListener(x -> {
                PopupMenu m = new PopupMenu(this, x);
                m.getMenu().add(0, 1, 0, R.string.remove);
                m.getMenu().add(0, 2, 1, R.string.app_info);
                m.setOnMenuItemClickListener(mi -> {
                    if (mi.getItemId() == 1) {
                        dock[slot] = null;
                        store.saveDock(dock);
                        buildDock();
                    } else {
                        a.openInfo(x);
                    }
                    return true;
                });
                m.show();
                return true;
            });
            slots[i] = v;
        }
        dockView.setSlots(slots);
    }

    private void addToDock(AppInfo app) {
        String[] names = new String[4];
        for (int i = 0; i < 4; i++) {
            AppInfo a = dock[i] == null ? null : byKey.get(dock[i]);
            names[i] = (i + 1) + "  ·  " + (a == null ? "—" : a.label);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.dock_slot)
                .setItems(names, (dlg, which) -> {
                    dock[which] = app.key();
                    store.saveDock(dock);
                    buildDock();
                    closeDrawer(true);
                })
                .show();
    }

    // ---------------------------------------------------------------- drawer

    void openDrawer() {
        if (drawerOpen) return;
        drawerOpen = true;
        drawer.setVisibility(View.VISIBLE);
        drawer.setAlpha(0f);
        drawer.animate().alpha(1f).setDuration(180).start();
        drawer.panel.setTranslationY(root.getHeight() * 0.5f);
        drawer.panel.animate().translationY(0).setDuration(300)
                .setInterpolator(new DecelerateInterpolator(2.2f)).start();
        setBlur(true);
    }

    @Override
    public void closeDrawer() {
        closeDrawer(true);
    }

    private void closeDrawer(boolean animate) {
        if (!drawerOpen) return;
        drawerOpen = false;
        drawer.reset();
        setBlur(false);
        if (!animate) {
            drawer.setVisibility(View.GONE);
            return;
        }
        drawer.panel.animate().translationY(root.getHeight() * 0.5f).setDuration(220)
                .setInterpolator(new AccelerateInterpolator(1.6f)).start();
        drawer.animate().alpha(0f).setDuration(220).withEndAction(() -> {
            if (!drawerOpen) drawer.setVisibility(View.GONE);
        }).start();
    }

    /** Frosts the wallpaper behind the Start menu where the device supports window blur. */
    private void setBlur(boolean on) {
        try {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (on) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                lp.setBlurBehindRadius(Aero.dp(this, 36));
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                lp.setBlurBehindRadius(0);
            }
            lp.dimAmount = 0f;
            getWindow().setAttributes(lp);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onAppLongPress(AppInfo app, View anchorView) {
        PopupMenu m = new PopupMenu(this, anchorView);
        m.getMenu().add(0, 1, 0, R.string.add_home);
        m.getMenu().add(0, 2, 1, R.string.add_dock);
        m.getMenu().add(0, 3, 2, R.string.app_info);
        m.getMenu().add(0, 4, 3, R.string.uninstall);
        m.setOnMenuItemClickListener(mi -> {
            switch (mi.getItemId()) {
                case 1: {
                    HomeStore.Item it = new HomeStore.Item();
                    it.type = HomeStore.APP;
                    it.component = app.key();
                    if (addItem(it)) closeDrawer(true);
                    break;
                }
                case 2:
                    addToDock(app);
                    break;
                case 3:
                    app.openInfo(anchorView);
                    break;
                case 4:
                    try {
                        startActivity(new Intent(Intent.ACTION_DELETE,
                                Uri.parse("package:" + app.component.getPackageName())));
                    } catch (Exception ignored) {
                    }
                    break;
                default:
                    break;
            }
            return true;
        });
        m.show();
    }

    // ---------------------------------------------------------------- gestures

    private void expandNotifications() {
        try {
            Object sb = getSystemService("statusbar");
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb);
        } catch (Exception ignored) {
        }
    }

    /** The grid, plus swipe up for the Start menu and swipe down for notifications. */
    private final class HomeGrid extends CellLayout {
        float downX, downY;
        private boolean swiped;
        private View target;
        private final float threshold;

        HomeGrid(android.content.Context c) {
            super(c, COLS, ROWS);
            setClickable(true);
            setLongClickable(true);
            threshold = Aero.dp(c, 48);
        }

        private View childAt(float x, float y) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View v = getChildAt(i);
                if (x >= v.getLeft() && x < v.getRight() && y >= v.getTop() && y < v.getBottom()) return v;
            }
            return null;
        }

        /** True once a vertical swipe has been recognised. */
        private boolean track(android.view.MotionEvent e) {
            switch (e.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    swiped = false;
                    target = childAt(downX, downY);
                    return false;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (swiped || target instanceof SpinnerView) return swiped;
                    float dx = e.getX() - downX, dy = e.getY() - downY;
                    if (Math.abs(dy) > threshold && Math.abs(dy) > 2 * Math.abs(dx)) {
                        swiped = true;
                        if (dy < 0) openDrawer();
                        else expandNotifications();
                    }
                    return swiped;
                default:
                    return swiped;
            }
        }

        @Override
        public boolean onInterceptTouchEvent(android.view.MotionEvent e) {
            return track(e);
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            boolean s = track(e);
            if (s) {
                cancelLongPress();
                setPressed(false);
                return true;
            }
            return super.onTouchEvent(e);
        }
    }
}
