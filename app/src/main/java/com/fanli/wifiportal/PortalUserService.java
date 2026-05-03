package com.fanli.wifiportal;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class PortalUserService extends IPortalShell.Stub {
    public PortalUserService() {
    }

    public PortalUserService(Context context) {
    }

    @Override
    public String exec(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(20, TimeUnit.SECONDS);
            String output = readFully(process.getInputStream());
            if (!finished) {
                process.destroyForcibly();
                return "exit=124\n" + output + "\nTimed out";
            }
            return "exit=" + process.exitValue() + "\n" + output;
        } catch (Throwable e) {
            return "exit=1\n" + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    private static String readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
