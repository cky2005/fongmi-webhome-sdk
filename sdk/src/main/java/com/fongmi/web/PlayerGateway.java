package com.fongmi.web;

import org.json.JSONObject;

public interface PlayerGateway {
    void playUrl(String url, String title, JSONObject options);
    void playVod(String siteKey, String vodId, String title, String pic, JSONObject options);
    void playVodInline(JSONObject payload);
    void preloadArtwork(String pic, String wallPic);
    void control(String action);
    JSONObject status();
}
