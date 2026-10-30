package fr.adrien.aerolauncher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Saves what is on the home grid and in the dock. */
final class HomeStore {

    static final String APP = "app", WIDGET = "widget", SPINNER = "spinner";

    static final class Item {
        String type;
        String component;   // apps
        int widgetId = -1;  // hosted widgets
        int x, y, sx = 1, sy = 1;
    }

    private final SharedPreferences sp;

    HomeStore(Context c) {
        sp = c.getSharedPreferences("aero_home", Context.MODE_PRIVATE);
    }

    boolean isFirstRun() {
        return !sp.contains("items");
    }

    List<Item> items() {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Item it = new Item();
                it.type = o.getString("t");
                it.component = o.optString("c", null);
                it.widgetId = o.optInt("w", -1);
                it.x = o.getInt("x");
                it.y = o.getInt("y");
                it.sx = o.optInt("sx", 1);
                it.sy = o.optInt("sy", 1);
                out.add(it);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    void saveItems(List<Item> items) {
        JSONArray a = new JSONArray();
        try {
            for (Item it : items) {
                JSONObject o = new JSONObject();
                o.put("t", it.type);
                if (it.component != null) o.put("c", it.component);
                if (it.widgetId >= 0) o.put("w", it.widgetId);
                o.put("x", it.x).put("y", it.y).put("sx", it.sx).put("sy", it.sy);
                a.put(o);
            }
        } catch (Exception ignored) {
        }
        sp.edit().putString("items", a.toString()).apply();
    }

    /** Four dock slots; null means empty. */
    String[] dock() {
        String[] out = new String[4];
        try {
            JSONArray a = new JSONArray(sp.getString("dock", "[]"));
            for (int i = 0; i < 4 && i < a.length(); i++) {
                out[i] = a.isNull(i) ? null : a.getString(i);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    boolean hasDock() {
        return sp.contains("dock");
    }

    void saveDock(String[] dock) {
        JSONArray a = new JSONArray();
        for (String s : dock) a.put(s == null ? JSONObject.NULL : s);
        sp.edit().putString("dock", a.toString()).apply();
    }
}
