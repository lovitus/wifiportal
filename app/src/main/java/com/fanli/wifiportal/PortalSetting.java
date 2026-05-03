package com.fanli.wifiportal;

final class PortalSetting {
    enum Store {
        GLOBAL,
        DEVICE_CONFIG_CONNECTIVITY
    }

    final Store store;
    final String key;
    final String value;
    final int minSdk;

    PortalSetting(Store store, String key, String value, int minSdk) {
        this.store = store;
        this.key = key;
        this.value = value;
        this.minSdk = minSdk;
    }

    String id() {
        return store.name() + ":" + key;
    }

    String label() {
        if (store == Store.GLOBAL) {
            return "settings/global " + key;
        }
        return "device_config/connectivity " + key;
    }

    String readCommand() {
        if (store == Store.GLOBAL) {
            return "settings get global " + ShellText.quote(key);
        }
        return "cmd device_config get connectivity " + ShellText.quote(key);
    }

    String writeCommand() {
        if (store == Store.GLOBAL) {
            return "settings put global " + ShellText.quote(key) + " " + ShellText.quote(value);
        }
        return "cmd device_config put connectivity " + ShellText.quote(key) + " " + ShellText.quote(value);
    }
}
