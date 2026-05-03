package com.fanli.wifiportal;

import android.content.SharedPreferences;

import java.net.URI;
import java.net.URISyntaxException;

final class PortalConfig {
    static final String DEFAULT_HTTP_URL = "http://connectivitycheck.gstatic.com/generate_204";
    static final String DEFAULT_HTTPS_URL = "https://www.google.com/generate_204";
    static final String DEFAULT_FALLBACK_URL = "http://www.google.com/gen_204";
    static final String CHINA_HTTP_URL = "http://connect.rom.miui.com/generate_204";
    static final String CHINA_HTTPS_URL = "https://connect.rom.miui.com/generate_204";
    static final String CHINA_FALLBACK_URL = "http://connectivitycheck.platform.hicloud.com/generate_204";
    static final String CHINA_FALLBACK_HTTPS_URL = "https://connectivitycheck.platform.hicloud.com/generate_204";

    boolean disableDetection;
    boolean includeLegacyServer;
    boolean includeHttpUrl;
    boolean includeHttpsUrl;
    boolean includeFallbackUrl;
    boolean includeOtherFallbackUrls;
    boolean includeModernUrlLists;
    boolean includeUseHttps;
    boolean includeUserAgent;
    boolean includeFallbackProbeSpecs;
    boolean includeLegacyWifiWatchdog;
    String httpUrl;
    String httpsUrl;
    String fallbackUrl;
    String otherFallbackUrls;
    String otherHttpUrls;
    String otherHttpsUrls;
    String userAgent;
    String fallbackProbeSpecs;
    boolean useHttps;
    int portalMode;

    static PortalConfig defaults() {
        PortalConfig config = new PortalConfig();
        config.disableDetection = false;
        config.includeLegacyServer = true;
        config.includeHttpUrl = true;
        config.includeHttpsUrl = true;
        config.includeFallbackUrl = true;
        config.includeOtherFallbackUrls = true;
        config.includeModernUrlLists = true;
        config.includeUseHttps = true;
        config.includeUserAgent = false;
        config.includeFallbackProbeSpecs = false;
        config.includeLegacyWifiWatchdog = false;
        config.httpUrl = DEFAULT_HTTP_URL;
        config.httpsUrl = DEFAULT_HTTPS_URL;
        config.fallbackUrl = DEFAULT_FALLBACK_URL;
        config.otherFallbackUrls = DEFAULT_FALLBACK_URL;
        config.otherHttpUrls = DEFAULT_HTTP_URL;
        config.otherHttpsUrls = DEFAULT_HTTPS_URL;
        config.userAgent = "";
        config.fallbackProbeSpecs = "";
        config.useHttps = true;
        config.portalMode = 1;
        return config;
    }

    static PortalConfig mainlandChina() {
        PortalConfig config = defaults();
        config.disableDetection = false;
        config.includeLegacyServer = true;
        config.includeHttpUrl = true;
        config.includeHttpsUrl = true;
        config.includeFallbackUrl = true;
        config.includeOtherFallbackUrls = true;
        config.includeModernUrlLists = true;
        config.includeUseHttps = true;
        config.includeUserAgent = false;
        config.includeFallbackProbeSpecs = false;
        config.includeLegacyWifiWatchdog = false;
        config.httpUrl = CHINA_HTTP_URL;
        config.httpsUrl = CHINA_HTTPS_URL;
        config.fallbackUrl = CHINA_FALLBACK_URL;
        config.otherFallbackUrls = CHINA_HTTP_URL + "," + CHINA_FALLBACK_URL;
        config.otherHttpUrls = CHINA_HTTP_URL + "," + CHINA_FALLBACK_URL;
        config.otherHttpsUrls = CHINA_HTTPS_URL + "," + CHINA_FALLBACK_HTTPS_URL;
        config.useHttps = true;
        config.portalMode = 1;
        return config;
    }

