package io.github.mkxpz.engine;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ModifierConfig {
    public boolean infiniteHp;
    public boolean infiniteMp;
    public boolean instantKill;
    public boolean noClip;
    public boolean allItems;
    public boolean goldEnabled;
    public int gold;
    public boolean expEnabled;
    public int exp;
    public long applyNonce;

    public static ModifierConfig load(Context context, String gameId, String path) {
        ModifierConfig config = new ModifierConfig();
        config.loadFromPrefs(context, gameId);
        File file = path == null || path.isEmpty() ? null : new File(path);
        if (file != null && file.isFile()) {
            config.loadFromFile(file);
        }
        return config;
    }

    public void save(Context context, String gameId, String path) {
        saveToPrefs(context, gameId);
        if (path != null && !path.isEmpty()) {
            writeToFile(new File(path));
        }
    }

    public void bumpApplyNonce() {
        applyNonce = Math.max(System.currentTimeMillis(), applyNonce + 1L);
    }

    public void writeToFile(File file) {
        if (file == null) {
            return;
        }
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String text = toPropertiesText();
        if (file.isFile() && text.equals(readText(file))) {
            return;
        }
        try (FileOutputStream output = new FileOutputStream(file, false)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    private static String readText(File file) {
        byte[] buffer = new byte[(int) Math.min(file.length(), 64 * 1024)];
        try (FileInputStream input = new FileInputStream(file)) {
            int read = input.read(buffer);
            if (read < 0) {
                return "";
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    public String toJsonString() {
        try {
            return new JSONObject()
                    .put("infiniteHp", infiniteHp)
                    .put("infiniteMp", infiniteMp)
                    .put("instantKill", instantKill)
                    .put("noClip", noClip)
                    .put("allItems", allItems)
                    .put("goldEnabled", goldEnabled)
                    .put("gold", gold)
                    .put("expEnabled", expEnabled)
                    .put("exp", exp)
                    .put("applyNonce", applyNonce)
                    .toString();
        } catch (JSONException ignored) {
            return "{}";
        }
    }

    private String toPropertiesText() {
        return "infiniteHp=" + bool(infiniteHp) + "\n"
                + "infiniteMp=" + bool(infiniteMp) + "\n"
                + "instantKill=" + bool(instantKill) + "\n"
                + "noClip=" + bool(noClip) + "\n"
                + "allItems=" + bool(allItems) + "\n"
                + "goldEnabled=" + bool(goldEnabled) + "\n"
                + "gold=" + clamp(gold, 0, 99999999) + "\n"
                + "expEnabled=" + bool(expEnabled) + "\n"
                + "exp=" + clamp(exp, 0, 99999999) + "\n"
                + "applyNonce=" + Math.max(0L, applyNonce) + "\n";
    }

    private void loadFromFile(File file) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
            return;
        }
        infiniteHp = bool(properties.getProperty("infiniteHp"), infiniteHp);
        infiniteMp = bool(properties.getProperty("infiniteMp"), infiniteMp);
        instantKill = bool(properties.getProperty("instantKill"), instantKill);
        noClip = bool(properties.getProperty("noClip"), noClip);
        allItems = bool(properties.getProperty("allItems"), allItems);
        goldEnabled = bool(properties.getProperty("goldEnabled"), goldEnabled);
        gold = intValue(properties.getProperty("gold"), gold);
        expEnabled = bool(properties.getProperty("expEnabled"), expEnabled);
        exp = intValue(properties.getProperty("exp"), exp);
        applyNonce = longValue(properties.getProperty("applyNonce"), applyNonce);
    }

    private void loadFromPrefs(Context context, String gameId) {
        if (context == null || gameId == null || gameId.isEmpty()) {
            return;
        }
        String prefix = gameId + ".";
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        infiniteHp = prefs.getBoolean(prefix + "infiniteHp", false);
        infiniteMp = prefs.getBoolean(prefix + "infiniteMp", false);
        instantKill = prefs.getBoolean(prefix + "instantKill", false);
        noClip = prefs.getBoolean(prefix + "noClip", false);
        allItems = prefs.getBoolean(prefix + "allItems", false);
        goldEnabled = prefs.getBoolean(prefix + "goldEnabled", false);
        gold = prefs.getInt(prefix + "gold", 999999);
        expEnabled = prefs.getBoolean(prefix + "expEnabled", false);
        exp = prefs.getInt(prefix + "exp", 999999);
        applyNonce = prefs.getLong(prefix + "applyNonce", 0L);
    }

    private void saveToPrefs(Context context, String gameId) {
        if (context == null || gameId == null || gameId.isEmpty()) {
            return;
        }
        String prefix = gameId + ".";
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(prefix + "infiniteHp", infiniteHp)
                .putBoolean(prefix + "infiniteMp", infiniteMp)
                .putBoolean(prefix + "instantKill", instantKill)
                .putBoolean(prefix + "noClip", noClip)
                .putBoolean(prefix + "allItems", allItems)
                .putBoolean(prefix + "goldEnabled", goldEnabled)
                .putInt(prefix + "gold", clamp(gold, 0, 99999999))
                .putBoolean(prefix + "expEnabled", expEnabled)
                .putInt(prefix + "exp", clamp(exp, 0, 99999999))
                .putLong(prefix + "applyNonce", Math.max(0L, applyNonce))
                .apply();
    }

    private static String bool(boolean value) {
        return value ? "1" : "0";
    }

    private static boolean bool(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(java.util.Locale.US);
        if ("1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized)) {
            return true;
        }
        if ("0".equals(normalized) || "false".equals(normalized) || "no".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static int intValue(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return clamp(Integer.parseInt(value.trim()), 0, 99999999);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long longValue(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Math.max(0L, Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final String PREFS = "mkxpz_modifier";
}
