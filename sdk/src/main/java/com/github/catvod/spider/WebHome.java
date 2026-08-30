package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.fongmi.web.FmActionHandler;
import com.fongmi.web.FmController;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

/**
 * WebHome Spider — fongmi/catvod 系影视壳的接入点。
 *
 * <p>接入方式: 在 jar 站点配置里写
 * <pre>
 * {
 *   "key": "webhome",
 *   "name": "WebHome 演示",
 *   "type": 3,
 *   "api": "csp_WebHome",
 *   "homePage": "https://example.com/your-home.html"
 * }
 * </pre>
 *
 * <p>壳启动该 Spider 后，{@link #homeContent(boolean)} / {@link #detailContent(List)}
 * 会打开全屏 WebView 加载 homePage，并通过注入的 fmsdk.js 让 HTML 能调用
 * window.fm / window.fongmi 的所有 SDK 能力。
 *
 * <p>壳可在自己的 init 之前调 {@link #setHandler(FmActionHandler)} 注入自定义业务实现，
 * 否则用默认实现（HTTP 走 HttpURLConnection，缓存走 SharedPreferences）。
 */
public class WebHome extends Spider {

    private static volatile FmActionHandler globalHandler;
    private static volatile WeakReference<Context> globalContext;

    private String extend = "";

    public WebHome() {}

    @Override
    public void init(Context context, String extend) {
        this.extend = extend == null ? "" : extend.trim();
        if (context != null) globalContext = new WeakReference<>(context.getApplicationContext());
    }

    @Override
    public String homeContent(boolean filter) {
        show();
        return "{\"class\":[],\"list\":[]}";
    }

    @Override
    public String homeVideoContent() {
        return "{\"list\":[]}";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> ext) {
        return "{\"list\":[]}";
    }

    @Override
    public String detailContent(List<String> ids) {
        show();
        return "{\"list\":[]}";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return "{\"list\":[]}";
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        try {
            String url = id == null ? "" : id;
            int at = url.indexOf("@@");
            if (at > 0) url = url.substring(0, at);
            org.json.JSONObject r = new org.json.JSONObject();
            r.put("parse", 0);
            r.put("playUrl", "");
            r.put("url", url);
            r.put("jx", "");
            return r.toString();
        } catch (Throwable e) {
            return "{\"parse\":0,\"url\":\"\"}";
        }
    }

    @Override
    public void destroy() {
        FmController.get().close();
    }

    private void show() {
        Context ctx = globalContext != null ? globalContext.get() : null;
        if (ctx == null) return;
        FmActionHandler h = globalHandler;
        FmController.get().show(ctx, this.extend, "webhome", "WebHome", null, h);
    }

    /** 壳在 init 自己 Spider 之前调用，注入业务实现。 */
    public static void setHandler(FmActionHandler h) {
        globalHandler = h;
    }

    /** 壳传入自己的 context（部分 fongmi 壳 init 不提供）。 */
    public static void setContext(Context ctx) {
        if (ctx != null) globalContext = new WeakReference<>(ctx.getApplicationContext());
    }
}
