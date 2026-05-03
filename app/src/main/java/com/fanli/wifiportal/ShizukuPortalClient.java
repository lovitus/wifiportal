package com.fanli.wifiportal;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

final class ShizukuPortalClient {
    interface ServiceCallback {
        void onReady(PortalShell shell);

        void onError(String message);
    }

    private static final int REQUEST_CODE = 1001;

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ServiceCallback> pendingCallbacks = new ArrayList<>();
    private IPortalShell shell;
    private boolean binding;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            shell = IPortalShell.Stub.asInterface(service);
            binding = false;
            flushReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            shell = null;
            binding = false;
        }
    };

    ShizukuPortalClient(Context context) {
        this.context = context.getApplicationContext();
    }

    boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean hasPermission() {
        try {
            return !Shizuku.isPreV11()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    int serverUid() {
        try {
            return Shizuku.getUid();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    String serverVersionName() {
        try {
            int version = Shizuku.getVersion();
            int patch = Shizuku.getServerPatchVersion();
            return patch > 0 ? "v" + version + "." + patch : "v" + version;
        } catch (Throwable ignored) {
            return "";
        }
    }

    void requestPermission() {
        try {
            if (!Shizuku.isPreV11() && !Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(REQUEST_CODE);
            }
        } catch (Throwable ignored) {
        }
    }

    void withShell(ServiceCallback callback) {
        if (!isBinderAlive()) {
            callback.onError("Shizuku 未运行。请先启动 Shizuku/Sui。");
            return;
        }
        if (!hasPermission()) {
            requestPermission();
            callback.onError("缺少 Shizuku 授权，已请求弹窗授权。");
            return;
        }
        if (shell != null) {
            callback.onReady(new PortalShell(shell));
            return;
        }
        pendingCallbacks.add(callback);
        if (binding) {
            return;
        }
        binding = true;
        try {
            ComponentName componentName = new ComponentName(context, PortalUserService.class);
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(componentName)
                    .daemon(false)
                    .processNameSuffix("portal")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE)
                    .tag("wifiportal-shell");
            Shizuku.bindUserService(args, connection);
        } catch (Throwable e) {
            binding = false;
            flushError("无法绑定 Shizuku UserService: " + e.getMessage());
        }
    }

    void destroy() {
        IPortalShell current = shell;
        shell = null;
        if (current != null) {
            try {
                current.destroy();
            } catch (Throwable ignored) {
            }
        }
    }

    private void flushReady() {
        List<ServiceCallback> callbacks = new ArrayList<>(pendingCallbacks);
        pendingCallbacks.clear();
        PortalShell portalShell = new PortalShell(shell);
        for (ServiceCallback callback : callbacks) {
            mainHandler.post(() -> callback.onReady(portalShell));
        }
    }

    private void flushError(String message) {
        List<ServiceCallback> callbacks = new ArrayList<>(pendingCallbacks);
        pendingCallbacks.clear();
        for (ServiceCallback callback : callbacks) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
}
