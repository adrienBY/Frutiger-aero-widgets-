package fr.adrien.aeroclock;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

/** Setup screen: grants location so the weather widget can show local conditions. */
public class WeatherSetupActivity extends Activity {

    private static final int REQ_LOCATION = 1;
    private static final int REQ_BACKGROUND = 2;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(pad, dp(48), pad, pad);
        root.setBackgroundColor(0xFF0C34D2);

        ImageView preview = new ImageView(this);
        preview.setImageResource(R.drawable.wx_preview);
        preview.setAdjustViewBounds(true);
        preview.setContentDescription(getString(R.string.weather_name));
        root.addView(preview, new LinearLayout.LayoutParams(dp(220), dp(220)));

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(24), 0, dp(24));
        root.addView(status);

        Button locate = new Button(this);
        locate.setText(R.string.weather_use_location);
        locate.setOnClickListener(v -> requestPermissions(new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION));
        root.addView(locate);

        Button background = new Button(this);
        background.setText(R.string.weather_background);
        background.setOnClickListener(v -> requestPermissions(new String[]{
                Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQ_BACKGROUND));
        root.addView(background);

        TextView hint = new TextView(this);
        hint.setTextColor(0xCCFFFFFF);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(16), 0, 0);
        hint.setText(R.string.weather_hint);
        root.addView(hint);

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStatus();
        if (hasLocation()) locateAndRefresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (hasLocation()) locateAndRefresh();
        showStatus();
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void locateAndRefresh() {
        LocationManager lm = getSystemService(LocationManager.class);
        String prov = lm.hasProvider(LocationManager.FUSED_PROVIDER) ? LocationManager.FUSED_PROVIDER
                : LocationManager.NETWORK_PROVIDER;
        try {
            lm.getCurrentLocation(prov, null, getMainExecutor(), loc -> {
                Location l = loc != null ? loc : WeatherUpdater.lastKnown(this);
                WeatherUpdater.requestRefresh(this, l);
                status.postDelayed(this::showStatus, 3000);
            });
        } catch (SecurityException | IllegalArgumentException e) {
            WeatherUpdater.requestRefresh(this, WeatherUpdater.lastKnown(this));
        }
    }

    private void showStatus() {
        SharedPreferences sp = getSharedPreferences("aero_weather", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        if (!hasLocation()) {
            sb.append(getString(R.string.weather_need_location));
        } else if (sp.contains("code")) {
            int code = sp.getInt("code", 0);
            sb.append(Math.round(sp.getFloat("temp", 0))).append("° · ")
                    .append(WeatherCodes.label(code, sp.getBoolean("day", true)))
                    .append("\n").append(sp.getString("place", ""))
                    .append("\n").append(getString(R.string.weather_updated)).append(' ')
                    .append(DateFormat.getTimeInstance(DateFormat.SHORT)
                            .format(new Date(sp.getLong("updated", 0))));
        } else {
            sb.append(getString(R.string.weather_loading));
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            sb.append("\n").append(getString(R.string.weather_auto_on));
        }
        status.setText(sb);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
