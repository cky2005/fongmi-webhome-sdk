package com.fongmi.web;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.CookieManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class DefaultFmActionHandler implements FmActionHandler {

    private static final String DEFAULT_UA =
            "Mozilla/5.0 (Linux; Android 12; fongmi-webhome) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36";

    private static final int READ_TIMEOUT_MS = 30000;
    private static final int CONNECT_TIMEOUT_MS = 15000;

    protected final Context appContext;
    protected final PlayerGateway player;

    public DefaultFmActionHandler(Context context) {
        this(context, null);
    }

    public DefaultFmActionHandler(Context context, PlayerGateway player) {
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.player = player;
    }

    @Override
    public FmHttpResponse http(String url, String method, JSONObject headers, String body,
                               String responseType, int timeout, boolean includeCookie) {
        if (TextUtils.isEmpty(url)) return FmHttpResponse.failure("empty url");
        int to = timeout > 0 ? timeout * 1000 : READ_TIMEOUT_MS;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(to);
            conn.setReadTimeout(to);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("User-Agent", DEFAULT_UA);
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            conn.setRequestProperty("Connection", "keep-alive");

            if (headers != null) {
                Iterator<String> it = headers.keys();
                while (it.hasNext()) {
                    String k = it.next();
                    String lk = k.toLowerCase();
                    if (lk.equals("host") || lk.equals("content-length")
                            || lk.equals("connection") || lk.equals("accept-encoding")) continue;
                    conn.setRequestProperty(k, headers.optString(k));
                }
            }

            if (includeCookie && appContext != null) {
                String cookie = CookieManager.getInstance().getCookie(url);
                if (!TextUtils.isEmpty(cookie)) conn.setRequestProperty("Cookie", cookie);
            }

            String m = (method == null || method.isEmpty()) ? "GET" : method.toUpperCase();
            conn.setRequestMethod(m);
            if (!"GET".equals(m) && !"HEAD".equals(m) && body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes("UTF-8"));
                os.flush();
                os.close();
            }

            int code = conn.getResponseCode();
            String finalUrl = conn.getURL().toString();
            String encoding = conn.getContentEncoding();
            InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] raw = readAll(is, encoding);

            if ("base64".equalsIgnoreCase(responseType)) {
                return FmHttpResponse.successBytes(code, finalUrl, Base64.encodeToString(raw, Base64.NO_WRAP));
            }
            return FmHttpResponse.success(code, finalUrl, new String(raw, "UTF-8"));
        } catch (Throwable t) {
            return FmHttpResponse.failure(t.getClass().getSimpleName() + ": " + t.getMessage());
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Throwable ignored) {}
            }
        }
    }

    private static byte[] readAll(InputStream in, String encoding) throws IOException {
        if (in == null) return new byte[0];
        try {
            if ("gzip".equalsIgnoreCase(encoding)) in = new GZIPInputStream(in);
            else if ("deflate".equalsIgnoreCase(encoding)) in = new InflaterInputStream(in);
        } catch (IOException ignored) {}
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    @Override
    public void playUrl(String url, String title, JSONObject options) {
        if (player != null) player.playUrl(url, title, options);
    }

    @Override
    public void playVod(String siteKey, String vodId, String title, String pic, JSONObject options) {
        if (player != null) player.playVod(siteKey, vodId, title, pic, options);
    }

    @Override
    public void playVodInline(JSONObject payload) {
        if (player != null) player.playVodInline(payload);
    }

    @Override
    public void preloadArtwork(String pic, String wallPic) {
        if (player != null) player.preloadArtwork(pic, wallPic);
    }

    @Override
    public void controlPlayer(String action) {
        if (player != null) player.control(action);
    }

    @Override
    public JSONObject playerStatus() {
        if (player != null) {
            JSONObject r = player.status();
            if (r != null) return r;
        }
        return new JSONObject();
    }

    @Override public void search(String keyword, JSONObject options) { }
    @Override public void openVod() { }
    @Override public void openLive() { }
    @Override public void openKeep() { }
    @Override public void openSetting() { }

    @Override
    public JSONObject history() {
        return new JSONObject();
    }

    private android.content.SharedPreferences prefs() {
        return appContext.getSharedPreferences("fongmi_webhome_cache", Context.MODE_PRIVATE);
    }

    private String cacheKey(String rule, String key) {
        return "cache_" + (rule == null || rule.isEmpty() ? "" : rule + "_") + key;
    }

    @Override
    public String cacheGet(String key, String rule) {
        return prefs().getString(cacheKey(rule, key), "");
    }

    @Override
    public void cacheSet(String key, String value, String rule) {
        prefs().edit().putString(cacheKey(rule, key), value).apply();
    }

    @Override
    public void cacheDel(String key, String rule) {
        prefs().edit().remove(cacheKey(rule, key)).apply();
    }

    @Override public void setChrome(JSONObject options) { }
    @Override public void restoreChrome() { }
    @Override public void setToolbar(boolean visible) { }

    @Override
    public JSONObject getViewport() {
        JSONObject v = new JSONObject();
        try {
            v.put("width", 0);
            v.put("height", 0);
            v.put("safeTop", 0);
            v.put("safeRight", 0);
            v.put("safeBottom", 0);
            v.put("safeLeft", 0);
            v.put("gestureLeft", 0);
            v.put("gestureRight", 0);
            v.put("gestureBottom", 0);
            v.put("statusBarHeight", 0);
            v.put("navigationBarHeight", 0);
            v.put("keyboardBottom", 0);
            v.put("chromeMode", "normal");
            v.put("systemBarsHidden", false);
        } catch (JSONException ignored) {}
        return v;
    }

    @Override
    public JSONObject deviceInfo() {
        JSONObject d = new JSONObject();
        try {
            d.put("uuid", android.provider.Settings.Secure.getString(
                    appContext.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID));
            d.put("name", android.os.Build.MODEL);
            d.put("ip", "http://127.0.0.1:" + WebResource.portOrDefault(appContext));
            d.put("type", isTv() ? 0 : 1);
            d.put("time", System.currentTimeMillis());
            d.put("serial", "");
            d.put("eth", "");
            d.put("wlan", "");
        } catch (Throwable t) {}
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
            c.put("url", "webhome");
            c.put("desc", "FongMi WebHome SDK");
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

    @Override
    public void extLog(String message, String data) {
        android.util.Log.d("WebHomeSDK", message + " " + data);
    }

    @Override
    public void extToast(String message) { }

    @Override
    public JSONObject panCheck(JSONObject payload) {
        JSONObject r = new JSONObject();
        try { r.put("results", new org.json.JSONArray()); } catch (JSONException ignored) {}
        return r;
    }

    @Override
    public void panPlay(JSONObject payload) {
        if (payload == null) return;
        String url = payload.optString("url");
        String title = payload.optString("title", url);
        if (!TextUtils.isEmpty(url)) playUrl(url, title, null);
    }

    @Override public void navigationBack() { }
    @Override public void navigationReload() { }

    @Override
    public JSONObject resolveInlineEpisode(JSONObject episode) {
        return null;
    }

    private boolean isTv() {
        if (appContext == null) return false;
        try {
            return appContext.getPackageManager().hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_LEANBACK);
        } catch (Throwable t) {
            return false;
        }
    }
}
