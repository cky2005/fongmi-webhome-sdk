package com.github.catvod.spider;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 默认 no-op action handler。如果壳不实现 FmActionHandler，SDK 仍能跑：
 * - http 走 OkHttp (FmBridge 内置实现)
 * - 其它方法都是 no-op, 页面调 fm.openVod() 等会无效果
 * - cache 走 SharedPreferences
 */
public class DefaultFmActionHandler implements FmActionHandler {

    @Override
    public FmHttpResponse http(String url, String method, JSONObject headers, String body,
                               String responseType, int timeout, boolean includeCookie) {
        // FmBridge 实际处理 HTTP。这里返回 null 让 FmBridge 走自己的实现。
        return null;
    }

    @Override public void playUrl(String url, String title, JSONObject options) { }
    @Override public void playVod(String siteKey, String vodId, String title, String pic, JSONObject options) { }
    @Override public void playVodInline(JSONObject payload) { }
    @Override public void preloadArtwork(String pic, String wallPic) { }
    @Override public void controlPlayer(String action) { }
    @Override public JSONObject playerStatus() { return new JSONObject(); }

    @Override public void search(String keyword, JSONObject options) { }
    @Override public void openVod() { }
    @Override public void openLive() { }
    @Override public void openKeep() { }
    @Override public void openSetting() { }
    @Override public JSONObject history() { return new JSONObject(); }

    private static final Map<String, String> cacheStore = new ConcurrentHashMap<>();

    @Override
    public String cacheGet(String key, String rule) {
        return cacheStore.get(cacheKey(rule, key));
    }

    @Override
    public void cacheSet(String key, String value, String rule) {
        cacheStore.put(cacheKey(rule, key), value);
    }

    @Override
    public void cacheDel(String key, String rule) {
        cacheStore.remove(cacheKey(rule, key));
    }

    private static String cacheKey(String rule, String key) {
        return "cache_" + (rule == null || rule.isEmpty() ? "" : rule + "_") + key;
    }

    @Override public void setChrome(JSONObject options) { }
    @Override public void restoreChrome() { }
    @Override public void setToolbar(boolean visible) { }
    @Override public JSONObject getViewport() {
        JSONObject v = new JSONObject();
        try {
            v.put("width", 0);
            v.put("height", 0);
            v.put("chromeMode", "normal");
            v.put("systemBarsHidden", false);
        } catch (JSONException ignored) {}
        return v;
    }

    @Override
    public JSONObject deviceInfo() {
        JSONObject d = new JSONObject();
        try {
            d.put("uuid", "");
            d.put("name", android.os.Build.MODEL);
            d.put("ip", "http://127.0.0.1:9978");
            d.put("type", 1);
            d.put("time", System.currentTimeMillis());
        } catch (JSONException ignored) {}
        return d;
    }

    @Override
    public JSONObject siteInfo() {
        JSONObject s = new JSONObject();
        try {
            s.put("key", "webhome");
            s.put("name", "WebHome");
            s.put("homePage", "");
            s.put("type", 3);
            s.put("header", new JSONObject());
        } catch (JSONException ignored) {}
        return s;
    }

    @Override
    public JSONObject configInfo() {
        JSONObject c = new JSONObject();
        try {
            c.put("id", 0);
            c.put("url", "");
            c.put("desc", "WebHome");
            c.put("driveCheck", true);
        } catch (JSONException ignored) {}
        return c;
    }

    @Override
    public JSONObject extInfo() {
        JSONObject e = new JSONObject();
        try {
            e.put("siteKey", "webhome");
            e.put("siteName", "WebHome");
            e.put("homePage", "");
            e.put("enabled", true);
            e.put("matched", true);
            e.put("ready", true);
        } catch (JSONException ignored) {}
        return e;
    }

    @Override public void extLog(String message, String data) {
        android.util.Log.d("WebHomeExt", message + " " + data);
    }

    @Override public void extToast(String message) { }

    @Override
    public JSONObject panCheck(JSONObject payload) {
        JSONObject r = new JSONObject();
        try { r.put("results", new org.json.JSONArray()); } catch (JSONException ignored) {}
        return r;
    }

    @Override public void panPlay(JSONObject payload) { }
    @Override public void navigationBack() { }
    @Override public void navigationReload() { }
}
