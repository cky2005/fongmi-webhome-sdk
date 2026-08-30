package com.fongmi.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FmController {

    private static final String TAG = "FmController";
    private static final long LOAD_TIMEOUT_MS = 15000;
    private static final String SDK_ASSET = "fmsdk.js";

    private static volatile FmController INSTANCE;
    private static volatile boolean lifecycleInstalled;
    private static volatile WeakReference<Activity> foreground = new WeakReference<>(null);
    private static final Object LOCK = new Object();

    private final Handler main = new Handler(Looper.getMainLooper());
    private Overlay overlay;
    private String siteKey;
    private String siteName;
    private String chromeMode = WebHomeChrome.NORMAL;
    private int retryCount = 0;

    private FmController() {}

    public static FmController get() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) INSTANCE = new FmController();
            }
        }
        return INSTANCE;
    }

    public static void installLifecycle(Application app) {
        if (lifecycleInstalled || app == null) return;
        synchronized (LOCK) {
            if (lifecycleInstalled) return;
            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(Activity a, Bundle b) { remember(a); }
                @Override public void onActivityStarted(Activity a) { remember(a); }
                @Override public void onActivityResumed(Activity a) { remember(a); }
                @Override public void onActivityPaused(Activity a) { }
                @Override public void onActivityStopped(Activity a) { }
                @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
                @Override public void onActivityDestroyed(Activity a) {
                    if (foreground.get() == a) foreground = new WeakReference<>(null);
                }
            });
            lifecycleInstalled = true;
        }
    }

    private static void remember(Activity a) {
        if (a != null && !a.isFinishing() && !a.isDestroyed()) {
            foreground = new WeakReference<>(a);
        }
    }

    private static Activity currentActivity() {
        Activity a = foreground.get();
        if (a != null && !a.isFinishing() && !a.isDestroyed()) return a;
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            Field f = at.getDeclaredField("mActivities");
            f.setAccessible(true);
            Object obj = f.get(thread);
            if (obj instanceof Map) {
                for (Object r : ((Map<?, ?>) obj).values()) {
                    if (r == null) continue;
                    Field pf = r.getClass().getDeclaredField("paused");
                    pf.setAccessible(true);
                    if (Boolean.TRUE.equals(pf.get(r))) continue;
                    Field af = r.getClass().getDeclaredField("activity");
                    af.setAccessible(true);
                    Object act = af.get(r);
                    if (act instanceof Activity) {
                        Activity act2 = (Activity) act;
                        if (!act2.isFinishing() && !act2.isDestroyed()) return act2;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public void show(Context ctx, String url, String siteKey, String siteName,
                     Map<String, String> siteHeader, FmActionHandler handler) {
        if (ctx == null || TextUtils.isEmpty(url)) return;
        Context appCtx = ctx.getApplicationContext();
        if (appCtx instanceof Application) installLifecycle((Application) appCtx);

        FmActionHandler actualHandler = handler != null ? handler : new DefaultFmActionHandler(ctx);

        // 如果已经在主线程，直接显示（同步）
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Activity act = currentActivity();
            if (act == null) {
                // 等待 Activity 出现
                if (retryCount++ < 18) {
                    main.postDelayed(() -> show(ctx, url, siteKey, siteName, siteHeader, actualHandler), 180);
                } else {
                    retryCount = 0;
                    Log.w(TAG, "no foreground activity, give up showing " + url);
                }
                return;
            }
            retryCount = 0;
            this.siteKey = siteKey;
            this.siteName = siteName;
            if (overlay != null && overlay.isShowing()) overlay.dismiss();
            overlay = new Overlay(act, url, siteHeader, actualHandler);
            overlay.show();
            return;
        }

        // 其他线程，post 到主线程
        main.post(() -> {
            Activity act = currentActivity();
            if (act == null) {
                if (retryCount++ < 18) {
                    main.postDelayed(() -> show(ctx, url, siteKey, siteName, siteHeader, actualHandler), 180);
                } else {
                    retryCount = 0;
                    Log.w(TAG, "no foreground activity, give up showing " + url);
                }
                return;
            }
            retryCount = 0;
            this.siteKey = siteKey;
            this.siteName = siteName;

            if (overlay != null && overlay.isShowing()) overlay.dismiss();
            overlay = new Overlay(act, url, siteHeader, actualHandler);
            overlay.show();
        });
    }

    /**
     * 同步显示 WebView，阻塞调用线程直到 WebView 关闭。
     * 用于在 Spider 的 homeContent() 等回调里打开 WebView。
     */
    public void showSync(Context ctx, String url, String siteKey, String siteName,
                          Map<String, String> siteHeader, FmActionHandler handler) {
        if (ctx == null || TextUtils.isEmpty(url)) return;
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // 不在主线程，转到主线程 showSync 阻塞
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean[] done = {false};
            main.post(() -> {
                showSyncOnMain(ctx, url, siteKey, siteName, siteHeader, handler, latch);
                done[0] = true;
            });
            try {
                // 最多等 5 分钟（用户可能长时间看 HTML）
                latch.await(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        // 已经在主线程
        showSyncOnMain(ctx, url, siteKey, siteName, siteHeader, handler, null);
    }

    private void showSyncOnMain(Context ctx, String url, String siteKey, String siteName,
                                 Map<String, String> siteHeader, FmActionHandler handler,
                                 CountDownLatch latch) {
        FmActionHandler actualHandler = handler != null ? handler : new DefaultFmActionHandler(ctx);
        Activity act = currentActivity();
        if (act == null) {
            Log.w(TAG, "no foreground activity for showSync");
            if (latch != null) latch.countDown();
            return;
        }
        this.siteKey = siteKey;
        this.siteName = siteName;
        if (overlay != null && overlay.isShowing()) overlay.dismiss();
        overlay = new Overlay(act, url, siteHeader, actualHandler, latch);
        overlay.show();
    }

    public void close() {
        main.post(() -> {
            if (overlay != null) {
                overlay.dismiss();
                overlay = null;
            }
        });
    }

    public boolean isShowing() {
        return overlay != null && overlay.isShowing();
    }

    public void setChromeMode(String mode) {
        this.chromeMode = WebHomeChrome.isValid(mode) ? mode : WebHomeChrome.NORMAL;
    }

    private static final class Overlay extends android.app.Dialog {
        private final Activity host;
        private final String url;
        private final Map<String, String> siteHeader;
        private final FmActionHandler handler;
        private final CountDownLatch closeLatch;
        private WebView webView;
        private FmBridge bridge;
        private String lastPageUrl;
        private int loadToken;

        @SuppressLint("ClickableViewAccessibility")
        Overlay(Activity activity, String url, Map<String, String> siteHeader, FmActionHandler handler) {
            this(activity, url, siteHeader, handler, null);
        }

        @SuppressLint("ClickableViewAccessibility")
        Overlay(Activity activity, String url, Map<String, String> siteHeader, FmActionHandler handler, CountDownLatch latch) {
            super(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            this.host = activity;
            this.url = url;
            this.siteHeader = siteHeader;
            this.handler = handler;
            this.closeLatch = latch;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);

            FrameLayout root = new FrameLayout(getContext());
            root.setBackgroundColor(0xFF000000);
            webView = new WebView(getContext());
            root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
            setContentView(root);

            Window w = getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0xFF000000));
                w.setLayout(-1, -1);
            }

            setupWebView(webView);

            setOnKeyListener((d, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    if (webView.canGoBack()) {
                        String current = webView.getUrl();
                        if (current != null && current.equals(lastPageUrl)) {
                            dismiss();
                        } else {
                            webView.goBack();
                        }
                    } else {
                        dismiss();
                    }
                    return true;
                }
                return false;
            });

            try {
                webView.loadUrl(url);
            } catch (Throwable t) {
                webView.loadDataWithBaseURL(null,
                        "<html><body style='background:#000;color:#fff;font-family:sans-serif;padding:24px;'>"
                                + "<h1>WebHome 加载失败</h1><pre>" + t.getMessage() + "</pre></body></html>",
                        "text/html", "UTF-8", null);
            }

            webView.postDelayed(() -> {
                if (!isShowing() || webView == null) return;
                String current = webView.getUrl();
                if (TextUtils.isEmpty(current) || "about:blank".equals(current)) {
                    Log.w(TAG, "load timeout, recreating webview");
                    recreateWebView(url);
                }
            }, LOAD_TIMEOUT_MS);
        }

        @SuppressLint({ "SetJavaScriptEnabled", "AddJavascriptInterface" })
        private void setupWebView(WebView v) {
            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            WebSettings s = v.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            s.setUseWideViewPort(true);
            s.setLoadWithOverviewMode(true);
            s.setCacheMode(WebSettings.LOAD_DEFAULT);
            s.setAllowFileAccess(true);
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
            s.setBuiltInZoomControls(true);
            s.setDisplayZoomControls(false);
            s.setJavaScriptCanOpenWindowsAutomatically(true);

            if (Build.VERSION.SDK_INT >= 26) {
                v.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
            }

            v.setBackgroundColor(0xFF000000);
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);

            try {
                CookieManager cm = CookieManager.getInstance();
                cm.setAcceptCookie(true);
                cm.setAcceptThirdPartyCookies(v, true);
                if (Build.VERSION.SDK_INT >= 24) cm.setAcceptFileSchemeCookies(true);
            } catch (Throwable ignored) {}

            if (siteHeader != null) {
                String cookie = siteHeader.get("Cookie");
                if (cookie != null) CookieManager.getInstance().setCookie(url, cookie);
            }

            bridge = new FmBridge(v, handler);
            v.addJavascriptInterface(bridge, "fongmiBridge");

            v.setWebChromeClient(new WebChromeClient());
            v.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return url == null || url.length() == 0
                            || !(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://"));
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return shouldOverrideUrlLoading(view, request.getUrl().toString());
                }

                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    lastPageUrl = url;
                    int token = ++loadToken;
                    view.postDelayed(() -> {
                        if (token == loadToken && isShowing()) {
                            String cur = view.getUrl();
                            if (cur == null || "about:blank".equals(cur)) {
                                Log.w(TAG, "load timeout per-token, recreating");
                                recreateWebView(url);
                            }
                        }
                    }, LOAD_TIMEOUT_MS);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    lastPageUrl = url;
                    try { CookieManager.getInstance().flush(); } catch (Throwable ignored) {}
                }
            });

            v.post(() -> injectSdk(v));
        }

        private void injectSdk(WebView v) {
            String js = loadAssetJs(getContext());
            if (js == null) {
                Log.e(TAG, "fmsdk.js not found in assets");
                return;
            }
            StringBuilder wrapped = new StringBuilder("(function(){try{\n");
            wrapped.append(js);
            wrapped.append("\n}catch(e){console.error('fmsdk inject failed',e);}})();");
            try {
                v.evaluateJavascript(wrapped.toString(), value ->
                        v.evaluateJavascript("window.dispatchEvent && window.dispatchEvent(new CustomEvent('fmsdk'));", null));
            } catch (Throwable t) {
                Log.e(TAG, "injectSdk failed", t);
            }
        }

        private String loadAssetJs(Context ctx) {
            try (InputStream is = ctx.getAssets().open(SDK_ASSET);
                 BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (IOException e) {
                return null;
            }
        }

        private void recreateWebView(String url) {
            android.view.ViewGroup parent = (android.view.ViewGroup) webView.getParent();
            if (parent == null) return;
            int idx = parent.indexOfChild(webView);
            android.view.ViewGroup.LayoutParams lp = webView.getLayoutParams();
            try {
                webView.stopLoading();
                webView.destroy();
            } catch (Throwable ignored) {}
            parent.removeView(webView);

            webView = new WebView(getContext());
            webView.setLayoutParams(lp);
            parent.addView(webView, idx);
            setupWebView(webView);
            webView.loadUrl(url);
        }

        @Override
        public void dismiss() {
            try { CookieManager.getInstance().flush(); } catch (Throwable ignored) {}
            if (webView != null) {
                try {
                    webView.stopLoading();
                    webView.pauseTimers();
                    webView.loadUrl("about:blank");
                    webView.removeAllViews();
                    webView.destroy();
                } catch (Throwable ignored) {}
                webView = null;
            }
            bridge = null;
            super.dismiss();
            if (closeLatch != null) closeLatch.countDown();
        }

        @Override
        public void onStop() {
            super.onStop();
            if (webView != null) webView.pauseTimers();
        }

        @Override
        public void onStart() {
            super.onStart();
            if (webView != null) webView.resumeTimers();
        }
    }
}
