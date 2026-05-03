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

    Map<String, PortalValue> readAll(List<PortalSetting> settings, int sdk) throws RemoteException {
        Map<String, PortalValue> values = new LinkedHashMap<>();
        for (PortalSetting setting : settings) {
            if (sdk < setting.minSdk) {
                continue;
            }
            String command = setting.readCommand();
            ShellText.ShellResult result = ShellText.parseResult(shell.exec(command));
            if (result.ok()) {
                values.put(setting.id(), PortalValue.fromSettingsOutput(result.output, "Shizuku UserService: " + command));
            } else {
                values.put(setting.id(), PortalValue.missing("读取失败: " + command + " exit=" + result.exitCode));
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

    String runCommands(List<String> commands) throws RemoteException {
        StringBuilder script = new StringBuilder();
        for (String command : commands) {
            script.append(command).append('\n');
        }
        ShellText.ShellResult result = ShellText.parseResult(shell.exec(script.toString()));
        if (!result.ok()) {
            return "执行失败，exit=" + result.exitCode + "\n" + result.output;
        }
        return result.output.length() == 0 ? "命令已执行" : result.output;
    }

    String sdk() throws RemoteException {
        ShellText.ShellResult result = ShellText.parseResult(shell.exec("getprop ro.build.version.sdk"));
        return result.output;
    }

}
