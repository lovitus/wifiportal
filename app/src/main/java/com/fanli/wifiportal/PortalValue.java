package com.fanli.wifiportal;

final class PortalValue {
    final boolean readable;
    final boolean exists;
    final String value;
    final String source;

    private PortalValue(boolean readable, boolean exists, String value, String source) {
        this.readable = readable;
        this.exists = exists;
        this.value = value == null ? "" : value;
        this.source = source == null ? "" : source;
    }

    static PortalValue of(String value, String source) {
        return new PortalValue(true, true, value, source);
    }

    static PortalValue missing(String source) {
        return new PortalValue(true, false, "", source);
    }

    static PortalValue readFailure(String source) {
        return new PortalValue(false, false, "", source);
    }

    static PortalValue fromSettingsOutput(String output, String source) {
        String clean = output == null ? "" : output.trim();
        if ("null".equals(clean)) {
            return missing(source);
        }
        return of(clean, source);
    }

    boolean sameDesired(String desired) {
        return readable && exists && value.equals(desired == null ? "" : desired);
    }

    boolean same(PortalValue other) {
        if (other == null) {
            return false;
        }
        return readable && other.readable && exists == other.exists && value.equals(other.value);
    }

    String display() {
        if (!readable) {
            return "(读取失败)";
        }
        return exists ? value : "(未设置)";
    }
}
