package com.fanli.wifiportal;

import android.content.SharedPreferences;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PortalSnapshots {
    private static final String ORIGINAL_EXISTS = "snapshot.original.exists.";
    private static final String ORIGINAL_READABLE = "snapshot.original.readable.";
    private static final String ORIGINAL_VALUE = "snapshot.original.value.";
    private static final String ORIGINAL_SOURCE = "snapshot.original.source.";
    private static final String CURRENT_EXISTS = "snapshot.current.exists.";
    private static final String CURRENT_READABLE = "snapshot.current.readable.";
    private static final String CURRENT_VALUE = "snapshot.current.value.";
    private static final String CURRENT_SOURCE = "snapshot.current.source.";

    private PortalSnapshots() {
    }

    static void saveCurrent(SharedPreferences prefs, Map<String, PortalValue> values) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, PortalValue> entry : values.entrySet()) {
            PortalValue value = entry.getValue();
            editor.putBoolean(CURRENT_READABLE + entry.getKey(), value.readable);
            editor.putBoolean(CURRENT_EXISTS + entry.getKey(), value.exists);
            editor.putString(CURRENT_VALUE + entry.getKey(), value.value);
            editor.putString(CURRENT_SOURCE + entry.getKey(), value.source);
        }
        editor.apply();
    }

    static int backupMissingOriginals(SharedPreferences prefs, List<PortalSetting> settings, Map<String, PortalValue> values) {
        SharedPreferences.Editor editor = prefs.edit();
        int saved = 0;
        for (PortalSetting setting : settings) {
            if (original(prefs, setting) != null) {
                continue;
            }
            PortalValue value = values.get(setting.id());
            if (value == null || !value.readable) {
                continue;
            }
            editor.putBoolean(ORIGINAL_READABLE + setting.id(), true);
            editor.putBoolean(ORIGINAL_EXISTS + setting.id(), value.exists);
            editor.putString(ORIGINAL_VALUE + setting.id(), value.value);
            editor.putString(ORIGINAL_SOURCE + setting.id(), value.source);
            saved++;
        }
        editor.apply();
        return saved;
    }

    static int overwriteOriginals(SharedPreferences prefs, Map<String, PortalValue> values) {
        SharedPreferences.Editor editor = prefs.edit();
        int saved = 0;
        for (Map.Entry<String, PortalValue> entry : values.entrySet()) {
            PortalValue value = entry.getValue();
            if (!value.readable) {
                continue;
            }
            editor.putBoolean(ORIGINAL_READABLE + entry.getKey(), true);
            editor.putBoolean(ORIGINAL_EXISTS + entry.getKey(), value.exists);
            editor.putString(ORIGINAL_VALUE + entry.getKey(), value.value);
            editor.putString(ORIGINAL_SOURCE + entry.getKey(), value.source);
            saved++;
        }
        editor.apply();
        return saved;
    }

    static boolean hasOriginal(SharedPreferences prefs, PortalSetting setting) {
        return original(prefs, setting) != null;
    }

    static PortalValue original(SharedPreferences prefs, PortalSetting setting) {
        if (!prefs.contains(ORIGINAL_EXISTS + setting.id())) {
            return null;
        }
        boolean readable = readable(prefs, ORIGINAL_READABLE + setting.id(), ORIGINAL_SOURCE + setting.id());
        if (!readable) {
            return null;
        }
        boolean exists = prefs.getBoolean(ORIGINAL_EXISTS + setting.id(), false);
        String value = prefs.getString(ORIGINAL_VALUE + setting.id(), "");
        String source = prefs.getString(ORIGINAL_SOURCE + setting.id(), "");
        return exists ? PortalValue.of(value, source) : PortalValue.missing(source);
    }

    static PortalValue current(SharedPreferences prefs, PortalSetting setting) {
        if (!prefs.contains(CURRENT_EXISTS + setting.id())) {
            return null;
        }
        boolean readable = readable(prefs, CURRENT_READABLE + setting.id(), CURRENT_SOURCE + setting.id());
        if (!readable) {
            return PortalValue.readFailure(prefs.getString(CURRENT_SOURCE + setting.id(), ""));
        }
        boolean exists = prefs.getBoolean(CURRENT_EXISTS + setting.id(), false);
        String value = prefs.getString(CURRENT_VALUE + setting.id(), "");
        String source = prefs.getString(CURRENT_SOURCE + setting.id(), "");
        return exists ? PortalValue.of(value, source) : PortalValue.missing(source);
    }

    static Map<String, PortalValue> currentFor(SharedPreferences prefs, List<PortalSetting> settings) {
        Map<String, PortalValue> values = new LinkedHashMap<>();
        for (PortalSetting setting : settings) {
            PortalValue value = current(prefs, setting);
            if (value != null) {
                values.put(setting.id(), value);
            }
        }
        return values;
    }

    private static boolean readable(SharedPreferences prefs, String readableKey, String sourceKey) {
        if (prefs.contains(readableKey)) {
            return prefs.getBoolean(readableKey, true);
        }
        String source = prefs.getString(sourceKey, "");
        return source == null || !source.startsWith("读取失败:");
    }
}
