package fr.adrien.aeroclock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/**
 * Invisible screen opened by tapping the weather widget: grabs a fresh position
 * while the app is in the foreground, asks the widget to refresh, then closes.
 */
public class TapRefreshActivity extends Activity {

    private boolean done;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, WeatherSetupActivity.class));
            finish();
            return;
        }
        Toast.makeText(this, R.string.weather_refreshing, Toast.LENGTH_SHORT).show();
        LocationManager lm = getSystemService(LocationManager.class);
        String prov = lm.hasProvider(LocationManager.FUSED_PROVIDER) ? LocationManager.FUSED_PROVIDER
                : LocationManager.NETWORK_PROVIDER;
        try {
            lm.getCurrentLocation(prov, null, getMainExecutor(), this::finishWith);
        } catch (SecurityException | IllegalArgumentException e) {
            finishWith(null);
        }
        // Don't hang around if the position takes too long.
        new Handler(Looper.getMainLooper()).postDelayed(() -> finishWith(null), 6000);
    }

    private void finishWith(Location loc) {
        if (done) return;
        done = true;
        if (loc == null) loc = WeatherUpdater.lastKnown(this);
        WeatherUpdater.requestRefresh(this, loc);
        finish();
        overridePendingTransition(0, 0);
    }
}
