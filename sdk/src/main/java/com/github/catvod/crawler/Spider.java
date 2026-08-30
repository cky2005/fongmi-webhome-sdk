package com.github.catvod.crawler;

import android.content.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stub of catvod's Spider class — used only at SDK build time.
 * The real Spider from catvod requires okhttp and throws checked Exceptions.
 * This stub mirrors the public API signatures so the SDK's WebHome.java can
 * be compiled without pulling in catvod + okhttp as build dependencies.
 *
 * At runtime, the host shell provides the real com.github.catvod.crawler.Spider
 * class which has identical method signatures.
 *
 * The compiled Spider.class is excluded from the main jar by build.gradle.
 */
public abstract class Spider {

    public String siteKey;

    public void init(Context context) throws Exception {}

    public void init(Context context, String extend) throws Exception {
        init(context);
    }

    public String homeContent(boolean filter) throws Exception { return ""; }

    public String homeVideoContent() throws Exception { return ""; }

    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        return "";
    }

    public String detailContent(List<String> ids) throws Exception { return ""; }

    public String searchContent(String key, boolean quick) throws Exception { return ""; }

    public String searchContent(String key, boolean quick, String pg) throws Exception { return ""; }

    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception { return ""; }

    public String liveContent(String url) throws Exception { return ""; }

    public boolean manualVideoCheck() throws Exception { return false; }

    public boolean isVideoFormat(String url) throws Exception { return false; }

    public Object[] proxy(Map<String, String> params) throws Exception { return null; }

    public String action(String action) throws Exception { return null; }

    public void destroy() {}
}
