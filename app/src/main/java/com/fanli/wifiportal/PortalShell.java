package com.fanli.wifiportal;

import android.os.RemoteException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PortalShell {
    private final IPortalShell shell;

    PortalShell(IPortalShell shell) {
        this.shell = shell;
    }

    Map<String, String> readAll(List<PortalSetting> settings, int sdk) throws RemoteException {
        Map<String, String> values = new LinkedHashMap<>();
        for (PortalSetting setting : settings) {
            if (sdk < setting.minSdk) {
                continue;
            }
            ShellText.ShellResult result = ShellText.parseResult(shell.exec(setting.readCommand()));
            if (result.ok()) {
                values.put(setting.id(), normalizeGetOutput(result.output));
            }
        }
        return values;
    }

    String applyAll(List<PortalSetting> settings, int sdk) throws RemoteException {
        StringBuilder script = new StringBuilder();
        for (PortalSetting setting : settings) {
            if (sdk < setting.minSdk) {
                continue;
            }
            script.append(setting.writeCommand()).append('\n');
        }
        ShellText.ShellResult result = ShellText.parseResult(shell.exec(script.toString()));
        if (!result.ok()) {
            return "应用失败，exit=" + result.exitCode + "\n" + result.output;
        }
        return result.output.length() == 0 ? "已应用配置" : result.output;
    }

    String sdk() throws RemoteException {
        ShellText.ShellResult result = ShellText.parseResult(shell.exec("getprop ro.build.version.sdk"));
        return result.output;
    }

    private static String normalizeGetOutput(String output) {
        String value = output == null ? "" : output.trim();
        if ("null".equals(value)) {
            return "";
        }
        return value;
    }
}
