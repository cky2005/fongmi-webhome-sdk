package com.fongmi.web;

import android.text.TextUtils;

/**
 * WebHome chrome mode 常量 — 与 fongmi 系壳的语义一致。
 * <pre>
 *  mobile:
 *    normal    原生 UI 显示，WebHome 在原生内容区内
 *    edge      推荐首页。隐藏原生 UI，WebView 铺满，系统栏透明
 *    immersive 隐藏原生 UI + 系统栏
 *
 *  tv:
 *    tv-normal         顶部 toolbar + WebView 在 toolbar 下
 *    tv-toolbar-hidden WebView 全屏 overlay
 *    tv-overlay        顶部 toolbar，WebView 下移避让
 *    tv-full           WebView 全屏 overlay，隐藏 toolbar
 * </pre>
 */
public final class WebHomeChrome {

    public static final String NORMAL = "normal";
    public static final String EDGE = "edge";
    public static final String IMMERSIVE = "immersive";
    public static final String TV_NORMAL = "tv-normal";
    public static final String TV_TOOLBAR_HIDDEN = "tv-toolbar-hidden";
    public static final String TV_OVERLAY = "tv-overlay";
    public static final String TV_FULL = "tv-full";

    private WebHomeChrome() {}

    public static boolean isValid(String mode) {
        return NORMAL.equals(mode) || EDGE.equals(mode) || IMMERSIVE.equals(mode)
                || TV_NORMAL.equals(mode) || TV_TOOLBAR_HIDDEN.equals(mode)
                || TV_OVERLAY.equals(mode) || TV_FULL.equals(mode);
    }

    public static boolean hidesSystemBars(String mode) {
        return IMMERSIVE.equals(mode) || TV_FULL.equals(mode);
    }

    public static boolean isTv(String mode) {
        return TV_NORMAL.equals(mode) || TV_TOOLBAR_HIDDEN.equals(mode)
                || TV_OVERLAY.equals(mode) || TV_FULL.equals(mode);
    }
}
