package com.fanli.wifiportal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
    private static final String PREFS = "portal";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ShizukuPortalClient shizukuClient;
    private SharedPreferences prefs;
    private TextView statusView;
    private CheckBox disableDetection;
    private CheckBox includeLegacyServer;
    private CheckBox includeHttpUrl;
    private CheckBox includeHttpsUrl;
    private CheckBox includeFallbackUrl;
    private CheckBox includeOtherFallbackUrls;
    private CheckBox includeModernUrlLists;
    private CheckBox includeUseHttps;
    private CheckBox includeUserAgent;
    private CheckBox includeFallbackProbeSpecs;
    private CheckBox includeLegacyWifiWatchdog;
    private CheckBox useHttps;
    private EditText httpUrl;
    private EditText httpsUrl;
    private EditText fallbackUrl;
    private EditText otherFallbackUrls;
    private EditText otherHttpUrls;
    private EditText otherHttpsUrls;
    private EditText userAgent;
    private EditText fallbackProbeSpecs;
    private RadioGroup portalMode;
    private boolean mismatchDialogShowing;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> mainHandler.post(() -> {
                updateStatus("Shizuku 已激活，正在检查运行配置");
                if (!shizukuClient.hasPermission()) {
                    shizukuClient.requestPermission();
                } else {
                    checkPersistedConfig(true);
                }
            });

    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> mainHandler.post(() -> updateStatus("Shizuku 已断开"));

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
            (requestCode, grantResult) -> mainHandler.post(() -> {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    updateStatus("Shizuku 已授权，正在检查运行配置");
                    checkPersistedConfig(true);
                } else {
                    updateStatus("Shizuku 授权被拒绝，无法读取或写入全局配置");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        shizukuClient = new ShizukuPortalClient(this);
        buildUi();
        loadIntoUi(PortalConfig.load(prefs));
        Shizuku.addBinderReceivedListener(binderReceivedListener);
        Shizuku.addBinderDeadListener(binderDeadListener);
        Shizuku.addRequestPermissionResultListener(permissionResultListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshShizukuState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeBinderReceivedListener(binderReceivedListener);
        Shizuku.removeBinderDeadListener(binderDeadListener);
        Shizuku.removeRequestPermissionResultListener(permissionResultListener);
        executor.shutdownNow();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("WiFi Portal", 24, true);
        root.addView(title);
        statusView = text("", 14, false);
        statusView.setPadding(0, dp(8), 0, dp(12));
        root.addView(statusView);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(buttons, matchWrap());
        buttons.addView(button("检查", v -> saveAndCheck(false)), weightButton());
        buttons.addView(button("应用", v -> saveAndApply()), weightButton());
        buttons.addView(button("授权", v -> shizukuClient.requestPermission()), weightButton());
        buttons.addView(button("Shizuku", v -> openShizuku()), weightButton());

        root.addView(section("模式"));
        disableDetection = checkbox("全局禁用 captive portal 健康检测", false);
        root.addView(disableDetection);
        portalMode = new RadioGroup(this);
        portalMode.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton prompt = radio("提示登录", 1);
        RadioButton avoid = radio("避开网络", 2);
        portalMode.addView(prompt);
        portalMode.addView(avoid);
        root.addView(portalMode);

        root.addView(section("探测地址"));
        includeHttpUrl = checkbox("写入 HTTP 探测地址", true);
        root.addView(includeHttpUrl);
        httpUrl = edit("HTTP URL");
        root.addView(httpUrl);
        includeHttpsUrl = checkbox("写入 HTTPS 探测地址", true);
        root.addView(includeHttpsUrl);
        httpsUrl = edit("HTTPS URL");
        root.addView(httpsUrl);
        includeFallbackUrl = checkbox("写入 fallback 探测地址", true);
        root.addView(includeFallbackUrl);
        fallbackUrl = edit("Fallback URL");
        root.addView(fallbackUrl);

        root.addView(section("兼容项"));
        includeLegacyServer = checkbox("写入 Android 5/6 legacy server", true);
        includeOtherFallbackUrls = checkbox("写入其他 fallback URLs", true);
        includeModernUrlLists = checkbox("写入 Android 10+ HTTP/HTTPS URL 列表", true);
        includeUseHttps = checkbox("写入 HTTPS 校验开关", true);
        useHttps = checkbox("启用 HTTPS 校验", true);
        includeUserAgent = checkbox("写入 User-Agent", false);
        includeFallbackProbeSpecs = checkbox("写入 fallback probe specs", false);
        includeLegacyWifiWatchdog = checkbox("禁用旧版 Wi-Fi watchdog 项", false);
        root.addView(includeLegacyServer);
        root.addView(includeOtherFallbackUrls);
        otherFallbackUrls = edit("Other fallback URLs，逗号分隔");
        root.addView(otherFallbackUrls);
        root.addView(includeModernUrlLists);
        otherHttpUrls = edit("Other HTTP URLs，逗号分隔");
        root.addView(otherHttpUrls);
        otherHttpsUrls = edit("Other HTTPS URLs，逗号分隔");
        root.addView(otherHttpsUrls);
        root.addView(includeUseHttps);
        root.addView(useHttps);
        root.addView(includeUserAgent);
        userAgent = edit("User-Agent，可留空");
        root.addView(userAgent);
        root.addView(includeFallbackProbeSpecs);
        fallbackProbeSpecs = edit("url@@/@@statusRegex@@/@@contentRegex");
        fallbackProbeSpecs.setMinLines(2);
        root.addView(fallbackProbeSpecs);
        root.addView(includeLegacyWifiWatchdog);

        CompoundButton.OnCheckedChangeListener enabledUpdater = (buttonView, isChecked) -> updateFieldEnabledStates();
        disableDetection.setOnCheckedChangeListener(enabledUpdater);
        includeHttpUrl.setOnCheckedChangeListener(enabledUpdater);
        includeHttpsUrl.setOnCheckedChangeListener(enabledUpdater);
        includeFallbackUrl.setOnCheckedChangeListener(enabledUpdater);
        includeOtherFallbackUrls.setOnCheckedChangeListener(enabledUpdater);
        includeModernUrlLists.setOnCheckedChangeListener(enabledUpdater);
        includeUseHttps.setOnCheckedChangeListener(enabledUpdater);
        includeUserAgent.setOnCheckedChangeListener(enabledUpdater);
        includeFallbackProbeSpecs.setOnCheckedChangeListener(enabledUpdater);

        setContentView(scrollView);
    }

    private void refreshShizukuState() {
        if (!shizukuClient.isBinderAlive()) {
            updateStatus("Shizuku 未运行。Android 5 可使用 su/adb 手工执行同名 settings；应用内自动应用需要 Android 6+ Shizuku/Sui。");
            return;
        }
        int uid = shizukuClient.serverUid();
        String identity = uid == 0 ? "root" : uid == 2000 ? "shell/adb" : "uid=" + uid;
        updateStatus("Shizuku " + shizukuClient.serverVersionName() + " 已运行，身份 " + identity);
        if (!shizukuClient.hasPermission()) {
            shizukuClient.requestPermission();
            updateStatus("缺少 Shizuku 授权，已请求弹窗授权。");
        } else {
            checkPersistedConfig(true);
        }
    }

    private void saveAndCheck(boolean showDialogOnly) {
        PortalConfig config = readFromUi();
        String error = config.validate();
        if (error != null) {
            toast(error);
            return;
        }
        config.save(prefs);
        checkConfig(config, !showDialogOnly);
    }

    private void saveAndApply() {
        PortalConfig config = readFromUi();
        String error = config.validate();
        if (error != null) {
            toast(error);
            return;
        }
        config.save(prefs);
        applyConfig(config);
    }

    private void checkPersistedConfig(boolean showDialogOnMismatch) {
        checkConfig(PortalConfig.load(prefs), showDialogOnMismatch);
    }

    private void checkConfig(PortalConfig config, boolean showDialogOnMismatch) {
        List<PortalSetting> desired = PortalRules.desired(config, Build.VERSION.SDK_INT);
        updateStatus("正在读取系统 captive portal 配置");
        shizukuClient.withShell(new ShizukuPortalClient.ServiceCallback() {
            @Override
            public void onReady(PortalShell shell) {
                executor.execute(() -> {
                    try {
                        Map<String, String> current = shell.readAll(desired, Build.VERSION.SDK_INT);
                        List<PortalRules.Diff> diffs = PortalRules.diff(desired, current, Build.VERSION.SDK_INT);
                        mainHandler.post(() -> handleDiffs(diffs, config, showDialogOnMismatch));
                    } catch (Throwable e) {
                        mainHandler.post(() -> updateStatus("读取失败: " + e.getMessage()));
                    }
                });
            }

            @Override
            public void onError(String message) {
                updateStatus(message);
            }
        });
    }

    private void applyConfig(PortalConfig config) {
        List<PortalSetting> desired = PortalRules.desired(config, Build.VERSION.SDK_INT);
        updateStatus("正在应用 captive portal 配置");
        shizukuClient.withShell(new ShizukuPortalClient.ServiceCallback() {
            @Override
            public void onReady(PortalShell shell) {
                executor.execute(() -> {
                    try {
                        String output = shell.applyAll(desired, Build.VERSION.SDK_INT);
                        Map<String, String> current = shell.readAll(desired, Build.VERSION.SDK_INT);
                        List<PortalRules.Diff> diffs = PortalRules.diff(desired, current, Build.VERSION.SDK_INT);
                        mainHandler.post(() -> {
                            if (diffs.isEmpty()) {
                                updateStatus("已应用并验证一致");
                            } else {
                                updateStatus("已尝试应用，但仍有 " + diffs.size() + " 项不一致");
                                showMismatchDialog(diffs, config);
                            }
                            if (output.length() > 0 && !"已应用配置".equals(output)) {
                                Toast.makeText(MainActivity.this, output, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Throwable e) {
                        mainHandler.post(() -> updateStatus("应用失败: " + e.getMessage()));
                    }
                });
            }

            @Override
            public void onError(String message) {
                updateStatus(message);
            }
        });
    }

    private void handleDiffs(List<PortalRules.Diff> diffs, PortalConfig config, boolean showDialogOnMismatch) {
        if (diffs.isEmpty()) {
            updateStatus("运行配置与保存设置一致");
            return;
        }
        updateStatus("运行配置与保存设置不一致: " + diffs.size() + " 项");
        if (showDialogOnMismatch) {
            showMismatchDialog(diffs, config);
        }
    }

    private void showMismatchDialog(List<PortalRules.Diff> diffs, PortalConfig config) {
        if (mismatchDialogShowing) {
            return;
        }
        mismatchDialogShowing = true;
        StringBuilder message = new StringBuilder();
        int count = Math.min(diffs.size(), 8);
        for (int i = 0; i < count; i++) {
            PortalRules.Diff diff = diffs.get(i);
            message.append(diff.setting.label())
                    .append("\n当前: ").append(diff.actual.length() == 0 ? "(空)" : diff.actual)
                    .append("\n期望: ").append(diff.expected)
                    .append("\n\n");
        }
        if (diffs.size() > count) {
            message.append("另有 ").append(diffs.size() - count).append(" 项不一致。");
        }
        new AlertDialog.Builder(this)
                .setTitle("需要重新应用")
                .setMessage(message.toString())
                .setNegativeButton("稍后", null)
                .setPositiveButton("重新应用", (dialog, which) -> applyConfig(config))
                .setOnDismissListener(dialog -> mismatchDialogShowing = false)
                .show();
    }

    private PortalConfig readFromUi() {
        PortalConfig config = PortalConfig.defaults();
        config.disableDetection = disableDetection.isChecked();
        config.includeLegacyServer = includeLegacyServer.isChecked();
        config.includeHttpUrl = includeHttpUrl.isChecked();
        config.includeHttpsUrl = includeHttpsUrl.isChecked();
        config.includeFallbackUrl = includeFallbackUrl.isChecked();
        config.includeOtherFallbackUrls = includeOtherFallbackUrls.isChecked();
        config.includeModernUrlLists = includeModernUrlLists.isChecked();
        config.includeUseHttps = includeUseHttps.isChecked();
        config.includeUserAgent = includeUserAgent.isChecked();
        config.includeFallbackProbeSpecs = includeFallbackProbeSpecs.isChecked();
        config.includeLegacyWifiWatchdog = includeLegacyWifiWatchdog.isChecked();
        config.httpUrl = textOf(httpUrl);
        config.httpsUrl = textOf(httpsUrl);
        config.fallbackUrl = textOf(fallbackUrl);
        config.otherFallbackUrls = textOf(otherFallbackUrls);
        config.otherHttpUrls = textOf(otherHttpUrls);
        config.otherHttpsUrls = textOf(otherHttpsUrls);
        config.userAgent = textOf(userAgent);
        config.fallbackProbeSpecs = textOf(fallbackProbeSpecs);
        config.useHttps = useHttps.isChecked();
        int checked = portalMode.getCheckedRadioButtonId();
        config.portalMode = checked == 2 ? 2 : 1;
        return config;
    }

    private void loadIntoUi(PortalConfig config) {
        disableDetection.setChecked(config.disableDetection);
        includeLegacyServer.setChecked(config.includeLegacyServer);
        includeHttpUrl.setChecked(config.includeHttpUrl);
        includeHttpsUrl.setChecked(config.includeHttpsUrl);
        includeFallbackUrl.setChecked(config.includeFallbackUrl);
        includeOtherFallbackUrls.setChecked(config.includeOtherFallbackUrls);
        includeModernUrlLists.setChecked(config.includeModernUrlLists);
        includeUseHttps.setChecked(config.includeUseHttps);
        includeUserAgent.setChecked(config.includeUserAgent);
        includeFallbackProbeSpecs.setChecked(config.includeFallbackProbeSpecs);
        includeLegacyWifiWatchdog.setChecked(config.includeLegacyWifiWatchdog);
        httpUrl.setText(config.httpUrl);
        httpsUrl.setText(config.httpsUrl);
        fallbackUrl.setText(config.fallbackUrl);
        otherFallbackUrls.setText(config.otherFallbackUrls);
        otherHttpUrls.setText(config.otherHttpUrls);
        otherHttpsUrls.setText(config.otherHttpsUrls);
        userAgent.setText(config.userAgent);
        fallbackProbeSpecs.setText(config.fallbackProbeSpecs);
        useHttps.setChecked(config.useHttps);
        portalMode.check(config.portalMode == 2 ? 2 : 1);
        updateFieldEnabledStates();
    }

    private void updateFieldEnabledStates() {
        boolean detectionEnabled = !disableDetection.isChecked();
        portalMode.setEnabled(detectionEnabled);
        for (int i = 0; i < portalMode.getChildCount(); i++) {
            portalMode.getChildAt(i).setEnabled(detectionEnabled);
        }
        httpUrl.setEnabled(detectionEnabled && includeHttpUrl.isChecked());
        httpsUrl.setEnabled(detectionEnabled && includeHttpsUrl.isChecked());
        fallbackUrl.setEnabled(detectionEnabled && includeFallbackUrl.isChecked());
        otherFallbackUrls.setEnabled(detectionEnabled && includeOtherFallbackUrls.isChecked());
        otherHttpUrls.setEnabled(detectionEnabled && includeModernUrlLists.isChecked());
        otherHttpsUrls.setEnabled(detectionEnabled && includeModernUrlLists.isChecked());
        useHttps.setEnabled(detectionEnabled && includeUseHttps.isChecked());
        userAgent.setEnabled(detectionEnabled && includeUserAgent.isChecked());
        fallbackProbeSpecs.setEnabled(detectionEnabled && includeFallbackProbeSpecs.isChecked());
    }

    private void openShizuku() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        if (launchIntent != null) {
            startActivity(launchIntent);
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")));
    }

    private void updateStatus(String message) {
        statusView.setText("Android API " + Build.VERSION.SDK_INT + " · " + message);
    }

    private TextView section(String label) {
        TextView view = text(label, 17, true);
        view.setPadding(0, dp(18), 0, dp(6));
        return view;
    }

    private TextView text(String label, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(sp);
        view.setTextColor(0xFF1F2328);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private CheckBox checkbox(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextSize(15);
        box.setChecked(checked);
        return box;
    }

    private RadioButton radio(String label, int id) {
        RadioButton button = new RadioButton(this);
        button.setText(label);
        button.setId(id);
        return button;
    }

    private EditText edit(String hint) {
        EditText view = new EditText(this);
        view.setHint(hint);
        view.setTextSize(14);
        view.setSingleLine(false);
        view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private Button button(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams weightButton() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
