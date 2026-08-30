# FongMi WebHome SDK

A standalone WebHome/FM SDK library for fongmi/catvod TV box shells (FongMi/TV, TVBox, fengmi forks, etc.). Lets shells load HTML pages that use `window.fm` / `window.fongmi`.

## Quick start

Add to site config:
```json
{
  "key": "webhome",
  "name": "WebHome 演示",
  "type": 3,
  "api": "csp_WebHome",
  "homePage": "https://example.com/your-home.html"
}
```

## Build

GitHub Actions will build a jar automatically. To build locally:
```bash
export ANDROID_HOME=/path/to/Android/Sdk
gradle :sdk:jar :sdk:jarWithAssets :sdk:sourcesJar
```

## Output jars

- `sdk/build/libs/sdk.jar` — main SDK
- `sdk/build/libs/sdk-assets.jar` — fmsdk.js + demo HTML
- `sdk/build/libs/sdk-sources.jar` — sources
