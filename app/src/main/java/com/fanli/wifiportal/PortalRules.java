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

    static List<PortalSetting> catalog(int sdk) {
        List<PortalSetting> settings = new ArrayList<>();
        addGlobal(settings, CAPTIVE_PORTAL_SERVER, "", 21);
        addGlobal(settings, CAPTIVE_PORTAL_DETECTION_ENABLED, "", 21);
        addGlobal(settings, CAPTIVE_PORTAL_MODE, "", 23);
        addGlobal(settings, CAPTIVE_PORTAL_HTTP_URL, "", 24);
        addGlobal(settings, CAPTIVE_PORTAL_HTTPS_URL, "", 24);
        addGlobal(settings, CAPTIVE_PORTAL_FALLBACK_URL, "", 24);
        addMirrored(settings, CAPTIVE_PORTAL_OTHER_FALLBACK_URLS, "", sdk);
        addMirrored(settings, CAPTIVE_PORTAL_OTHER_HTTP_URLS, "", sdk);
        addMirrored(settings, CAPTIVE_PORTAL_OTHER_HTTPS_URLS, "", sdk);
        addMirrored(settings, CAPTIVE_PORTAL_USER_AGENT, "", sdk);
        addMirrored(settings, CAPTIVE_PORTAL_USE_HTTPS, "", sdk);
        addMirrored(settings, CAPTIVE_PORTAL_FALLBACK_PROBE_SPECS, "", sdk);
        addGlobal(settings, NETWORK_AVOID_BAD_WIFI, "", 21);
        addGlobal(settings, WIFI_WATCHDOG_ON, "", 21);
        addGlobal(settings, WIFI_WATCHDOG_POOR_NETWORK_TEST_ENABLED, "", 21);
        addGlobal(settings, WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED, "", 21);
        return supported(settings, sdk);
    }

    static List<Diff> diff(List<PortalSetting> desired, Map<String, PortalValue> current, int sdk) {
        List<Diff> diffs = new ArrayList<>();
        for (PortalSetting setting : desired) {
            if (sdk < setting.minSdk) {
                continue;
            }
            PortalValue actual = current.get(setting.id());
            if (actual == null) {
                diffs.add(new Diff(setting, PortalValue.missing("读取失败"), PortalValue.of(setting.value, "desired")));
            } else if (!actual.sameDesired(setting.value)) {
                diffs.add(new Diff(setting, actual, PortalValue.of(setting.value, "desired")));
            }
        }
        return diffs;
    }

    static Map<String, PortalValue> emptyCurrent(List<PortalSetting> desired) {
        Map<String, PortalValue> map = new LinkedHashMap<>();
        for (PortalSetting setting : desired) {
            map.put(setting.id(), PortalValue.missing("empty"));
        }
        return map;
    }

    static List<PortalWritePlan.Item> desiredPlanItems(
            List<PortalSetting> desired,
            Map<String, PortalValue> current,
            android.content.SharedPreferences prefs,
            int sdk,
            PortalSetting onlySetting) {
        return desiredPlanItems(desired, current, prefs, sdk, onlySetting, "程序配置: SharedPreferences + 页面保存项");
    }

    static List<PortalWritePlan.Item> desiredPlanItems(
            List<PortalSetting> desired,
            Map<String, PortalValue> current,
            android.content.SharedPreferences prefs,
            int sdk,
            PortalSetting onlySetting,
            String source) {
        List<PortalWritePlan.Item> items = new ArrayList<>();
        for (PortalSetting setting : desired) {
            if (sdk < setting.minSdk || (onlySetting != null && !setting.id().equals(onlySetting.id()))) {
                continue;
            }
            PortalValue currentValue = current.get(setting.id());
            PortalValue target = PortalValue.of(setting.value, "saved-config");
            if (currentValue != null && currentValue.same(target)) {
                continue;
            }
            items.add(new PortalWritePlan.Item(
                    setting,
                    PortalSnapshots.original(prefs, setting),
                    currentValue,
                    target,
                    setting.writeCommand(target),
                    source));
        }
        return items;
    }

    static List<PortalWritePlan.Item> restorePlanItems(
            List<PortalSetting> catalog,
            Map<String, PortalValue> current,
            android.content.SharedPreferences prefs,
            int sdk,
            PortalSetting onlySetting) {
        List<PortalWritePlan.Item> items = new ArrayList<>();
        for (PortalSetting setting : catalog) {
            if (sdk < setting.minSdk || (onlySetting != null && !setting.id().equals(onlySetting.id()))) {
                continue;
            }
            PortalValue original = PortalSnapshots.original(prefs, setting);
            if (original == null) {
                continue;
            }
            PortalValue currentValue = current.get(setting.id());
            if (currentValue != null && currentValue.same(original)) {
                continue;
            }
            items.add(new PortalWritePlan.Item(
                    setting,
                    original,
                    currentValue,
                    original,
                    setting.writeCommand(original),
                    "原始备份: SharedPreferences"));
        }
        return items;
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

    private static List<PortalSetting> supported(List<PortalSetting> settings, int sdk) {
        List<PortalSetting> supported = new ArrayList<>();
        for (PortalSetting setting : settings) {
            if (sdk >= setting.minSdk) {
                supported.add(setting);
            }
        }
        return supported;
    }

    static final class Diff {
        final PortalSetting setting;
        final PortalValue actual;
        final PortalValue expected;

        Diff(PortalSetting setting, PortalValue actual, PortalValue expected) {
            this.setting = setting;
            this.actual = actual;
            this.expected = expected;
        }
    }
}
