package fr.adrien.aeroclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Aero Blue weather widget. Refreshes every 30 minutes and when tapped. */
public class WeatherWidget extends AppWidgetProvider {

    static final String ACTION_REFRESH = "fr.adrien.aeroclock.WEATHER_REFRESH";
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_REFRESH.equals(intent.getAction())) {
            Location loc = null;
            if (intent.hasExtra("lat")) {
                loc = new Location("tap");
                loc.setLatitude(intent.getDoubleExtra("lat", 0));
                loc.setLongitude(intent.getDoubleExtra("lon", 0));
            }
            run(context, loc, true);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        run(context, null, false);
    }

    private void run(Context context, Location loc, boolean force) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            try {
                WeatherUpdater.refresh(app, loc, force);
            } finally {
                pending.finish();
            }
        });
    }
}
