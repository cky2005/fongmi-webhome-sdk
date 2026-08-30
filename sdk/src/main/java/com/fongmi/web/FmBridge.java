package com.fongmi.web;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FongMi WebHome SDK 的 JS 桥 — 这是注入到 WebView 的 {@code fongmiBridge} 对象。
 * <p>
 * 设计参考 webhtv 仓库的 HomeWebBridge.java，但是解耦了所有业务实现：
 * 业务通过 {@link FmActionHandler} 由壳注入。
 * <p>
 * 与 webhtv 兼容的接口:
 * <ul>
 *   <li>{@link #invoke} — 通用方法分发器</li>
 *   <li>{@link #console} — 调试 console</li>
 *   <li>{@link #network} — 调试 network</li>
 *   <li>{@link #resourceUrl} — 生成 webResource URL</li>
 *   <li>{@link #resultLength} / {@link #resultChunk} / {@link #clearResult} — 大结果分片</li>
 *   <li>{@link #inlineResult} — inline resolver 回调</li>
 * </ul>
 */
public class FmBridge {

    private static final int INLINE_LIMIT = 12_000;
    private static final int CHUNK_SIZE = 60_000;
    private static final long INLINE_TIMEOUT_SECONDS = 20;

    private final WebView webView;
    private final FmActionHandler handler;
    private final Map<String, String> results = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> inlineResults = new ConcurrentHashMap<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    public FmBridge(WebView webView, FmActionHandler handler) {
        this.webView = webView;
        this.handler = handler;
    }

    // ============== JS 接口 ==============

    /**
     * 通用方法分发器。JS SDK 通过这个入口调用所有 SDK 方法。
     * payload 是 JSON 字符串，requestId 是 SDK 自动生成的去重 ID。
     */
    @JavascriptInterface
    public void invoke(final String requestId, final String method, final String payload) {
        Thread t = new Thread(() -> {
            try {
                JSONObject p = parseObject(payload);
                String result = handle(method, p);
                resolve(requestId, result);
            } catch (Throwable e) {
                reject(requestId, e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }, "fongmi-webhome-invoke");
        t.setDaemon(true);
        t.start();
    }

    @JavascriptInterface
    public void console(String level, String message) {
        if (handler == null) return;
        try {
            handler.extLog("[console." + level + "]", message == null ? "" : message);
        } catch (Throwable ignored) {}
    }

    @JavascriptInterface
    public void network(String type, String method, String url, int status, long durationMs, String detail) {
        if (handler == null) return;
        try {
            handler.extLog("[net." + type + "]", method + " " + url + " " + status + " " + durationMs + "ms " + (detail == null ? "" : detail));
        } catch (Throwable ignored) {}
    }

    @JavascriptInterface
    public String resourceUrl(String url, String options) {
        try {
            JSONObject opt = parseObject(options);
            return WebResource.wrap(webView.getContext(), url, opt);
        } catch (Throwable t) {
            return WebResource.wrap(webView.getContext(), url, null);
        }
    }

    @JavascriptInterface
    public int resultLength(String id) {
        String r = results.get(id);
        return r == null ? 0 : r.length();
    }

    @JavascriptInterface
    public String resultChunk(String id, int start) {
        String r = results.get(id);
        if (r == null || start < 0 || start >= r.length()) return "";
        int end = Math.min(start + CHUNK_SIZE, r.length());
        return r.substring(start, end);
    }

    @JavascriptInterface
    public void clearResult(String id) {
        results.remove(id);
    }

    @JavascriptInterface
    public void inlineResult(String id, String payload) {
        CompletableFuture<String> f = inlineResults.remove(id);
        if (f != null) f.complete(payload);
    }

    // ============== 内部 dispatch ==============

    private String handle(String method, JSONObject payload) {
        if (handler == null) return "{}";
        switch (method) {
            // net
            case "net.request":         return handleNetRequest(payload);
            case "net.resourceUrl":     return quote(resourceUrl(payload.optString("url"), payload.toString()));

            // player
            case "player.playUrl":      handler.playUrl(payload.optString("url"), payload.optString("title"), payload); return "{}";
            case "player.playVod":      handler.playVod(payload.optString("siteKey"), payload.optString("vodId"),
                    payload.optString("title"), payload.optString("pic"), payload); return "{}";
            case "player.playVodInline": handler.playVodInline(payload); return "{}";
            case "player.preloadArtwork": handler.preloadArtwork(payload.optString("pic"), payload.optString("wallPic")); return "{}";
            case "player.control":      handler.controlPlayer(payload.optString("action")); return "{}";
            case "player.status":       return handler.playerStatus().toString();

            // app
            case "app.search":          handler.search(payload.optString("keyword"), payload); return "{}";
            case "app.openVod":         handler.openVod(); return "{}";
            case "app.openLive":        handler.openLive(); return "{}";
            case "app.openKeep":        handler.openKeep(); return "{}";
            case "app.openSetting":     handler.openSetting(); return "{}";
            case "app.history":         return handler.history().toString();

            // cache
            case "cache.get":           return quote(handler.cacheGet(payload.optString("key"), payload.optString("rule")));
            case "cache.set":           handler.cacheSet(payload.optString("key"), payload.optString("value"), payload.optString("rule")); return "{}";
            case "cache.del":           handler.cacheDel(payload.optString("key"), payload.optString("rule")); return "{}";

            // ui
            case "ui.setToolbar":       handler.setToolbar(!payload.has("visible") || payload.optBoolean("visible")); return "{}";
            case "ui.setChrome":        handler.setChrome(payload); return "{}";
            case "ui.restoreChrome":    handler.restoreChrome(); return "{}";
            case "ui.getViewport":      return handler.getViewport().toString();

            // device/site/config
            case "device.info":         return handler.deviceInfo().toString();
            case "site.info":           return handler.siteInfo().toString();
            case "config.info":         return handler.configInfo().toString();

            // ext
            case "ext.info":            return handler.extInfo().toString();
            case "ext.log":             handler.extLog(payload.optString("message"), payload.optString("data")); return "{}";
            case "ext.toast":           handler.extToast(payload.optString("message")); return "{}";

            // pan
            case "pan.check":           return handler.panCheck(payload).toString();
            case "pan.play":            handler.panPlay(payload); return "{}";

            // nav
            case "navigation.back":     handler.navigationBack(); return "{}";
            case "navigation.reload":   handler.navigationReload(); return "{}";

            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    private String handleNetRequest(JSONObject payload) {
        String url = payload.optString("url");
        String method = (payload.optString("method", "GET")).toUpperCase(Locale.ROOT);
        JSONObject headers = payload.optJSONObject("headers");
        String body = payload.optString("body");
        String responseType = payload.optString("responseType", "text");
        int timeout = payload.optInt("timeout", 30);
        boolean includeCookie = "include".equalsIgnoreCase(payload.optString("credentials"));

        FmHttpResponse resp = handler.http(url, method, headers, body, responseType, timeout, includeCookie);
        JSONObject out = new JSONObject();
        try {
            out.put("ok", resp.ok());
            out.put("status", resp.status);
            out.put("url", resp.url);
            if ("base64".equalsIgnoreCase(responseType)) {
                out.put("body", resp.base64);
            } else {
                String text = resp.text == null ? "" : resp.text;
                out.put("body", text);
            }
            if (resp.error != null) out.put("error", resp.error);
        } catch (JSONException ignored) {}
        return out.toString();
    }

    // ============== resolve / reject ==============

    private void resolve(String requestId, String value) {
        if (value == null) value = "{}";
        // 大结果分片
        if (value.length() > INLINE_LIMIT) {
            String resultId = "r_" + UUID.randomUUID().toString().replace("-", "");
            results.put(resultId, value);
            // 返回壳体: { __fmResultId: resultId } — JS SDK hydrate() 会自动拉分片
            value = "{\"__fmResultId\":\"" + resultId + "\"}";
        }
        final String inject = "window.fongmiNative && window.fongmiNative.resolve("
                + quote(requestId) + "," + value + ");";
        runOnUi(() -> evaluateJs(inject));
    }

    private void reject(String requestId, String error) {
        String safe = error == null ? "" : error.replace("'", "\\'").replace("\n", " ");
        final String inject = "window.fongmiNative && window.fongmiNative.reject("
                + quote(requestId) + ",'" + safe + "');";
        runOnUi(() -> evaluateJs(inject));
    }

    private void runOnUi(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else main.post(r);
    }

    private void evaluateJs(String js) {
        try {
            webView.evaluateJavascript(js, null);
        } catch (Throwable t) {
            android.util.Log.e("FmBridge", "evaluateJavascript failed", t);
        }
    }

    // ============== inline resolver ==============

    /**
     * 触发 inline 集数解析。
     * 由 {@link FmController#resolveInline(JSONObject)} 调用，结果通过 {@link #inlineResult} 回灌。
     */
    public CompletableFuture<JSONObject> resolveInline(JSONObject episode) {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        String id = "inline_" + UUID.randomUUID().toString().replace("-", "");
        inlineResults.put(id, future);

        // 先问壳：壳可自己返回 null 走默认 JS resolver
        try {
            JSONObject fromHandler = handler.resolveInlineEpisode(episode);
            if (fromHandler != null) {
                inlineResults.remove(id);
                future.complete(fromHandler);
                return future;
            }
        } catch (Throwable t) {
            inlineResults.remove(id);
            future.completeExceptionally(t);
            return future;
        }

        // 默认: 在 WebView 内执行 window.__fmWebHomeInlineResolver(episode)
        String episodeJson = episode.toString().replace("\\", "\\\\").replace("'", "\\'");
        String script = "(function(){"
                + "var ep=" + episodeJson + ";"
                + "var r=window.__fmWebHomeInlineResolver||window.__fmYmvidResolveEpisode;"
                + "if(typeof r!=='function'){"
                + "  window.fongmiBridge.inlineResult('" + id + "',JSON.stringify({error:'no inline resolver'}));"
                + "  return;"
                + "}"
                + "Promise.resolve().then(function(){return r(ep);}).then(function(v){"
                + "  window.fongmiBridge.inlineResult('" + id + "',JSON.stringify(v||{}));"
                + "},function(e){"
                + "  window.fongmiBridge.inlineResult('" + id + "',JSON.stringify({error:(e&&e.message)||String(e)}));"
                + "});"
                + "})();";

        runOnUi(() -> evaluateJs(script));

        // 超时
        main.postDelayed(() -> {
            CompletableFuture<JSONObject> f = inlineResults.remove(id);
            if (f != null && !f.isDone()) {
                f.completeExceptionally(new RuntimeException("inline resolve timeout"));
            }
        }, INLINE_TIMEOUT_SECONDS * 1000);

        return future;
    }

    // ============== util ==============

    private static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static JSONObject parseObject(String s) {
        if (TextUtils.isEmpty(s)) return new JSONObject();
        try { return new JSONObject(s); }
        catch (JSONException e) { return new JSONObject(); }
    }
}
