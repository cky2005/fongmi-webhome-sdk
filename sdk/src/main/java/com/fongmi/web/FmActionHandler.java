package com.fongmi.web;

import org.json.JSONObject;

/**
 * FongMi WebHome SDK 业务动作 SPI。
 * <p>
 * 壳实现本接口后通过 {@link FmBridge#setHandler(FmActionHandler)} 注入。
 * 默认实现见 {@link DefaultFmActionHandler}，壳可继承后只覆盖关心的方法。
 * <p>
 * 所有方法在 WebView 触发的 JS 调用线程上执行（一般是 WebView 工作线程），
 * 实现方负责自行 post 到主线程。
 */
public interface FmActionHandler {

    // ============== 网络 ==============

    /**
     * fm.req 调用的同步 HTTP 请求。
     *
     * @param url      目标 URL
     * @param method   GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS
     * @param headers  请求头（已 merge 站点 header、默认 UA、Cookie 等）
     * @param body     请求体（已 stringify）
     * @param responseType text/json/base64
     * @param timeout  超时秒
     * @param includeCookie 是否强制带 CookieManager 里的 Cookie
     * @return 见 FmHttpResponse
     */
    FmHttpResponse http(String url, String method, JSONObject headers, String body,
                        String responseType, int timeout, boolean includeCookie);

    // ============== 播放 ==============

    /**
     * 播放直链 m3u8/mp4 等。
     * 默认实现: 把 url 透传成 playerContent 的 id，由壳的播放器链路自动接管。
     */
    void playUrl(String url, String title, JSONObject options);

    /**
     * 播放指定 siteKey/vodId 的 CSP 影片。
     */
    void playVod(String siteKey, String vodId, String title, String pic, JSONObject options);

    /**
     * 播放 inline 多集，payload 含 episodes 数组。
     * 高级用法：注册 inline resolver（window.__fmWebHomeInlineResolver）后,
     * 壳的播放页在点集时通过 resolveInlineEpisode 回调拉真实媒体 URL。
     */
    void playVodInline(JSONObject payload);

    /**
     * 预热海报和横屏剧照。
     */
    void preloadArtwork(String pic, String wallPic);

    /**
     * 播放控制: play/pause/stop/prev/next/loop/replay。
     */
    void controlPlayer(String action);

    /**
     * 取当前播放状态。
     * 至少返回: { state, speed, duration, position, url, title }
     */
    JSONObject playerStatus();

    // ============== App 入口 ==============

    void search(String keyword, JSONObject options);

    void openVod();

    void openLive();

    void openKeep();

    void openSetting();

    /** 60 天内的最近观看。 */
    JSONObject history();

    // ============== 缓存 (Native Prefers) ==============

    String cacheGet(String key, String rule);

    void cacheSet(String key, String value, String rule);

    void cacheDel(String key, String rule);

    // ============== UI / Chrome ==============

    void setChrome(JSONObject options);

    void restoreChrome();

    void setToolbar(boolean visible);

    JSONObject getViewport();

    // ============== Device / Site / Config ==============

    JSONObject deviceInfo();

    JSONObject siteInfo();

    JSONObject configInfo();

    // ============== 扩展 ==============

    JSONObject extInfo();

    void extLog(String message, String data);

    void extToast(String message);

    // ============== 网盘 ==============

    /** 网盘检测，items: [{type,url,password}]. 返回: { results: [...] } */
    JSONObject panCheck(JSONObject payload);

    /** 网盘/推送播放。 */
    void panPlay(JSONObject payload);

    // ============== 导航 ==============

    void navigationBack();

    void navigationReload();

    // ============== inline resolver ==============

    /**
     * 壳侧可选：触发 inline resolver 求值。
     * 默认实现会由 SDK 在 WebView 内执行 window.__fmWebHomeInlineResolver(episode)。
     * 壳若已接管 inline 流程，可直接返回 null 跳过。
     */
    JSONObject resolveInlineEpisode(JSONObject episode);
}
