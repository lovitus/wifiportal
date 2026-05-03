package com.fanli.wifiportal;

final class ShellText {
    private ShellText() {
    }

    static String quote(String value) {
        if (value == null || value.length() == 0) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    static ShellResult parseResult(String raw) {
        if (raw == null) {
            return new ShellResult(-1, "");
        }
        String[] lines = raw.split("\\r?\\n", 2);
        if (lines.length > 0 && lines[0].startsWith("exit=")) {
            try {
                int code = Integer.parseInt(lines[0].substring(5).trim());
                String output = lines.length > 1 ? lines[1] : "";
                return new ShellResult(code, output.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return new ShellResult(0, raw.trim());
    }

    static final class ShellResult {
        final int exitCode;
        final String output;

        ShellResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }

        boolean ok() {
            return exitCode == 0;
        }
    }
}
