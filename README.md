# FongMi WebHome SDK

A standalone WebHome/FM SDK library for fongmi/catvod TV box shells. Lets any fongmi-shell (FongMi/TV, TVBox, fengmi forks) load HTML pages that use `window.fm` / `window.fongmi`.

## 快速使用 (Quick start)

把生成的 `webhome.jar` 放到壳的 `app/libs/` 或 `app/src/main/assets/spider/`，添加站点配置：

```json
{
  "key": "webhome",
  "name": "WebHome 演示",
  "type": 3,
  "api": "csp_WebHome",
  "homePage": "https://example.com/your-home.html"
}
```

把 `fmsdk.js` 从 `webhome-assets.jar` 抽出来放到壳的 `app/src/main/assets/fmsdk.js`：
```bash
unzip -j webhome-assets.jar fmsdk.js -d app/src/main/assets/
```

## 本地构建

```bash
export ANDROID_SDK_ROOT=/path/to/Android/Sdk
sdkmanager "platforms;android-34" "build-tools;34.0.0"
bash build.sh
```

输出在 `sdk/build/libs/`：
- `webhome.jar` — 主 SDK (含 dex，可被 DexClassLoader 加载)
- `webhome-assets.jar` — fmsdk.js + demo HTML

## 接入方式

壳用 DexClassLoader 加载 `webhome.jar` 后会自动找到 `com.github.catvod.spider.WebHome` 类（key=api=csp_WebHome → 类名 com.github.catvod.spider.WebHome）。

页面上 `window.fm.*` / `window.fongmi.*` 全部 SDK 方法可用，包括：
- `fm.req(url, options)` - 跨域 HTTP
- `fm.res(url, options)` - 生成 /webResource URL
- `fm.play/vod/vodInline/preloadArtwork/ctrl/stat` - 播放
- `fm.search/history/openVod/Live/Keep/Setting/back/reload` - App 能力
- `fm.cache.get/set/del` - 持久化
- `fm.ui.setChrome/getViewport/...` - UI
- `fm.device/site/config` - 信息
- `fm.ext.info/log/toast` - 扩展
- `fm.pan.check/play` - 网盘

## 自定义业务

壳可以在 `Application.onCreate` 调用：
```java
WebHome.setHandler(new MyFmActionHandler(this));
```

详见 `docs/` 目录。
