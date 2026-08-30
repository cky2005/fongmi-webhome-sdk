# FongMi WebHome SDK — Usage Guide

## 1. As a Spider (recommended)

The simplest way to integrate. Add a Spider entry to your site config:

```json
{
  "key": "webhome",
  "name": "WebHome 演示",
  "type": 3,
  "api": "csp_WebHome",
  "homePage": "https://example.com/your-home.html"
}
```

When the user enters this site:
- `WebHome.homeContent()` is called → opens the WebView overlay loading `homePage`
- The page can use all `window.fm.*` APIs
- When the user presses Back, the WebView is dismissed

## 2. As a library (advanced)

Add the AAR to your project, then call directly:

```java
import com.fongmi.web.FmController;
import com.fongmi.web.WebHomeChrome;
import com.github.catvod.spider.WebHome;

// 1. In Application.onCreate (optional)
WebHome.setHandler(new MyCustomHandler(this));
WebHome.setContext(this);

// 2. From anywhere
FmController.get().show(
    this,                                  // Context
    "https://example.com/home.html",       // URL
    "my_site",                             // siteKey
    "My WebHome",                          // siteName
    null,                                  // site header (or a Map)
    null                                   // custom handler (null = default)
);
FmController.get().setChromeMode(WebHomeChrome.EDGE);
FmController.get().close();
```

## 3. Customizing the action handler

```java
public class MyHandler extends DefaultFmActionHandler {
    public MyHandler(Context ctx) {
        super(ctx, new MyPlayerGateway(ctx));
    }

    @Override
    public void search(String keyword, JSONObject options) {
        // fongmi 壳: 启动 SearchActivity
        Intent it = new Intent();
        it.setClassName(appContext, "com.fongmi.android.tv.ui.activity.SearchActivity");
        it.putExtra("keyword", keyword);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(it);
    }

    @Override
    public void openVod() {
        Intent it = new Intent();
        it.setClassName(appContext, "com.fongmi.android.tv.ui.activity.HomeActivity");
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(it);
    }

    @Override
    public void openSetting() {
        Intent it = new Intent();
        it.setClassName(appContext, "com.fongmi.android.tv.ui.activity.SettingActivity");
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appContext.startActivity(it);
    }

    @Override
    public JSONObject history() {
        // 从本地数据库或 Prefers 读
        try {
            JSONArray arr = new JSONArray();
            // ... fill arr from your data source ...
            return arr;
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
```

## 4. Implementing a PlayerGateway

For `fm.play()` / `fm.vod()` to actually start playback in your shell:

```java
public class MyPlayerGateway implements PlayerGateway {
    private final Context ctx;
    public MyPlayerGateway(Context ctx) { this.ctx = ctx; }

    @Override
    public void playUrl(String url, String title, JSONObject options) {
        // 方案 A: 通过 Spider.playerContent 走 push agent
        // 我们的 WebHome Spider 已经处理 — 任何 playUrl 调用都会
        // 通过 FmActionHandler.playUrl() -> 你的实现 -> 触发播放
        //
        // 方案 B: 直接启动 VideoActivity
        Intent it = new Intent();
        it.setClassName(ctx, "com.fongmi.android.tv.ui.activity.VideoActivity");
        it.putExtra("siteKey", "push_agent");
        it.putExtra("vodId", url);
        it.putExtra("title", title);
        if (options != null) {
            it.putExtra("pic", options.optString("pic"));
            it.putExtra("wallPic", options.optString("wallPic"));
        }
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(it);
    }

    @Override
    public void playVod(String siteKey, String vodId, String title, String pic, JSONObject options) {
        // 启动壳的 VOD 详情/播放
        Intent it = new Intent();
        it.setClassName(ctx, "com.fongmi.android.tv.ui.activity.VideoActivity");
        it.putExtra("siteKey", siteKey);
        it.putExtra("vodId", vodId);
        it.putExtra("title", title);
        it.putExtra("pic", pic);
        if (options != null) it.putExtra("wallPic", options.optString("wallPic"));
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(it);
    }

    @Override
    public void playVodInline(JSONObject payload) {
        // 复杂: 启动 VideoActivity 携带自定义 payload
        Intent it = new Intent();
        it.setClassName(ctx, "com.fongmi.android.tv.ui.activity.VideoActivity");
        it.putExtra("inlinePayload", payload.toString());
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(it);
    }

    @Override
    public void preloadArtwork(String pic, String wallPic) {
        // 用 Glide 预热
        if (!TextUtils.isEmpty(pic)) Glide.with(ctx).load(pic).preload();
        if (!TextUtils.isEmpty(wallPic)) Glide.with(ctx).load(wallPic).preload();
    }

    @Override
    public void control(String action) {
        // 调壳的 PlaybackService
        // ... start service intent with action ...
    }

    @Override
    public JSONObject status() {
        // 从壳的 PlaybackService 拿当前状态
        JSONObject s = new JSONObject();
        try {
            s.put("state", 1);
            s.put("speed", 1.0);
            s.put("duration", 0);
            s.put("position", 0);
        } catch (JSONException e) {}
        return s;
    }
}
```

## 5. WebView configuration

If you need to customize WebView settings (UA, default zoom, etc.), override `setupWebView` in your subclass of `FmController`. Or call these helpers on the WebView after `show()`:

```java
WebView wv = FmController.get().getWebView(); // not implemented yet, see FmController
```

For now, the SDK provides sensible defaults. Customization goes in `FmController.setupWebView()` — feel free to override.

## 6. Inline episode resolver (advanced)

If your HTML page has multiple episodes and each episode page needs JS to extract the real media URL, use `fm.vodInline()` with a resolver:

```javascript
// Register resolver BEFORE calling fm.vodInline
window.__fmWebHomeInlineResolver = async function (episode) {
    // episode = { name, url, pageUrl, resolve, active }
    // Return { url, format?, headers?, credentials? }
    const html = await fm.req(episode.pageUrl, {
        headers: { Referer: location.href },
        credentials: 'include'
    });
    const m = html.text.match(/https?:\/\/[^"'\s]+\.m3u8[^"'\s]*/i);
    if (!m) throw new Error('m3u8 not found');
    return { url: m[0], format: 'application/x-mpegURL' };
};

// Then:
await fm.vodInline({
    vod_name: '示例多集',
    vod_pic: '...',
    episodes: [
        { name: '01', url: 'ep://1', pageUrl: 'https://example.com/play/1' },
        { name: '02', url: 'ep://2', pageUrl: 'https://example.com/play/2' }
    ]
});
```

When user picks an episode, the native player page calls the resolver in the WebView, gets the media URL, plays it.
