package com.fongmi.web;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONObject;

/**
 * 播放器网关 — 壳实现本接口后注入到 {@link DefaultFmActionHandler}，让 SDK 调用真实播放器。
 * <p>
 * fongmi/fengmi 影视壳的典型实现:
 * <pre>{@code
 * public class FongmiPlayerGateway implements PlayerGateway {
 *     public void playUrl(String url, String title, JSONObject options) {
 *         // 方式 A: 启动 push Agent Activity
 *         Intent it = new Intent(Intent.ACTION_VIEW);
 *         it.setDataAndType(Uri.parse(url), "video/*");
 *         it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 *         it.putExtra("title", title);
 *         appContext.startActivity(it);
 *
 *         // 方式 B: 直接构造 Spider.playerContent 返回 JSON,让壳走完整解析
 *         // 详见 fongmi 壳的 push agent 链路
 *     }
 *
 *     public void playVod(String siteKey, String vodId, String title, String pic, JSONObject options) {
 *         // 调壳的 VideoActivity
 *         Intent it = new Intent();
 *         it.setClassName(appContext, "com.fongmi.android.tv.ui.activity.VideoActivity");
 *         it.putExtra("siteKey", siteKey);
 *         it.putExtra("vodId", vodId);
 *         it.putExtra("title", title);
 *         it.putExtra("pic", pic);
 *         // ...
 *         it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 *         appContext.startActivity(it);
 *     }
 * }
 * }</pre>
 */
public interface PlayerGateway {

    void playUrl(String url, String title, JSONObject options);

    void playVod(String siteKey, String vodId, String title, String pic, JSONObject options);

    void playVodInline(JSONObject payload);

    void preloadArtwork(String pic, String wallPic);

    void control(String action);

    /** state: 1=其它 2=ready 3=playing 6=buffering */
    JSONObject status();
}
