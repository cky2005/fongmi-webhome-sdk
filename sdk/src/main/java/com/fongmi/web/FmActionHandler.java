package com.fongmi.web;

import org.json.JSONObject;

/**
 * FongMi WebHome SDK 业务动作 SPI。
 * 壳实现本接口后通过 {@link FmBridge#setHandler(FmActionHandler)} 注入。
 * 默认实现见 {@link DefaultFmActionHandler}，壳可继承后只覆盖关心的方法。
 */
public interface FmActionHandler {

    FmHttpResponse http(String url, String method, JSONObject headers, String body,
                        String responseType, int timeout, boolean includeCookie);

    void playUrl(String url, String title, JSONObject options);

    void playVod(String siteKey, String vodId, String title, String pic, JSONObject options);

    void playVodInline(JSONObject payload);

    void preloadArtwork(String pic, String wallPic);

    void controlPlayer(String action);

    JSONObject playerStatus();

    void search(String keyword, JSONObject options);

    void openVod();

    void openLive();

    void openKeep();

    void openSetting();

    JSONObject history();

    String cacheGet(String key, String rule);

    void cacheSet(String key, String value, String rule);

    void cacheDel(String key, String rule);

    void setChrome(JSONObject options);

    void restoreChrome();

    void setToolbar(boolean visible);

    JSONObject getViewport();

    JSONObject deviceInfo();

    JSONObject siteInfo();

    JSONObject configInfo();

    JSONObject extInfo();

    void extLog(String message, String data);

    void extToast(String message);

    JSONObject panCheck(JSONObject payload);

    void panPlay(JSONObject payload);

    void navigationBack();

    void navigationReload();

    /** 默认由 SDK 在 WebView 内执行 window.__fmWebHomeInlineResolver(episode) */
    JSONObject resolveInlineEpisode(JSONObject episode);
}
