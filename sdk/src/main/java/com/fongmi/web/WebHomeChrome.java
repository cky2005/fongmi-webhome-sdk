package com.fongmi.web;

import android.text.TextUtils;

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
