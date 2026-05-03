package com.fanli.wifiportal;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class PortalUserService extends IPortalShell.Stub {
    private static final long COMMAND_TIMEOUT_MS = 20000L;
    private static final long POLL_INTERVAL_MS = 50L;
    private static final long READER_JOIN_MS = 1000L;

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
            Process started = process;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thread reader = new Thread(() -> readInto(started.getInputStream(), output), "portal-shell-output");
            reader.setDaemon(true);
            reader.start();
            long deadline = System.currentTimeMillis() + COMMAND_TIMEOUT_MS;
            while (true) {
                Integer exitCode = exitCodeOrNull(process);
                if (exitCode != null) {
                    joinReader(reader);
                    return "exit=" + exitCode + "\n" + outputString(output);
                }
                if (System.currentTimeMillis() >= deadline) {
                    process.destroy();
                    joinReader(reader);
                    return "exit=124\n" + outputString(output) + "\nTimed out";
                }
                Thread.sleep(POLL_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "exit=130\nInterrupted";
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

    private static void readInto(InputStream inputStream, ByteArrayOutputStream output) {
        byte[] buffer = new byte[4096];
        int read;
        try {
            while ((read = inputStream.read(buffer)) != -1) {
                synchronized (output) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static Integer exitCodeOrNull(Process process) {
        try {
            return process.exitValue();
        } catch (IllegalThreadStateException ignored) {
            return null;
        }
    }

    private static void joinReader(Thread reader) throws InterruptedException {
        reader.join(READER_JOIN_MS);
    }

    private static String outputString(ByteArrayOutputStream output) throws IOException {
        synchronized (output) {
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