    static PortalConfig load(SharedPreferences prefs) {
        PortalConfig def = defaults();
        PortalConfig config = new PortalConfig();
        config.disableDetection = prefs.getBoolean("disableDetection", def.disableDetection);
        config.includeLegacyServer = prefs.getBoolean("includeLegacyServer", def.includeLegacyServer);
        config.includeHttpUrl = prefs.getBoolean("includeHttpUrl", def.includeHttpUrl);
        config.includeHttpsUrl = prefs.getBoolean("includeHttpsUrl", def.includeHttpsUrl);
        config.includeFallbackUrl = prefs.getBoolean("includeFallbackUrl", def.includeFallbackUrl);
        config.includeOtherFallbackUrls = prefs.getBoolean("includeOtherFallbackUrls", def.includeOtherFallbackUrls);
        config.includeModernUrlLists = prefs.getBoolean("includeModernUrlLists", def.includeModernUrlLists);
        config.includeUseHttps = prefs.getBoolean("includeUseHttps", def.includeUseHttps);
        config.includeUserAgent = prefs.getBoolean("includeUserAgent", def.includeUserAgent);
        config.includeFallbackProbeSpecs = prefs.getBoolean("includeFallbackProbeSpecs", def.includeFallbackProbeSpecs);
        config.includeLegacyWifiWatchdog = prefs.getBoolean("includeLegacyWifiWatchdog", def.includeLegacyWifiWatchdog);
        config.httpUrl = prefs.getString("httpUrl", def.httpUrl);
        config.httpsUrl = prefs.getString("httpsUrl", def.httpsUrl);
        config.fallbackUrl = prefs.getString("fallbackUrl", def.fallbackUrl);
        config.otherFallbackUrls = prefs.getString("otherFallbackUrls", def.otherFallbackUrls);
        config.otherHttpUrls = prefs.getString("otherHttpUrls", def.otherHttpUrls);
        config.otherHttpsUrls = prefs.getString("otherHttpsUrls", def.otherHttpsUrls);
        config.userAgent = prefs.getString("userAgent", def.userAgent);
        config.fallbackProbeSpecs = prefs.getString("fallbackProbeSpecs", def.fallbackProbeSpecs);
        config.useHttps = prefs.getBoolean("useHttps", def.useHttps);
        config.portalMode = prefs.getInt("portalMode", def.portalMode);
        return config;
    }

    void save(SharedPreferences prefs) {
        prefs.edit()
                .putBoolean("disableDetection", disableDetection)
                .putBoolean("includeLegacyServer", includeLegacyServer)
                .putBoolean("includeHttpUrl", includeHttpUrl)
                .putBoolean("includeHttpsUrl", includeHttpsUrl)
                .putBoolean("includeFallbackUrl", includeFallbackUrl)
                .putBoolean("includeOtherFallbackUrls", includeOtherFallbackUrls)
                .putBoolean("includeModernUrlLists", includeModernUrlLists)
                .putBoolean("includeUseHttps", includeUseHttps)
                .putBoolean("includeUserAgent", includeUserAgent)
                .putBoolean("includeFallbackProbeSpecs", includeFallbackProbeSpecs)
                .putBoolean("includeLegacyWifiWatchdog", includeLegacyWifiWatchdog)
                .putString("httpUrl", clean(httpUrl))
                .putString("httpsUrl", clean(httpsUrl))
                .putString("fallbackUrl", clean(fallbackUrl))
                .putString("otherFallbackUrls", clean(otherFallbackUrls))
                .putString("otherHttpUrls", clean(otherHttpUrls))
                .putString("otherHttpsUrls", clean(otherHttpsUrls))
                .putString("userAgent", clean(userAgent))
                .putString("fallbackProbeSpecs", clean(fallbackProbeSpecs))
                .putBoolean("useHttps", useHttps)
                .putInt("portalMode", portalMode)
                .apply();
    }

    String validate() {
        if (!disableDetection) {
            String error = validateUrlList("HTTP URL", httpUrl, false);
            if (includeHttpUrl && error != null) return error;
            error = validateUrlList("HTTPS URL", httpsUrl, false);
            if (includeHttpsUrl && error != null) return error;
            error = validateUrlList("Fallback URL", fallbackUrl, false);
            if (includeFallbackUrl && error != null) return error;
            error = validateUrlList("Other fallback URLs", otherFallbackUrls, true);
            if (includeOtherFallbackUrls && error != null) return error;
            error = validateUrlList("Other HTTP URLs", otherHttpUrls, true);
            if (includeModernUrlLists && error != null) return error;
            error = validateUrlList("Other HTTPS URLs", otherHttpsUrls, true);
            if (includeModernUrlLists && error != null) return error;
        }
        return null;
    }

    static String hostFromUrl(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getHost() != null && uri.getHost().length() > 0) {
                return uri.getHost();
            }
        } catch (URISyntaxException ignored) {
        }
        return clean(url);
    }

    static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String validateUrlList(String label, String value, boolean commaList) {
        String clean = clean(value);
        if (clean.length() == 0) {
            return label + " 不能为空";
        }
        String[] parts = commaList ? clean.split(",") : new String[]{clean};
        for (String part : parts) {
            String candidate = part.trim();
            try {
                URI uri = new URI(candidate);
                String scheme = uri.getScheme();
                if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                    return label + " 必须使用 http 或 https";
                }
                if (uri.getHost() == null || uri.getHost().length() == 0) {
                    return label + " 缺少 host";
                }
            } catch (URISyntaxException e) {
                return label + " 格式不正确";
            }
        }
        return null;
    }
}
