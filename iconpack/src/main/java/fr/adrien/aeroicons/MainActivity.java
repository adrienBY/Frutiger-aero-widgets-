package fr.adrien.aeroicons;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** Preview screen with a one-tap "apply in Nova" button. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.apply).setOnClickListener(v -> {
            Intent nova = new Intent("com.teslacoilsw.launcher.APPLY_ICON_THEME")
                    .setPackage("com.teslacoilsw.launcher")
                    .putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_TYPE", "GO")
                    .putExtra("com.teslacoilsw.launcher.extra.ICON_THEME_PACKAGE", getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(nova);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.no_nova, Toast.LENGTH_LONG).show();
            }
        });
    }
}
