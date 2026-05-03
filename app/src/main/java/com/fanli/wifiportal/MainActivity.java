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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
    private static final String PREFS = "portal";
    private static final String AUDIT_LOG = "auditLog";
    private static final int COLOR_OK = 0xFF0B6E4F;
    private static final int COLOR_FAIL = 0xFFB3261E;
    private static final int COLOR_WAIT = 0xFF7A4D00;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ShizukuPortalClient shizukuClient;
    private SharedPreferences prefs;
    private TextView shizukuStatusView;
    private TextView statusView;
    private TextView coverageView;
    private TextView auditView;
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
    private Spinner settingSpinner;
    private final List<PortalSetting> selectableSettings = new ArrayList<>();
    private boolean mismatchDialogShowing;

    private final Shizuku.OnBinderReceivedListener binderReceivedListener =
            () -> mainHandler.post(() -> {
                setShizukuStatus(COLOR_WAIT, "Shizuku 已激活，正在确认授权");
                if (!shizukuClient.hasPermission()) {
                    boolean requested = shizukuClient.requestPermission();
                    setShizukuStatus(COLOR_WAIT, requested
                            ? "Shizuku 已激活，但缺少授权，已请求授权"
                            : "Shizuku 已激活，但缺少授权，请在 Shizuku 中手动授权");
                } else {
                    setShizukuStatus(COLOR_OK, "Shizuku 已授权，可读取和写入系统配置");
                    readRuntimeAndRefresh(true);
                }
            });

    private final Shizuku.OnBinderDeadListener binderDeadListener =
            () -> mainHandler.post(() -> setShizukuStatus(COLOR_FAIL, "Shizuku 已断开，无法读取或写入"));

    private final Shizuku.OnRequestPermissionResultListener permissionResultListener =
            (requestCode, grantResult) -> mainHandler.post(() -> {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    setShizukuStatus(COLOR_OK, "Shizuku 授权成功，可读取和写入系统配置");
                    appendAudit("Shizuku 授权结果: 成功");
                    readRuntimeAndRefresh(true);
                } else {
                    setShizukuStatus(COLOR_FAIL, "Shizuku 授权失败，所有系统读取和写入都被阻止");
                    appendAudit("Shizuku 授权结果: 失败");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        shizukuClient = new ShizukuPortalClient(this);
        buildUi();
        loadIntoUi(PortalConfig.load(prefs));
        auditView.setText(prefs.getString(AUDIT_LOG, ""));
        refreshCoverageFromSnapshots();
        refreshSettingSpinner();
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
        shizukuClient.destroy();
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
        shizukuStatusView = text("", 15, true);
        shizukuStatusView.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.addView(shizukuStatusView, matchWrap());
        statusView = text("", 14, false);
        statusView.setPadding(0, dp(8), 0, dp(12));
        root.addView(statusView);

        LinearLayout rowOne = buttonRow(root);
        rowOne.addView(button("保存方案", v -> saveLocalConfig()), weightButton());
        rowOne.addView(button("读取系统", v -> readRuntimeAndRefresh(false)), weightButton());
        rowOne.addView(button("审计写入", v -> prepareDesiredWrite(null)), weightButton());
        rowOne.addView(button("授权", v -> shizukuClient.requestPermission()), weightButton());

        LinearLayout rowTwo = buttonRow(root);
        rowTwo.addView(button("备份默认", v -> backupDefaultsWithReview()), weightButton());
        rowTwo.addView(button("恢复全部", v -> prepareRestore(null)), weightButton());
        rowTwo.addView(button("单项写入", v -> prepareDesiredWrite(selectedSetting())), weightButton());
        rowTwo.addView(button("单项恢复", v -> prepareRestore(selectedSetting())), weightButton());

        LinearLayout rowThree = buttonRow(root);
        rowThree.addView(button("中国优化", v -> prepareChinaOptimization()), weightButton());
        rowThree.addView(button("Shizuku", v -> openShizuku()), weightButton());
        rowThree.addView(button("清空日志", v -> clearAuditLog()), weightButton());

        root.addView(section("单项"));
        settingSpinner = new Spinner(this);
        root.addView(settingSpinner, matchWrap());

        root.addView(section("覆盖项"));
        coverageView = logView();
        root.addView(coverageView, matchWrap());

        root.addView(section("模式"));
        disableDetection = checkbox("全局禁用 captive portal 健康检测", false);
        root.addView(disableDetection);
        portalMode = new RadioGroup(this);
        portalMode.setOrientation(RadioGroup.HORIZONTAL);
        portalMode.addView(radio("提示登录", 1));
        portalMode.addView(radio("避开网络", 2));
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

        root.addView(section("审计日志"));
        auditView = logView();
        root.addView(auditView, matchWrap());

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
            setShizukuStatus(COLOR_FAIL, "Shizuku 未运行，不能读取或写入系统配置");
            updateStatus("Android 5 可安装应用和保存方案；应用内特权操作需要 Shizuku/Sui。");
            return;
        }
        int uid = shizukuClient.serverUid();
        String identity = uid == 0 ? "root" : uid == 2000 ? "shell/adb" : "uid=" + uid;
        if (!shizukuClient.hasPermission()) {
            setShizukuStatus(COLOR_WAIT, "Shizuku 已运行但未授权，身份 " + identity);
            boolean requested = shizukuClient.requestPermission();
            appendAudit(requested
                    ? "Shizuku 状态: 已运行但未授权，已请求授权"
                    : "Shizuku 状态: 已运行但未授权，请在 Shizuku 中手动授权");
            return;
        }
        setShizukuStatus(COLOR_OK, "Shizuku 已授权，身份 " + identity + "，版本 " + shizukuClient.serverVersionName());
        readRuntimeAndRefresh(true);
    }

    private void saveLocalConfig() {
        PortalConfig config = readFromUi();
        String error = config.validate();
        if (error != null) {
            toast(error);
            return;
        }
        config.save(prefs);
        appendAudit("程序配置来源: 页面输入 -> SharedPreferences(" + PREFS + ")，只保存本地方案，未写入系统");
        updateStatus("方案已保存，正在检测运行值是否一致");
        refreshCoverageFromSnapshots();
        refreshSettingSpinner();
        if (shizukuClient.isBinderAlive() && shizukuClient.hasPermission()) {
            readRuntimeAndRefresh(false);
        } else {
            updateStatus("方案已保存；Shizuku 未授权或未运行，暂不能检测运行值");
            appendAudit("检测跳过: Shizuku 未授权或未运行，显示使用最近一次运行值快照");
        }
    }

    private void readRuntimeAndRefresh(boolean showMismatchOnOpen) {
        PortalConfig config = PortalConfig.load(prefs);
        List<PortalSetting> catalog = PortalRules.catalog(Build.VERSION.SDK_INT);
        List<PortalSetting> desired = PortalRules.desired(config, Build.VERSION.SDK_INT);
        updateStatus("正在读取系统配置并刷新覆盖项");
        withShell(shell -> executor.execute(() -> {
            try {
                Map<String, PortalValue> current = shell.readAll(catalog, Build.VERSION.SDK_INT);
                int backedUp = PortalSnapshots.backupMissingOriginals(prefs, catalog, current);
                PortalSnapshots.saveCurrent(prefs, current);
                List<PortalRules.Diff> diffs = PortalRules.diff(desired, current, Build.VERSION.SDK_INT);
                mainHandler.post(() -> {
                    appendAudit("运行时数据来源: Shizuku UserService(" + shizukuIdentity() + ")");
                    appendAudit("读取命令:\n" + readCommands(catalog));
                    if (backedUp > 0) {
                        appendAudit("默认备份: 首次保存 " + backedUp + " 项原始系统值");
                    }
                    refreshCoverage(desired, current);
                    handleDiffs(diffs, config, showMismatchOnOpen);
                });
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    updateStatus("读取失败: " + e.getMessage());
                    appendAudit("运行时读取失败: " + e.getMessage());
                });
            }
        }));
    }

    private void backupDefaultsWithReview() {
        List<PortalSetting> catalog = PortalRules.catalog(Build.VERSION.SDK_INT);
        updateStatus("正在读取系统值，准备备份为默认值");
        withShell(shell -> executor.execute(() -> {
            try {
                Map<String, PortalValue> current = shell.readAll(catalog, Build.VERSION.SDK_INT);
                mainHandler.post(() -> showBackupDialog(catalog, current));
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    updateStatus("备份读取失败: " + e.getMessage());
                    appendAudit("默认备份读取失败: " + e.getMessage());
                });
            }
        }));
    }

    private void prepareDesiredWrite(PortalSetting onlySetting) {
        PortalConfig config = readFromUi();
        String error = config.validate();
        if (error != null) {
            toast(error);
            return;
        }
        List<PortalSetting> catalog = PortalRules.catalog(Build.VERSION.SDK_INT);
        List<PortalSetting> desired = PortalRules.desired(config, Build.VERSION.SDK_INT);
        if (onlySetting != null && !containsSetting(desired, onlySetting)) {
            toast("当前保存方案没有覆盖该单项");
            appendAudit("单项写入取消: 当前保存方案没有覆盖 " + onlySetting.label());
            refreshCoverageFromSnapshots();
            return;
        }
        updateStatus("正在生成写入审核计划");
        withShell(shell -> executor.execute(() -> {
            try {
                Map<String, PortalValue> current = shell.readAll(catalog, Build.VERSION.SDK_INT);
                int backedUp = PortalSnapshots.backupMissingOriginals(prefs, catalog, current);
                PortalSnapshots.saveCurrent(prefs, current);
                List<PortalWritePlan.Item> items = PortalRules.desiredPlanItems(
                        desired,
                        current,
                        prefs,
                        Build.VERSION.SDK_INT,
                        onlySetting,
                        onlySetting == null
                                ? "页面待应用方案: 确认执行且回读一致后保存"
                                : "页面待应用单项: 只写入该项，不保存整套方案");
                PortalWritePlan plan = new PortalWritePlan(onlySetting == null ? "写入保存方案" : "写入单项", items);
                mainHandler.post(() -> {
                    if (backedUp > 0) {
                        appendAudit("默认备份: 写入前首次保存 " + backedUp + " 项原始系统值");
                    }
                    refreshCoverage(desired, current);
                    showWritePlan(plan, desired, onlySetting == null ? config : null);
                });
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    updateStatus("生成写入计划失败: " + e.getMessage());
                    appendAudit("生成写入计划失败: " + e.getMessage());
                });
            }
        }));
    }

    private void prepareRestore(PortalSetting onlySetting) {
        List<PortalSetting> catalog = PortalRules.catalog(Build.VERSION.SDK_INT);
        List<PortalSetting> desired = PortalRules.desired(PortalConfig.load(prefs), Build.VERSION.SDK_INT);
        updateStatus(onlySetting == null ? "正在生成全局恢复审核计划" : "正在生成单项恢复审核计划");
        withShell(shell -> executor.execute(() -> {
            try {
                Map<String, PortalValue> current = shell.readAll(catalog, Build.VERSION.SDK_INT);
                PortalSnapshots.saveCurrent(prefs, current);
                List<PortalWritePlan.Item> items = PortalRules.restorePlanItems(
                        catalog, current, prefs, Build.VERSION.SDK_INT, onlySetting);
                PortalWritePlan plan = new PortalWritePlan(onlySetting == null ? "恢复全部默认" : "恢复单项默认", items);
                mainHandler.post(() -> {
                    refreshCoverage(desired, current);
                    if (plan.isEmpty() && onlySetting != null && !PortalSnapshots.hasOriginal(prefs, onlySetting)) {
                        updateStatus("该项没有原始备份，不能恢复默认");
                        appendAudit("恢复取消: 没有原始备份 " + onlySetting.label());
                        return;
                    }
                    showWritePlan(plan, desired, null);
                });
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    updateStatus("生成恢复计划失败: " + e.getMessage());
                    appendAudit("生成恢复计划失败: " + e.getMessage());
                });
            }
        }));
    }

    private void prepareChinaOptimization() {
        PortalConfig storedConfig = PortalConfig.load(prefs);
        PortalConfig recommendedConfig = PortalConfig.mainlandChina();
        List<PortalSetting> catalog = PortalRules.catalog(Build.VERSION.SDK_INT);
        List<PortalSetting> storedDesired = PortalRules.desired(storedConfig, Build.VERSION.SDK_INT);
        List<PortalSetting> recommendedDesired = PortalRules.desired(recommendedConfig, Build.VERSION.SDK_INT);
        updateStatus("正在读取系统值，准备中国大陆优化审核");
        withShell(shell -> executor.execute(() -> {
            try {
                Map<String, PortalValue> current = shell.readAll(catalog, Build.VERSION.SDK_INT);
                int backedUp = PortalSnapshots.backupMissingOriginals(prefs, catalog, current);
                PortalSnapshots.saveCurrent(prefs, current);
                List<PortalWritePlan.Item> items = PortalRules.desiredPlanItems(
                        recommendedDesired,
                        current,
                        prefs,
                        Build.VERSION.SDK_INT,
                        null,
                        "中国大陆推荐方案: MIUI + 华为 204 端点");
                PortalWritePlan plan = new PortalWritePlan("中国大陆一键优化", items);
                mainHandler.post(() -> {
                    if (backedUp > 0) {
                        appendAudit("默认备份: 中国优化前首次保存 " + backedUp + " 项原始系统值");
                    }
                    refreshCoverage(storedDesired, current);
                    showChinaOptimizationDialog(plan, storedDesired, recommendedDesired, current, recommendedConfig);
                });
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    updateStatus("中国优化审核失败: " + e.getMessage());
                    appendAudit("中国优化审核失败: " + e.getMessage());
                });
            }
        }));
    }

    private void showChinaOptimizationDialog(
            PortalWritePlan plan,
            List<PortalSetting> storedDesired,
            List<PortalSetting> recommendedDesired,
            Map<String, PortalValue> current,
            PortalConfig recommendedConfig) {
        StringBuilder body = new StringBuilder();
        body.append("推荐来源: 内置中国大陆方案，使用 connect.rom.miui.com 与 connectivitycheck.platform.hicloud.com 的 204 端点。\n");
        body.append("确认后先执行下列命令，回读运行值与推荐值一致后才保存为本地方案。\n\n");
        for (PortalSetting recommended : recommendedDesired) {
            PortalSetting stored = findSetting(storedDesired, recommended);
            PortalValue running = current.get(recommended.id());
            PortalValue original = PortalSnapshots.original(prefs, recommended);
            body.append(recommended.label())
                    .append('\n')
                    .append("原始: ").append(original == null ? "(未备份)" : original.display()).append('\n')
                    .append("存储: ").append(stored == null ? "(保存方案未覆盖)" : stored.value).append('\n')
                    .append("运行: ").append(running == null ? "(未读取)" : running.display()).append('\n')
                    .append("推荐: ").append(recommended.value).append('\n')
                    .append("命令: ").append(recommended.writeCommand()).append("\n\n");
        }
        if (plan.isEmpty()) {
            body.append("运行值已经与推荐值一致。确认后只保存推荐方案并刷新显示。");
        } else {
            body.append("实际待执行命令:\n").append(joinCommands(plan.commands()));
        }
        appendAudit("中国大陆优化审核:\n" + body);
        showScrollableDialog(
                "中国大陆一键优化",
                body.toString(),
                "取消",
                "确认应用",
                () -> {
                    if (plan.isEmpty()) {
                        recommendedConfig.save(prefs);
                        loadIntoUi(recommendedConfig);
                        appendAudit("中国大陆优化: 运行值已一致，已保存推荐方案");
                        updateStatus("中国大陆推荐方案已保存，运行/显示/保存一致");
                        refreshCoverage(recommendedDesired, current);
                    } else {
                        executePlan(plan, recommendedDesired, recommendedConfig);
                    }
                });
    }

    private void showWritePlan(PortalWritePlan plan, List<PortalSetting> desiredAfterExecute, PortalConfig saveAfterSuccess) {
        if (plan.isEmpty()) {
            if (saveAfterSuccess != null) {
                saveAfterSuccess.save(prefs);
                loadIntoUi(saveAfterSuccess);
                refreshCoverageFromSnapshots();
                updateStatus(plan.title + ": 当前运行值已一致，方案已保存，运行/显示/保存一致");
                appendAudit(plan.title + ": 无待执行命令，已保存已验证一致的方案");
                return;
            }
            updateStatus(plan.title + ": 无需写入，当前值已一致或没有可恢复备份");
            appendAudit(plan.title + ": 无待执行命令");
            return;
        }
        String commands = joinCommands(plan.commands());
        appendAudit("待写入数据审核: " + plan.title + "\n" + commands);
        showScrollableDialog(
                plan.title,
                plan.describeForReview(),
                "取消",
                "确认执行",
                () -> executePlan(plan, desiredAfterExecute, saveAfterSuccess));
    }

    private void executePlan(PortalWritePlan plan, List<PortalSetting> desiredAfterExecute, PortalConfig saveAfterSuccess) {
        updateStatus("正在执行已审核命令");
        withShell(shell -> executor.execute(() -> {
            try {
                String output = shell.runCommands(plan.commands());
                List<PortalSetting> catalog = PortalRules.catalog(Build.VERSION.SDK_INT);
                Map<String, PortalValue> current = shell.readAll(catalog, Build.VERSION.SDK_INT);
                PortalSnapshots.saveCurrent(prefs, current);
                List<PortalWritePlan.Item> mismatches = plan.mismatches(current);
                mainHandler.post(() -> {
                    appendAudit("已执行命令:\n" + joinCommands(plan.commands()));
                    if (output.length() > 0 && !"命令已执行".equals(output)) {
                        appendAudit("命令输出:\n" + output);
                    }
                    if (mismatches.isEmpty() && saveAfterSuccess != null) {
                        saveAfterSuccess.save(prefs);
                        loadIntoUi(saveAfterSuccess);
                        appendAudit("程序配置来源: 已验证运行值一致 -> 保存推荐方案到 SharedPreferences(" + PREFS + ")");
                    }
                    refreshCoverage(desiredAfterExecute, current);
                    if (mismatches.isEmpty()) {
                        if (saveAfterSuccess != null) {
                            updateStatus("命令已执行，运行/显示/保存已检测一致");
                        } else {
                            updateStatus("命令已执行并回读验证，覆盖项显示已更新");
                        }
                    } else {
                        updateStatus("命令已执行，但回读仍有 " + mismatches.size() + " 项不一致；未保存新的推荐方案");
                        appendAudit("回读不一致:\n" + describePlanMismatch(mismatches, current));
                    }
                });
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    updateStatus("执行失败: " + e.getMessage());
                    appendAudit("执行失败: " + e.getMessage());
                });
            }
        }));
    }

    private void handleDiffs(List<PortalRules.Diff> diffs, PortalConfig config, boolean showDialogOnMismatch) {
        if (diffs.isEmpty()) {
            updateStatus("运行配置与保存方案一致");
            return;
        }
        updateStatus("运行配置与保存方案不一致: " + diffs.size() + " 项。请先审计写入计划再执行。");
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
                    .append("\n当前: ").append(diff.actual.display())
                    .append("\n期望: ").append(diff.expected.display())
                    .append("\n\n");
        }
        if (diffs.size() > count) {
            message.append("另有 ").append(diffs.size() - count).append(" 项不一致。\n");
        }
        message.append("不会自动写入。点击“打开审核”后会显示所有即将执行的命令。");
        new AlertDialog.Builder(this)
                .setTitle("运行配置不一致")
                .setMessage(message.toString())
                .setNegativeButton("稍后", null)
                .setPositiveButton("打开审核", (dialog, which) -> {
                    loadIntoUi(config);
                    prepareDesiredWrite(null);
                })
                .setOnDismissListener(dialog -> mismatchDialogShowing = false)
                .show();
    }

    private void showBackupDialog(List<PortalSetting> catalog, Map<String, PortalValue> current) {
        StringBuilder body = new StringBuilder();
        body.append("这会把当前系统值保存为“原始默认值”，用于以后恢复默认。不会执行系统写入命令。\n\n");
        for (PortalSetting setting : catalog) {
            PortalValue value = current.get(setting.id());
            body.append(setting.label())
                    .append("\n当前: ")
                    .append(value == null ? "(未读取)" : value.display())
                    .append("\n来源: ")
                    .append(value == null ? "(无)" : value.source)
                    .append("\n\n");
        }
        appendAudit("默认备份审核: 当前系统值来自 Shizuku UserService\n读取命令:\n" + readCommands(catalog));
        showScrollableDialog(
                "备份当前系统值为默认",
                body.toString(),
                "取消",
                "确认备份",
                () -> {
                    int saved = PortalSnapshots.overwriteOriginals(prefs, current);
                    PortalSnapshots.saveCurrent(prefs, current);
                    appendAudit("默认备份: 用户确认后覆盖保存 " + saved + " 项原始值，跳过读取失败项");
                    updateStatus("默认备份已保存");
                    refreshCoverageFromSnapshots();
                });
    }

    private void refreshCoverageFromSnapshots() {
        PortalConfig config = PortalConfig.load(prefs);
        List<PortalSetting> desired = PortalRules.desired(config, Build.VERSION.SDK_INT);
        Map<String, PortalValue> current = PortalSnapshots.currentFor(prefs, desired);
        refreshCoverage(desired, current);
    }

    private void refreshCoverage(List<PortalSetting> desired, Map<String, PortalValue> current) {
        StringBuilder builder = new StringBuilder();
        builder.append("程序配置来源: SharedPreferences(").append(PREFS).append(")\n");
        builder.append("运行时数据来源: ");
        builder.append(current.isEmpty() ? "未读取" : "Shizuku UserService / 最近一次读取");
        builder.append("\n\n");
        for (PortalSetting setting : desired) {
            PortalValue original = PortalSnapshots.original(prefs, setting);
            PortalValue currentValue = current.get(setting.id());
            if (currentValue == null) {
                currentValue = PortalSnapshots.current(prefs, setting);
            }
            String state;
            if (currentValue == null) {
                state = "未读取";
            } else if (currentValue.sameDesired(setting.value)) {
                state = "一致";
            } else {
                state = "将覆盖";
            }
            builder.append(setting.label())
                    .append(" [").append(state).append("]\n")
                    .append("原始: ").append(original == null ? "(未备份)" : original.display()).append('\n')
                    .append("当前: ").append(currentValue == null ? "(未读取)" : currentValue.display()).append('\n')
                    .append("目标: ").append(setting.value).append('\n')
                    .append("命令: ").append(setting.writeCommand()).append("\n\n");
        }
        coverageView.setText(builder.toString());
    }

    private void refreshSettingSpinner() {
        selectableSettings.clear();
        selectableSettings.addAll(PortalRules.catalog(Build.VERSION.SDK_INT));
        List<String> labels = new ArrayList<>();
        for (PortalSetting setting : selectableSettings) {
            labels.add(setting.label());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        settingSpinner.setAdapter(adapter);
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

    private void withShell(ShellConsumer consumer) {
        shizukuClient.withShell(new ShizukuPortalClient.ServiceCallback() {
            @Override
            public void onReady(PortalShell shell) {
                consumer.accept(shell);
            }

            @Override
            public void onError(String message) {
                setShizukuStatus(COLOR_FAIL, message);
                updateStatus(message);
                appendAudit("Shizuku 操作失败: " + message);
            }
        });
    }

    private void openShizuku() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
        if (launchIntent != null) {
            startActivity(launchIntent);
            return;
        }
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")));
    }

    private void setShizukuStatus(int color, String message) {
        shizukuStatusView.setText(message);
        shizukuStatusView.setTextColor(color);
        int background = color == COLOR_OK ? 0xFFEAF7EF : color == COLOR_FAIL ? 0xFFFDECEC : 0xFFFFF7E6;
        shizukuStatusView.setBackgroundColor(background);
    }

    private void updateStatus(String message) {
        statusView.setText(getString(R.string.status_format, Build.VERSION.SDK_INT, message));
    }

    private void appendAudit(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String existing = auditView == null ? prefs.getString(AUDIT_LOG, "") : auditView.getText().toString();
        String next = existing + time + "  " + message + "\n\n";
        if (next.length() > 16000) {
            next = next.substring(next.length() - 16000);
        }
        if (auditView != null) {
            auditView.setText(next);
        }
        prefs.edit().putString(AUDIT_LOG, next).apply();
    }

    private void clearAuditLog() {
        auditView.setText("");
        prefs.edit().remove(AUDIT_LOG).apply();
        updateStatus("审计日志已清空");
    }

    private String shizukuIdentity() {
        int uid = shizukuClient.serverUid();
        String identity = uid == 0 ? "root" : uid == 2000 ? "shell/adb" : "uid=" + uid;
        return identity + " " + shizukuClient.serverVersionName();
    }

    private String readCommands(List<PortalSetting> settings) {
        StringBuilder builder = new StringBuilder();
        for (PortalSetting setting : settings) {
            builder.append(setting.readCommand()).append('\n');
        }
        return builder.toString();
    }

    private String joinCommands(List<String> commands) {
        StringBuilder builder = new StringBuilder();
        for (String command : commands) {
            builder.append(command).append('\n');
        }
        return builder.toString();
    }

    private boolean containsSetting(List<PortalSetting> settings, PortalSetting target) {
        for (PortalSetting setting : settings) {
            if (setting.id().equals(target.id())) {
                return true;
            }
        }
        return false;
    }

    private PortalSetting findSetting(List<PortalSetting> settings, PortalSetting target) {
        for (PortalSetting setting : settings) {
            if (setting.id().equals(target.id())) {
                return setting;
            }
        }
        return null;
    }

    private String describePlanMismatch(List<PortalWritePlan.Item> mismatches, Map<String, PortalValue> current) {
        StringBuilder builder = new StringBuilder();
        for (PortalWritePlan.Item item : mismatches) {
            PortalValue actual = current.get(item.setting.id());
            builder.append(item.setting.label())
                    .append("\n当前: ").append(actual == null ? "(未读取)" : actual.display())
                    .append("\n目标: ").append(item.target.display())
                    .append('\n');
        }
        return builder.toString();
    }

    private PortalSetting selectedSetting() {
        int index = settingSpinner.getSelectedItemPosition();
        if (index < 0 || index >= selectableSettings.size()) {
            return null;
        }
        return selectableSettings.get(index);
    }

    private void showScrollableDialog(String title, String body, String negative, String positive, Runnable positiveAction) {
        ScrollView scrollView = new ScrollView(this);
        TextView bodyView = logView();
        bodyView.setText(body);
        int pad = dp(12);
        bodyView.setPadding(pad, pad, pad, pad);
        scrollView.addView(bodyView, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(scrollView)
                .setNegativeButton(negative, null)
                .setPositiveButton(positive, (dialog, which) -> positiveAction.run())
                .show();
    }

    private LinearLayout buttonRow(LinearLayout root) {
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(buttons, matchWrap());
        return buttons;
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

    private TextView logView() {
        TextView view = text("", 12, false);
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextColor(0xFF24292F);
        view.setBackgroundColor(0xFFF6F8FA);
        view.setPadding(dp(8), dp(8), dp(8), dp(8));
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

    private interface ShellConsumer {
        void accept(PortalShell shell);
    }
}
