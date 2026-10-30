package fr.adrien.aerolauncher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The Start menu: a frosted Aero glass panel with search and every app as a glass orb. */
final class DrawerView extends FrameLayout {

    interface Host {
        void onAppLongPress(AppInfo app, View anchor);
        void closeDrawer();
    }

    private final Host host;
    final LinearLayout panel;
    final EditText search;
    private final GridView grid;
    private final Adapter adapter = new Adapter();
    private List<AppInfo> all = new ArrayList<>();
    private final List<AppInfo> shown = new ArrayList<>();

    DrawerView(Context c, Host host) {
        super(c);
        this.host = host;
        setBackgroundColor(0x33001040);
        setOnClickListener(v -> host.closeDrawer());

        panel = new GlassPanel(c);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setClickable(true); // taps on the glass don't close it
        int pad = Aero.dp(c, 16);
        panel.setPadding(pad, Aero.dp(c, 20), pad, 0);
        LayoutParams plp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        plp.topMargin = Aero.dp(c, 56);
        addView(panel, plp);

        search = new EditText(c);
        search.setHint(R.string.search_hint);
        search.setSingleLine(true);
        search.setTypeface(Aero.regular);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        search.setTextColor(Aero.NAVY);
        search.setHintTextColor(0x990A3C8C);
        search.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.setPadding(Aero.dp(c, 20), 0, Aero.dp(c, 20), 0);
        GradientDrawable pill = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFFFFFFF, 0xFFE8F4FC});
        pill.setCornerRadius(Aero.dp(c, 24));
        pill.setStroke(Aero.dp(c, 1.5f), 0xFF9FCDEE);
        search.setBackground(pill);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            public void onTextChanged(CharSequence s, int a, int b, int d) { filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        search.setOnEditorActionListener((v, id, ev) -> {
            boolean go = id == EditorInfo.IME_ACTION_GO
                    || (ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (go && !shown.isEmpty()) {
                shown.get(0).launch(v);
                return true;
            }
            return false;
        });
        panel.addView(search, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Aero.dp(c, 48)));

        grid = new GridView(c);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(Aero.dp(c, 14));
        grid.setPadding(0, Aero.dp(c, 18), 0, Aero.dp(c, 40));
        grid.setClipToPadding(false);
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setVerticalScrollBarEnabled(false);
        grid.setAdapter(adapter);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        panel.addView(grid, glp);
    }

    void setApps(List<AppInfo> apps) {
        all = apps;
        filter(search.getText().toString());
    }

    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private void filter(String q) {
        shown.clear();
        String f = fold(q.trim());
        for (AppInfo a : all) if (f.isEmpty() || fold(a.label).contains(f)) shown.add(a);
        adapter.notifyDataSetChanged();
    }

    void reset() {
        search.setText("");
        grid.setSelection(0);
        InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
        if (imm != null) imm.hideSoftInputFromWindow(getWindowToken(), 0);
        search.clearFocus();
    }

    private final class Adapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int i) { return shown.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int i, View convert, ViewGroup parent) {
            AppInfo app = shown.get(i);
            AppIconView v = new AppIconView(getContext(), app, 56, true, true);
            v.setOnLongClickListener(x -> {
                host.onAppLongPress(app, x);
                return true;
            });
            v.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, Aero.dp(getContext(), 92)));
            return v;
        }
    }

    /** Light aqua frosted glass with a gloss band, like the Vista Start menu but bright. */
    private static final class GlassPanel extends LinearLayout {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();

        GlassPanel(Context c) {
            super(c);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas c) {
            float d = getResources().getDisplayMetrics().density;
            float rad = 30 * d;
            r.set(0, 0, getWidth(), getHeight() + rad);
            Path clip = new Path();
            clip.addRoundRect(r, rad, rad, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            p.setShader(new LinearGradient(0, 0, 0, getHeight(),
                    new int[]{0xEBF4FBFF, 0xE0D4ECFF, 0xE6BFE2FF}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(r, p);
            p.setShader(new LinearGradient(0, 0, 0, 90 * d, 0xB3FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), 90 * d, p);
            c.restore();
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f * d);
            p.setColor(0xCCFFFFFF);
            c.drawRoundRect(r, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
            super.onDraw(c);
        }
    }
}
