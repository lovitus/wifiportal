package com.fanli.wifiportal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PortalRules {
    static final String CAPTIVE_PORTAL_SERVER = "captive_portal_server";
    static final String CAPTIVE_PORTAL_DETECTION_ENABLED = "captive_portal_detection_enabled";
    static final String CAPTIVE_PORTAL_MODE = "captive_portal_mode";
    static final String CAPTIVE_PORTAL_HTTP_URL = "captive_portal_http_url";
    static final String CAPTIVE_PORTAL_HTTPS_URL = "captive_portal_https_url";
    static final String CAPTIVE_PORTAL_FALLBACK_URL = "captive_portal_fallback_url";
    static final String CAPTIVE_PORTAL_OTHER_FALLBACK_URLS = "captive_portal_other_fallback_urls";
    static final String CAPTIVE_PORTAL_OTHER_HTTP_URLS = "captive_portal_other_http_urls";
    static final String CAPTIVE_PORTAL_OTHER_HTTPS_URLS = "captive_portal_other_https_urls";
    static final String CAPTIVE_PORTAL_USER_AGENT = "captive_portal_user_agent";
    static final String CAPTIVE_PORTAL_USE_HTTPS = "captive_portal_use_https";
    static final String CAPTIVE_PORTAL_FALLBACK_PROBE_SPECS = "captive_portal_fallback_probe_specs";
    static final String NETWORK_AVOID_BAD_WIFI = "network_avoid_bad_wifi";
    static final String WIFI_WATCHDOG_ON = "wifi_watchdog_on";
    static final String WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED = "wifi_watchdog_poor_network_test_enabled";
    static final String WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED = "wifi_watchdog_background_check_enabled";

    private PortalRules() {
    }

    static List<PortalSetting> desired(PortalConfig config, int sdk) {
        List<PortalSetting> settings = new ArrayList<>();
        if (config.disableDetection) {
            addGlobal(settings, CAPTIVE_PORTAL_DETECTION_ENABLED, "0", 21);
            addGlobal(settings, CAPTIVE_PORTAL_MODE, "0", 23);
            addMirrored(settings, CAPTIVE_PORTAL_USE_HTTPS, "0", sdk);
            if (config.includeLegacyServer) {
                addGlobal(settings, CAPTIVE_PORTAL_SERVER, "127.0.0.1", 21);
            }
            if (config.includeLegacyWifiWatchdog) {
                addGlobal(settings, NETWORK_AVOID_BAD_WIFI, "0", 21);
                addGlobal(settings, WIFI_WATCHDOG_ON, "0", 21);
                addGlobal(settings, WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED, "0", 21);
                addGlobal(settings, WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED, "0", 21);
            }
            return settings;
        }

        addGlobal(settings, CAPTIVE_PORTAL_DETECTION_ENABLED, "1", 21);
        addGlobal(settings, CAPTIVE_PORTAL_MODE, String.valueOf(config.portalMode), 23);

        if (config.includeLegacyServer) {
            addGlobal(settings, CAPTIVE_PORTAL_SERVER, PortalConfig.hostFromUrl(config.httpUrl), 21);
        }
        if (config.includeHttpUrl) {
            addGlobal(settings, CAPTIVE_PORTAL_HTTP_URL, PortalConfig.clean(config.httpUrl), 24);
        }
        if (config.includeHttpsUrl) {
            addGlobal(settings, CAPTIVE_PORTAL_HTTPS_URL, PortalConfig.clean(config.httpsUrl), 24);
        }
        if (config.includeFallbackUrl) {
            addGlobal(settings, CAPTIVE_PORTAL_FALLBACK_URL, PortalConfig.clean(config.fallbackUrl), 24);
        }
        if (config.includeOtherFallbackUrls) {
            addMirrored(settings, CAPTIVE_PORTAL_OTHER_FALLBACK_URLS, PortalConfig.clean(config.otherFallbackUrls), sdk);
        }
        if (config.includeModernUrlLists) {
            addMirrored(settings, CAPTIVE_PORTAL_OTHER_HTTP_URLS, PortalConfig.clean(config.otherHttpUrls), sdk);
            addMirrored(settings, CAPTIVE_PORTAL_OTHER_HTTPS_URLS, PortalConfig.clean(config.otherHttpsUrls), sdk);
        }
        if (config.includeUseHttps) {
            addMirrored(settings, CAPTIVE_PORTAL_USE_HTTPS, config.useHttps ? "1" : "0", sdk);
        }
        if (config.includeUserAgent && PortalConfig.clean(config.userAgent).length() > 0) {
            addMirrored(settings, CAPTIVE_PORTAL_USER_AGENT, PortalConfig.clean(config.userAgent), sdk);
        }
        if (config.includeFallbackProbeSpecs && PortalConfig.clean(config.fallbackProbeSpecs).length() > 0) {
            addMirrored(settings, CAPTIVE_PORTAL_FALLBACK_PROBE_SPECS, PortalConfig.clean(config.fallbackProbeSpecs), sdk);
        }
        return settings;
    }

    static List<Diff> diff(List<PortalSetting> desired, Map<String, String> current, int sdk) {
        List<Diff> diffs = new ArrayList<>();
        for (PortalSetting setting : desired) {
            if (sdk < setting.minSdk) {
                continue;
            }
            String actual = current.get(setting.id());
            if (actual == null) {
                diffs.add(new Diff(setting, "(读取失败)", setting.value));
            } else if (!setting.value.equals(actual)) {
                diffs.add(new Diff(setting, actual, setting.value));
            }
        }
        return diffs;
    }

    static Map<String, String> emptyCurrent(List<PortalSetting> desired) {
        Map<String, String> map = new LinkedHashMap<>();
        for (PortalSetting setting : desired) {
            map.put(setting.id(), "");
        }
        return map;
    }

    private static void addGlobal(List<PortalSetting> settings, String key, String value, int minSdk) {
        settings.add(new PortalSetting(PortalSetting.Store.GLOBAL, key, value, minSdk));
    }

    private static void addMirrored(List<PortalSetting> settings, String key, String value, int sdk) {
        settings.add(new PortalSetting(PortalSetting.Store.GLOBAL, key, value, 21));
        if (sdk >= 29) {
            settings.add(new PortalSetting(PortalSetting.Store.DEVICE_CONFIG_CONNECTIVITY, key, value, 29));
        }
    }

    static final class Diff {
        final PortalSetting setting;
        final String actual;
        final String expected;

        Diff(PortalSetting setting, String actual, String expected) {
            this.setting = setting;
            this.actual = actual;
            this.expected = expected;
        }
    }
}
