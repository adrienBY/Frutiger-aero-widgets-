package fr.adrien.aeroclock;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Fetches current weather from Open-Meteo, caches it, and pushes the rendered tile. */
final class WeatherUpdater {

    private static final String PREFS = "aero_weather";
    private static final long MIN_REFRESH_MS = 10 * 60 * 1000L;
    private static final int TILE_PX = 600;

    private WeatherUpdater() {}

    /** Blocking; call off the main thread. location may be null (keeps the saved one). */
    static void refresh(Context ctx, Location location, boolean force) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (location == null) location = backgroundLocation(ctx);
        if (location != null) saveLocation(ctx, sp, location);

        if (!sp.contains("lat")) {
            push(ctx);
            return;
        }
        long age = System.currentTimeMillis() - sp.getLong("updated", 0);
        if (force || age > MIN_REFRESH_MS) {
            fetch(sp, sp.getFloat("lat", 0), sp.getFloat("lon", 0));
        }
        push(ctx);
    }

    private static void saveLocation(Context ctx, SharedPreferences sp, Location loc) {
        float[] moved = new float[1];
        boolean hadPlace = sp.contains("place");
        if (sp.contains("lat")) {
            Location.distanceBetween(sp.getFloat("lat", 0), sp.getFloat("lon", 0),
                    loc.getLatitude(), loc.getLongitude(), moved);
        } else {
            moved[0] = Float.MAX_VALUE;
        }
        SharedPreferences.Editor e = sp.edit()
                .putFloat("lat", (float) loc.getLatitude())
                .putFloat("lon", (float) loc.getLongitude());
        if (moved[0] > 400 || !hadPlace) {
            String place = placeName(ctx, loc);
            if (place != null) e.putString("place", place);
            e.putLong("updated", 0); // new spot: fetch fresh weather
        }
        e.apply();
    }

    @SuppressWarnings("deprecation")
    private static String placeName(Context ctx, Location loc) {
        if (!Geocoder.isPresent()) return null;
        try {
            List<Address> list = new Geocoder(ctx, Locale.getDefault())
                    .getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (list == null || list.isEmpty()) return null;
            Address a = list.get(0);
            if (a.getSubLocality() != null) return a.getSubLocality();
            if (a.getLocality() != null) return a.getLocality();
            return a.getSubAdminArea();
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Last known position, only when the user allowed location "all the time". */
    private static Location backgroundLocation(Context ctx) {
        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return null;
        return lastKnown(ctx);
    }

    static Location lastKnown(Context ctx) {
        LocationManager lm = ctx.getSystemService(LocationManager.class);
        if (lm == null) return null;
        Location best = null;
        for (String prov : new String[]{LocationManager.FUSED_PROVIDER,
                LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
            try {
                Location l = lm.getLastKnownLocation(prov);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException | IllegalArgumentException ignored) {
            }
        }
        return best;
    }

    private static void fetch(SharedPreferences sp, float lat, float lon) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code,is_day&timezone=auto", lat, lon));
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) return;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            JSONObject cur = new JSONObject(sb.toString()).getJSONObject("current");
            sp.edit()
                    .putFloat("temp", (float) cur.getDouble("temperature_2m"))
                    .putInt("code", cur.getInt("weather_code"))
                    .putBoolean("day", cur.getInt("is_day") == 1)
                    .putLong("updated", System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) {
            // keep the cached reading
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Renders the cached state (or the setup prompt) into every weather widget. */
    static void push(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, WeatherWidget.class));
        if (ids == null || ids.length == 0) return;

        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Bitmap bmp;
        String scene = "clear";
        boolean day = true;
        if (sp.contains("code")) {
            int code = sp.getInt("code", 0);
            day = sp.getBoolean("day", true);
            scene = WeatherCodes.scene(code);
            int t = Math.round(sp.getFloat("temp", 0));
            String temp = (t == 0 ? "0" : String.valueOf(t)) + "°";
            bmp = AeroWeatherRenderer.render(ctx, scene, !day, temp,
                    WeatherCodes.label(code, day), sp.getString("place", "Ma position"), TILE_PX);
        } else {
            bmp = AeroWeatherRenderer.render(ctx, scene, false, "--°",
                    "Touchez pour activer", "Aero Météo", TILE_PX);
        }
        int bgId = ctx.getResources().getIdentifier(
                "wx_" + scene + (day ? "_d" : "_n"), "drawable", ctx.getPackageName());
        boolean bubbles = scene.equals("clear") || scene.equals("partly") || scene.equals("cloudy");

        Intent tap = new Intent(ctx, TapRefreshActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        PendingIntent pi = PendingIntent.getActivity(ctx, 1, tap,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        for (int id : ids) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_weather);
            if (bgId != 0) v.setImageViewResource(R.id.weather_bg, bgId);
            v.setViewVisibility(R.id.bubbles_day, bubbles && day ? View.VISIBLE : View.GONE);
            v.setViewVisibility(R.id.bubbles_night, bubbles && !day ? View.VISIBLE : View.GONE);
            v.setImageViewBitmap(R.id.weather_image, bmp);
            v.setOnClickPendingIntent(R.id.weather_root, pi);
            mgr.updateAppWidget(id, v);
        }
    }

    /** Asks the widget receiver to refresh, optionally with a fresh position. */
    static void requestRefresh(Context ctx, Location loc) {
        Intent i = new Intent(ctx, WeatherWidget.class).setAction(WeatherWidget.ACTION_REFRESH);
        if (loc != null) {
            i.putExtra("lat", loc.getLatitude());
            i.putExtra("lon", loc.getLongitude());
        }
        ctx.sendBroadcast(i);
    }
}
