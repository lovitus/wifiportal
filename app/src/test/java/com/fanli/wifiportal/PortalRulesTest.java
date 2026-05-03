package com.fanli.wifiportal;

import org.junit.Test;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PortalRulesTest {
    @Test
    public void disableDetectionWritesLegacyAndModernDisableKeys() {
        PortalConfig config = PortalConfig.defaults();
        config.disableDetection = true;
        config.includeLegacyWifiWatchdog = true;

        List<PortalSetting> desired = PortalRules.desired(config, 35);

        assertHas(desired, PortalSetting.Store.GLOBAL, PortalRules.CAPTIVE_PORTAL_DETECTION_ENABLED, "0");
        assertHas(desired, PortalSetting.Store.GLOBAL, PortalRules.CAPTIVE_PORTAL_MODE, "0");
        assertHas(desired, PortalSetting.Store.GLOBAL, PortalRules.CAPTIVE_PORTAL_USE_HTTPS, "0");
        assertHas(desired, PortalSetting.Store.DEVICE_CONFIG_CONNECTIVITY, PortalRules.CAPTIVE_PORTAL_USE_HTTPS, "0");
        assertHas(desired, PortalSetting.Store.GLOBAL, PortalRules.NETWORK_AVOID_BAD_WIFI, "0");
    }

    @Test
    public void androidFiveRulesAvoidDeviceConfig() {
        PortalConfig config = PortalConfig.defaults();

        List<PortalSetting> desired = PortalRules.desired(config, 21);

        for (PortalSetting setting : desired) {
            assertFalse(setting.store == PortalSetting.Store.DEVICE_CONFIG_CONNECTIVITY);
        }
        assertHas(desired, PortalSetting.Store.GLOBAL, PortalRules.CAPTIVE_PORTAL_SERVER, "connectivitycheck.gstatic.com");
    }

    @Test
    public void diffReportsChangedValues() {
        PortalConfig config = PortalConfig.defaults();
        List<PortalSetting> desired = PortalRules.desired(config, 35);
        Map<String, PortalValue> current = new HashMap<>();
        for (PortalSetting setting : desired) {
            current.put(setting.id(), PortalValue.of(setting.value, "test"));
        }
        current.put(desired.get(0).id(), PortalValue.of("different", "test"));

        List<PortalRules.Diff> diffs = PortalRules.diff(desired, current, 35);

        assertEquals(1, diffs.size());
        assertEquals("different", diffs.get(0).actual.value);
    }

    @Test
    public void mainlandChinaPresetUsesReachableDomesticEndpoints() {
        PortalConfig config = PortalConfig.mainlandChina();

        assertEquals("http://connect.rom.miui.com/generate_204", config.httpUrl);
        assertEquals("https://connect.rom.miui.com/generate_204", config.httpsUrl);
        assertEquals("http://connectivitycheck.platform.hicloud.com/generate_204", config.fallbackUrl);
        assertTrue(config.otherHttpsUrls.contains("https://connectivitycheck.platform.hicloud.com/generate_204"));
        assertTrue(config.useHttps);
    }

    @Test
    public void writePlanMismatchChecksMissingTargets() {
        PortalSetting setting = new PortalSetting(
                PortalSetting.Store.GLOBAL,
                PortalRules.CAPTIVE_PORTAL_HTTP_URL,
                "",
                21);
        List<PortalWritePlan.Item> items = new ArrayList<>();
        items.add(new PortalWritePlan.Item(
                setting,
                PortalValue.of("original", "test"),
                PortalValue.of("current", "test"),
                PortalValue.missing("restore"),
                setting.deleteCommand(),
                "test"));
        PortalWritePlan plan = new PortalWritePlan("restore", items);
        Map<String, PortalValue> current = new HashMap<>();
        current.put(setting.id(), PortalValue.of("still-set", "test"));

        assertEquals(1, plan.mismatches(current).size());

        current.put(setting.id(), PortalValue.missing("test"));
        assertEquals(0, plan.mismatches(current).size());
    }

    private static void assertHas(List<PortalSetting> settings, PortalSetting.Store store, String key, String value) {
        for (PortalSetting setting : settings) {
            if (setting.store == store && setting.key.equals(key) && setting.value.equals(value)) {
                assertTrue(true);
                return;
            }
        }
        throw new AssertionError("Missing " + store + " " + key + "=" + value);
    }
}
