# FongMi WebHome SDK

[![Build Status](https://github.com/yourname/fongmi-webhome-sdk/actions/workflows/build.yml/badge.svg)](https://github.com/yourname/fongmi-webhome-sdk/actions)

A standalone WebHome/FM SDK library that can be embedded into any fongmi/catvod family TV box shell (FongMi/TV, TVBox, fengmi-based shells, etc.) to support HTML pages that use the `window.fm` / `window.fongmi` API.

## Features

- ✅ Full FM SDK compatibility — every method supported by webhtv's `HomeWebBridge.java`
- ✅ Java bridge (`@JavascriptInterface`) + injected `fmsdk.js` script
- ✅ WebView lifecycle (timeout recovery, render-process-gone, focus management)
- ✅ Inline episode resolver (`__fmWebHomeInlineResolver`)
- ✅ Large result chunking (12KB threshold)
- ✅ Pluggable action handler — shell can override any method
- ✅ Default action handler (HttpURLConnection, SharedPreferences cache)
- ✅ SDK API contract matches webhtv, so existing WebHome pages work as-is

## API Surface

All these `window.fm.*` and `window.fongmi.*` methods are supported:

| Category | Methods |
|---|---|
| **net** | `fm.req(url, opts)`, `fm.res(url, opts)` |
| **player** | `fm.play`, `fm.vod`, `fm.vodInline`, `fm.preloadArtwork`, `fm.ctrl`, `fm.stat` |
| **app** | `fm.search`, `fm.openVod/Live/Keep/Setting`, `fm.history`, `fm.back`, `fm.reload` |
| **cache** | `fm.cache.get/set/del(key, value, rule)` |
| **ui** | `fm.ui.setToolbar`, `fm.ui.setChrome`, `fm.ui.restoreChrome`, `fm.ui.getViewport` |
| **device/site/config** | `fm.device()`, `fm.site()`, `fm.config()` |
| **ext** | `fm.ext.info()`, `fm.ext.log()`, `fm.ext.toast()` |
| **pan** | `fm.pan.check(items)`, `fm.pan.play(payload)` |
| **navigation** | `fm.back()`, `fm.reload()` |
| **events** | `fmsdk`, `fmurlchange`, `popstate` |

See [docs/API.md](docs/API.md) for complete contract.

## Quick Start

### 1. Add the AAR to your shell project

In your `build.gradle`:
```gradle
dependencies {
    implementation project(':webhome-sdk')  // if AAR as module
    // or
    implementation files('libs/fongmi-webhome-sdk.aar')
}
```

### 2. Add a Spider entry in your site config

```json
{
  "key": "webhome",
  "name": "WebHome 演示",
  "type": 3,
  "api": "csp_WebHome",
  "homePage": "https://example.com/your-home.html"
}
```

`api: "csp_WebHome"` resolves to `com.github.catvod.spider.WebHome` — a `Spider` subclass that:
- On `homeContent()` / `detailContent()` → opens the WebView overlay loading `homePage`
- Injects `fmsdk.js` → exposes `window.fm` / `window.fongmi`
- Routes every SDK call through the `FmActionHandler` interface
- The `playerContent()` is wired to return the URL as a `parse:0, url:...` result, which fongmi-family shells will play automatically

### 3. (Optional) Plug in your own action handler

```java
public class MyHandler extends DefaultFmActionHandler {
    public MyHandler(Context ctx) {
        super(ctx, new MyPlayerGateway(ctx));
    }
    @Override
    public void search(String keyword, JSONObject options) {
        // start SearchActivity with your own logic
    }
    @Override
    public void openVod() {
        // start your VOD home
    }
}

// In your Application.onCreate:
WebHome.setHandler(new MyHandler(this));
```

## Shell Compatibility

| Shell | Compatible? | Notes |
|---|---|---|
| FongMi/TV | ✅ | Full support |
| TVBox (猫影视官方) | ✅ | Full support |
| fengmi 二开壳 | ✅ | Should work, set up the right `Spider` base class path |
| OKTV / 仓 TV 等 | ✅ | Compatible if shell uses catvod's `Spider` interface |
| 影迷 / 影视仓 | ⚠️ | Depends on Spider interface; may need shell-specific bridge |

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    HTML page in WebView                  │
│  <script>window.fm.req('https://api...', {...})</script> │
└─────────────┬────────────────────────────────────────────┘
              │ window.fm / window.fongmi
              ▼
┌──────────────────────────────────────────────────────────┐
│              fmsdk.js (injected at page load)             │
│   - wraps Promise around synchronous bridge.invoke()     │
│   - dispatches fmsdk / fmurlchange events                │
└─────────────┬────────────────────────────────────────────┘
              │ window.fongmiBridge.invoke(id, method, json)
              ▼
┌──────────────────────────────────────────────────────────┐
│  FmBridge.java  (JavascriptInterface)                    │
│   - parses method, dispatches to FmActionHandler         │
│   - chunks large results (>12KB)                         │
│   - resolves back via window.fongmiNative.resolve(id,..) │
└─────────────┬────────────────────────────────────────────┘
              │ method calls
              ▼
┌──────────────────────────────────────────────────────────┐
│  FmActionHandler (interface) — injected by shell         │
│   Default impl: DefaultFmActionHandler                   │
│   Shell can extend it (e.g. for VideoActivity.start)     │
└──────────────────────────────────────────────────────────┘
```

## Building

```bash
gradle :sdk:assembleRelease :sdk:jarWithAssets
```

Outputs:
- `sdk/build/outputs/aar/sdk-release.aar` — Android library with manifest
- `sdk/build/libs/sdk.jar` — Java classes only
- `sdk/build/libs/sdk-assets.jar` — includes fmsdk.js
- `sdk/build/libs/sdk-all.jar` — fat jar

## Demo

A complete demo HTML is included at `sdk/src/main/assets/webhome-demo.html`. To test:

1. Build the AAR
2. Add to a shell project
3. In your site config, set `"homePage": "https://your-shell-server/webhome-demo.html"` (or copy the HTML to a local file URL)
4. Open the shell, navigate to the WebHome site
5. The page exposes buttons for every SDK method

## License

GPL-3.0 — same as FongMi/TV
