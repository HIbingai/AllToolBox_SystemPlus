package com.atb.systemplus;

import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean-room rewrite focused on structure and control flow:
 * 1) Route by package/process in handleLoadPackage.
 * 2) Install hooks in explicit feature groups.
 * 3) Gate sensitive bypass behavior with trusted-caller checks.
 */
public final class SystemPlusEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "[SystemPlus][rewrite] ";
    private static final String PREF_PACKAGE = "com.atb.systemplus";
    private static final String PREF_FILE = "conf";

    private static final String PKG_ANDROID = "android";
    private static final String PKG_SYSTEM_UI = "com.android.systemui";
    private static final String PKG_LAUNCHER = "com.xtc.i3launcher";
    private static final String PKG_CAMERA = "com.xtc.camera.app";
    private static final String PKG_INSTALLER = "com.android.packageinstaller";

    private final XSharedPreferences prefs = new XSharedPreferences(PREF_PACKAGE, PREF_FILE);
    private final Set<String> installedProcessGuards = ConcurrentHashMap.newKeySet();
    private final Set<String> patchedIsDredClassNames = ConcurrentHashMap.newKeySet();

    private volatile boolean zygoteInitialized;

    @Override
    public synchronized void initZygote(StartupParam startupParam) {
        if (zygoteInitialized) {
            return;
        }
        reloadPrefs();
        installGlobalClipboardHooks();
        forceDebugBuildFlags();
        try {
            installSystemXsesHooks(ClassLoader.getSystemClassLoader());
            XposedBridge.log(TAG + "installSystemXsesHooks registered in zygote");
        } catch (Throwable t) {
            XposedBridge.log(TAG + "installSystemXsesHooks zygote failed: " + t.getClass().getSimpleName());
        }
        zygoteInitialized = true;

        String modulePath = startupParam != null ? startupParam.modulePath : "<null>";
        XposedBridge.log(TAG + "initZygote done, modulePath=" + modulePath);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null || lpparam.classLoader == null) {
            return;
        }

        reloadPrefs();
        hookModuleSelfCheck(lpparam);

        String processName = lpparam.processName != null ? lpparam.processName : "";
        String guard = lpparam.packageName + "#" + processName;
        if (!installedProcessGuards.add(guard)) {
            return;
        }

        if (PKG_ANDROID.equals(lpparam.packageName)) {
            XposedBridge.log(TAG + "install android hooks, process=" + processName);
            installAndroidProcessHooks(lpparam);
            installPermissionBypassHooks(lpparam);
        }

        if (PKG_SYSTEM_UI.equals(lpparam.packageName) && HookSettings.isEnabled(prefs, "SystemUI", false)) {
            installPermissionBypassHooks(lpparam);
        }

        if (PKG_LAUNCHER.equals(lpparam.packageName)
                && HookSettings.isEnabled(prefs, "PowerKey_LongClick_Recents", false)) {
            installLauncherHooks(lpparam);
        }

        if (PKG_CAMERA.equals(lpparam.packageName) && HookSettings.isEnabled(prefs, "VideoRecord", true)) {
            installCameraHooks(lpparam);
        }

        if (PKG_INSTALLER.equals(lpparam.packageName) && HookSettings.isEnabled(prefs, "Installer", true)) {
            installInstallerHooks(lpparam);
        }
    }

    private void hookModuleSelfCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!PREF_PACKAGE.equals(lpparam.packageName)) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(
                    "com.atb.systemplus.UnlockActivity",
                    lpparam.classLoader,
                    "isModuleEnabled",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(Boolean.TRUE);
                        }
                    }
            );
        } catch (Throwable ignored) {
            // Best-effort only.
        }
    }

    private void reloadPrefs() {
        try {
            prefs.reload();
        } catch (Throwable ignored) {
            // Keep default values on read failure.
        }
    }

    private void forceDebugBuildFlags() {
        try {
            XposedHelpers.setStaticBooleanField(Build.class, "isDebugSystem", true);
        } catch (Throwable ignored) {
            // Field may not exist on all ROMs.
        }
    }

    private void installAndroidProcessHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader cl = lpparam.classLoader;

        hookAllMethodsReturnBoolean(cl,
                "com.android.server.security.xtc.SystemSecurityService",
                "checkRoot",
                false);

        hookAllMethodsReturnDefault(cl,
                "com.android.server.security.xtc.SystemSecurityService",
                "doCheckUsbStatus");

        hookAllMethodsReturnDefault(cl,
                "com.android.server.security.xtc.SystemSecurityService",
                "processServerPush");

        boolean enableInstallerPipeline = HookSettings.isEnabled(prefs, "InstallerPipeline", false)
            || HookSettings.isEnabled(prefs, "InstallerBypass", false);
        if (enableInstallerPipeline) {
            installPackageInstallPipelineHooks(cl);
        } else {
            XposedBridge.log(TAG + "skip install pipeline hooks by default");
        }

        // Keep a narrow crash guard even when install pipeline bypass is disabled.
        // Some ROM hook chains throw intermittent NPE in installLocationPolicy.
        installPackageInstallStabilityGuards(cl);

        boolean disableVersionDowngradeCheck = HookSettings.isEnabled(
            prefs,
            "DisableVersionDowngradeCheck",
            false
        );
        if (disableVersionDowngradeCheck) {
            hookAllMethodsReturnBoolean(cl,
                "com.android.server.xtclog.XTCLogService$XTCUtilsService",
                "checkVersionDowngrade",
                false);
        } else {
            XposedBridge.log(TAG + "skip checkVersionDowngrade hook by default");
        }
        // Some OEM services like SystemXses may set an "isDRed" flag that causes
        // aggressive protections and service termination. Force it to false when
        // we can find the service class to avoid system self-kill paths.
        installSystemXsesHooks(cl);
    }

    private void installPermissionBypassHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        final boolean depEnabled = HookSettings.isEnabled(prefs, "DePermission", false)
                || HookSettings.isEnabled(prefs, "DePermissions", false);
        if (!depEnabled && !hasPrivilegeToken()) {
            return;
        }

        XC_MethodHook permissionBypassHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int uid = extractLikelyUid(param.args);
                String pkg = extractLikelyPackage(param.args);
                if (!isTrustedPermissionCaller(uid, pkg)) {
                    return;
                }

                Class<?> returnType = ((Method) param.method).getReturnType();
                if (Integer.TYPE.equals(returnType)) {
                    param.setResult(PackageManager.PERMISSION_GRANTED);
                    return;
                }
                if (Boolean.TYPE.equals(returnType)) {
                    param.setResult(Boolean.TRUE);
                    return;
                }
                if (Void.TYPE.equals(returnType)) {
                    param.setResult(null);
                }
            }
        };

        ClassLoader cl = lpparam.classLoader;
        hookAllMethodsIfExists(cl, "android.app.ContextImpl", "checkPermission", permissionBypassHook);
        hookAllMethodsIfExists(cl, "android.app.ContextImpl", "checkUriPermission", permissionBypassHook);
        hookAllMethodsIfExists(cl, "android.app.ContextImpl", "enforce", permissionBypassHook);
        hookAllMethodsIfExists(cl, "android.app.ContextImpl", "enforceCallingPermission", permissionBypassHook);
        hookAllMethodsIfExists(cl, "android.app.ContextImpl", "enforceCallingOrSelfPermission", permissionBypassHook);
        hookAllMethodsIfExists(cl, "android.content.ContentProvider", "checkPermissionAndAppOp", permissionBypassHook);
    }

    private void installLauncherHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader cl = lpparam.classLoader;

        hookAllMethodsReturnDefault(cl,
                "com.xtc.i3launcher.module.key.PowerKeyReceiver",
                "onReceive");

        hookAllMethodsReturnBoolean(cl,
                "com.xtc.initservice.auth.SystemKeyHelper",
                "isSystemSupport",
                true);

        hookAllMethodsReturnBoolean(cl,
                "com.xtc.initservice.auth.SystemKeyHelper",
                "isSystemHaveRsaKey",
                true);

        hookAllMethodsReturnDefault(cl,
                "com.xtc.initservice.auth.SystemKeyHelper",
                "setSystemKey");

        hookAllMethodsReturnDefault(cl,
                "com.xtc.initservice.auth.SystemKeyHelper",
                "decryptHttpToken");

        hookAllMethodsReturnDefault(cl,
                "com.xtc.initservice.auth.SystemKeyHelper",
                "getHttpTokenParam");
    }

    private void installCameraHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        final int durationSeconds = HookSettings.cameraDurationSeconds(prefs);
        ClassLoader cl = lpparam.classLoader;

        hookAllMethodsIfExists(cl,
                "com.xtc.camera.app.module.video.config.VideoRecordConfigManager",
                "getVideoTimeDuration",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(durationSeconds);
                    }
                });

        hookAllMethodsIfExists(cl,
                "com.xtc.camera.app.module.video.config.VideoRecordConfigManager",
                "getDefaultDuration",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(durationSeconds);
                    }
                });
    }

    private void installInstallerHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader cl = lpparam.classLoader;

        hookAllMethodsReturnBoolean(cl,
                "com.android.packageinstaller.DeviceUtils",
                "isWear",
                false);
    }

    private void installPackageInstallStabilityGuards(ClassLoader cl) {
        hookAllMethodsIfExists(cl,
            "com.android.server.pm.PackageManagerService$InstallParams",
            "installLocationPolicy",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!param.hasThrowable()) {
                        return;
                    }
                    Throwable throwable = param.getThrowable();
                    if (!(throwable instanceof NullPointerException)) {
                        return;
                    }

                    Class<?> returnType = ((Method) param.method).getReturnType();
                    if (Integer.TYPE.equals(returnType) || Integer.class.equals(returnType)) {
                        // 1 maps to internal install recommendation on legacy ROM branches.
                        param.setThrowable(null);
                        param.setResult(1);
                        XposedBridge.log(TAG + "guarded installLocationPolicy NPE with fallback result=1");
                    }
                }
            });

        hookAllMethodsIfExists(cl,
            "com.android.server.pm.PackageManagerService$InstallParams",
            "handleStartCopy",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!param.hasThrowable()) {
                        return;
                    }
                    Throwable throwable = param.getThrowable();
                    if (!(throwable instanceof NullPointerException)) {
                        return;
                    }

                    param.setThrowable(null);
                    param.setResult(null);
                    XposedBridge.log(TAG + "guarded handleStartCopy NPE by swallowing throwable");
                }
            });
    }

        private void installPackageInstallPipelineHooks(ClassLoader cl) {
        hookAllMethodsOverrideBooleanAfter(cl,
            "com.android.server.pm.PackageInstallerSession",
            "verifyOnXTCServer",
            true);

        hookAllMethodsOverrideBooleanAfter(cl,
            "com.android.server.pm.PackageInstallerSession",
            "verifyOnLocal",
            true);

        // ROM variants may rename these checks while keeping the same prefix.
        hookMethodsByPrefixReturnBoolean(cl,
            "com.android.server.pm.PackageInstallerSession",
            "verifyOnXTC",
            true);
        hookMethodsByPrefixReturnBoolean(cl,
            "com.android.server.pm.PackageInstallerSession",
            "verifyOnXtc",
            true);
        hookMethodsByPrefixReturnBoolean(cl,
            "com.android.server.pm.PackageInstallerSession",
            "verifyOnLocal",
            true);

        hookAllMethodsIfExists(cl,
            "com.android.server.pm.PackageManagerService",
            "handlePackagePostInstall",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Method method = (Method) param.method;
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    Object[] args = param.args;
                    if (args == null || parameterTypes == null) {
                        return;
                    }
                    int patchedCount = 0;
                    int max = Math.min(args.length, parameterTypes.length);
                    for (int i = 0; i < max; i++) {
                        if (args[i] != null) {
                            continue;
                        }
                        Class<?> parameterType = parameterTypes[i];
                        if (!parameterType.isArray()) {
                            continue;
                        }
                        Class<?> componentType = parameterType.getComponentType();
                        args[i] = java.lang.reflect.Array.newInstance(componentType, 0);
                        patchedCount++;
                        XposedBridge.log(TAG + "patched null array arg for handlePackagePostInstall index=" + i);
                    }

                    // ROM hook chains may pass PackageInstalledInfo with null array fields.
                    if (max > 0 && args[0] != null) {
                        patchedCount += patchNullArraysOnObject(args[0], "arg0", 0);
                    }
                    if (patchedCount > 0) {
                        XposedBridge.log(TAG + "handlePackagePostInstall patched total arrays=" + patchedCount);
                    }
                }
            });
    }


    private int patchNullArraysOnObject(Object target, String path, int depth) {
        if (target == null || depth > 2) {
            return 0;
        }
        int patched = 0;
        Class<?> cls = target.getClass();
        while (cls != null && !Object.class.equals(cls)) {
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    Object value = field.get(target);
                    if (fieldType.isArray()) {
                        if (value == null) {
                            Object emptyArray = java.lang.reflect.Array.newInstance(fieldType.getComponentType(), 0);
                            field.set(target, emptyArray);
                            patched++;
                            XposedBridge.log(TAG + "patched null array field " + path + "." + field.getName());
                        }
                        continue;
                    }
                    if (value != null && fieldType.getName().startsWith("com.android.server.pm.")) {
                        patched += patchNullArraysOnObject(value, path + "." + field.getName(), depth + 1);
                    }
                } catch (Throwable ignored) {
                    // Best-effort sanitization only.
                }
            }
            cls = cls.getSuperclass();
        }
        return patched;
    }

    private void installGlobalClipboardHooks() {
        if (!HookSettings.isEnabled(prefs, "ClipBoard", true)) {
            return;
        }

        hookAllMethods(TextView.class, "canPaste", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(Boolean.TRUE);
            }
        });

        if (HookSettings.isEnabled(prefs, "SelectText", true)) {
            hookAllMethods(TextView.class, "textCanBeSelected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(Boolean.TRUE);
                }
            });
        }

        if (HookSettings.isEnabled(prefs, "NoShowInput_InSelect", false)) {
            hookAllMethods(InputMethodManager.class, "showSoftInput", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.setResult(Boolean.FALSE);
                }
            });
        }

        if (HookSettings.isEnabled(prefs, "NoShowInput_OnLongCLick", false)) {
            hookAllMethodsIfExists(ClassLoader.getSystemClassLoader(),
                    "android.widget.Editor$SelectionModifierCursorController",
                    "onTouchEvent",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args != null && param.args.length > 0 && param.args[0] instanceof MotionEvent) {
                                param.setResult(Boolean.FALSE);
                            }
                        }
                    });
        }

        hookAllMethods(TelephonyManager.class, "getPhoneCompileStatus", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(defaultValueFor(((Method) param.method).getReturnType()));
            }
        });

        String serviceFieldName = (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) ? "sService" : "mService";
        XposedBridge.log(TAG + "clipboard backend field=" + serviceFieldName);

        hookAllMethods(ClipboardManager.class, "reportPrimaryClipChanged", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
            }
        });
    }

    private boolean isTrustedPermissionCaller(int uid, String packageName) {
        if (uid == 0 || uid == 1000 || uid == 2000) {
            return true;
        }
        if (packageName != null) {
            if (packageName.startsWith(PREF_PACKAGE)) {
                return true;
            }
            if (packageName.startsWith(PKG_LAUNCHER)) {
                return true;
            }
        }
        return hasPrivilegeToken();
    }

    private boolean hasPrivilegeToken() {
        String token = prefs.getString("key", "");
        return HookSettings.hasPrivilegeToken(token);
    }

    private int extractLikelyUid(Object[] args) {
        if (args == null) {
            return -1;
        }
        for (int i = args.length - 1; i >= 0; i--) {
            Object arg = args[i];
            if (arg instanceof Integer) {
                int value = (Integer) arg;
                if (value >= 0) {
                    return value;
                }
            }
        }
        return -1;
    }

    private String extractLikelyPackage(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof String) {
                    String value = (String) arg;
                    if (isLikelyPackageName(value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private boolean isLikelyPackageName(String value) {
        return value != null && value.indexOf('.') > 0 && !value.contains(" ");
    }

    private void hookAllMethodsReturnBoolean(ClassLoader cl, String className, String methodName, final boolean result) {
        hookAllMethodsIfExists(cl, className, methodName, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(result);
            }
        });
    }

    private void hookAllMethodsOverrideBooleanAfter(
            ClassLoader cl,
            String className,
            String methodName,
            final boolean result
    ) {
        hookAllMethodsIfExists(cl, className, methodName, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Class<?> returnType = ((Method) param.method).getReturnType();
                if (Boolean.TYPE.equals(returnType) || Boolean.class.equals(returnType)) {
                    param.setResult(result);
                }
            }
        });
    }

    private void hookMethodsByPrefixReturnBoolean(
            ClassLoader cl,
            String className,
            String methodPrefix,
            final boolean result
    ) {
        try {
            Class<?> target = XposedHelpers.findClass(className, cl);
            int hookedCount = 0;
            for (Method method : target.getDeclaredMethods()) {
                if (!method.getName().startsWith(methodPrefix)) {
                    continue;
                }
                Class<?> returnType = method.getReturnType();
                if (!Boolean.TYPE.equals(returnType) && !Boolean.class.equals(returnType)) {
                    continue;
                }
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(result);
                    }
                });
                hookedCount++;
            }
            if (hookedCount > 0) {
                XposedBridge.log(TAG + "hooked " + className + "#" + methodPrefix + "* count=" + hookedCount);
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + "skip " + className + "#" + methodPrefix + "* : " + t.getClass().getSimpleName());
        }
    }

    private void hookAllMethodsReturnDefault(ClassLoader cl, String className, String methodName) {
        hookAllMethodsIfExists(cl, className, methodName, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Class<?> returnType = ((Method) param.method).getReturnType();
                param.setResult(defaultValueFor(returnType));
            }
        });
    }

    private void hookAllMethodsIfExists(ClassLoader cl, String className, String methodName, XC_MethodHook hook) {
        try {
            Class<?> target = XposedHelpers.findClass(className, cl);
            XposedBridge.hookAllMethods(target, methodName, hook);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "skip " + className + "#" + methodName + " : " + t.getClass().getSimpleName());
        }
    }

    private void hookAllMethods(Class<?> target, String methodName, XC_MethodHook hook) {
        try {
            XposedBridge.hookAllMethods(target, methodName, hook);
        } catch (Throwable t) {
            XposedBridge.log(TAG + "skip " + target.getName() + "#" + methodName + " : " + t.getClass().getSimpleName());
        }
    }

    private Object defaultValueFor(Class<?> returnType) {
        if (returnType == null || Void.TYPE.equals(returnType)) {
            return null;
        }
        if (Boolean.TYPE.equals(returnType)) {
            return Boolean.FALSE;
        }
        if (Byte.TYPE.equals(returnType)) {
            return (byte) 0;
        }
        if (Short.TYPE.equals(returnType)) {
            return (short) 0;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        if (Long.TYPE.equals(returnType)) {
            return 0L;
        }
        if (Float.TYPE.equals(returnType)) {
            return 0f;
        }
        if (Double.TYPE.equals(returnType)) {
            return 0d;
        }
        if (Character.TYPE.equals(returnType)) {
            return '\0';
        }
        return null;
    }

    // Try several likely SystemXses class names and force any 'isDRed' fields to false.
    private void installSystemXsesHooks(ClassLoader cl) {
        final String[] candidates = new String[]{
                "com.android.server.xgseserver.xss.xsesService",
                "com.android.server.xgseserver.xss.XsesService",
                "com.android.server.xgseserver.xss.SystemXses",
                "com.android.server.security.xtc.SystemXses",
                "com.xtc.xses.SystemXses",
                "com.xtc.xses.xsesService"
        };

        for (final String cname : candidates) {
            try {
                final Class<?> target = XposedHelpers.findClass(cname, cl);
                XposedBridge.log(TAG + "found candidate SystemXses class=" + cname);

                // Explicitly override known aggressive checks/methods seen in OEM code.
                try {
                    hookAllMethodsReturnDefault(cl, cname, "onBootPhase");
                    // isDRed may be a method; force it to return false/default.
                    try {
                        XposedBridge.hookAllMethods(target, "isDRed", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                try {
                                    Class<?> rt = ((Method) param.method).getReturnType();
                                    param.setResult(defaultValueFor(rt));
                                    XposedBridge.log(TAG + "overrode isDRed on " + cname);
                                } catch (Throwable ignored) {
                                }
                            }
                        });
                    } catch (Throwable ignored) {
                    }

                    hookAllMethodsReturnDefault(cl, cname, "checkSystemStatus");
                    hookAllMethodsReturnDefault(cl, cname, "checkPFStatus");
                    hookAllMethodsReturnDefault(cl, cname, "checkRoot");
                    hookAllMethodsReturnDefault(cl, cname, "processServerPush");
                    hookAllMethodsReturnDefault(cl, cname, "realRequest");
                    hookAllMethodsReturnDefault(cl, cname, "doCheckUsbStatus");
                    hookAllMethodsReturnDefault(cl, cname, "checkUsbStatus");
                    hookAllMethodsReturnDefault(cl, cname, "saveRootInfo");
                    hookAllMethodsReturnDefault(cl, cname, "saveUsbInfo");
                    hookAllMethodsReturnDefault(cl, cname, "keepPushStatus");
                    hookAllMethodsReturnDefault(cl, cname, "checkBVC");
                } catch (Throwable ignored) {
                }

                XposedBridge.hookAllConstructors(target, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            forceClearIsDRedOn(target, param.thisObject);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + "forceClearIsDRedOn failed: " + t.getClass().getSimpleName());
                        }
                    }
                });

                // If there is an explicit xsesCR method (seen in logs), ensure we clear
                // the flag at start of that routine as well.
                try {
                    XposedBridge.hookAllMethods(target, "xsesCR", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                forceClearIsDRedOn(target, param.thisObject);
                                // Short-circuit xsesCR to avoid internal sets that enable protections.
                                Class<?> returnType = ((Method) param.method).getReturnType();
                                param.setResult(defaultValueFor(returnType));
                                XposedBridge.log(TAG + "short-circuited xsesCR on " + cname + " return=" + returnType.getName());
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + "xsesCR hook failed: " + t.getClass().getSimpleName());
                            }
                        }
                    });
                } catch (Throwable ignored) {
                    // method not present on this ROM variant
                }
                // Hook all declared methods on the target so we aggressively clear any
                // isDRed fields before/after method execution (best-effort to avoid
                // the flag becoming true during runtime-critical paths).
                try {
                    Method[] declared = target.getDeclaredMethods();
                    for (final Method m : declared) {
                        try {
                            final String mname = m.getName();
                            final String mn = mname == null ? "" : mname.toLowerCase();
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) {
                                    try {
                                        forceClearIsDRedOn(target, param.thisObject);
                                        forceClearIsDRedOn(target, null);
                                        // Aggressively short-circuit suspicious methods that may flip the flag.
                                        if (mn.contains("xsescr") || mn.contains("postfs") || (mn.contains("post") && mn.contains("fs")) || mn.contains("xses")) {
                                            Class<?> rt = ((Method) param.method).getReturnType();
                                            param.setResult(defaultValueFor(rt));
                                            XposedBridge.log(TAG + "short-circuited method " + mname + " on " + cname);
                                        }
                                    } catch (Throwable ignored) {
                                    }
                                }

                                @Override
                                protected void afterHookedMethod(MethodHookParam param) {
                                    try {
                                        forceClearIsDRedOn(target, param.thisObject);
                                        forceClearIsDRedOn(target, null);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            });
                        } catch (Throwable ignored) {
                        }
                    }
                    XposedBridge.log(TAG + "hooked all declared methods on " + cname + " to clear isDRed");
                } catch (Throwable ignored) {
                }
            } catch (Throwable t) {
                // class not found; try next candidate
            }
        }
        // Global safeguard: hook ClassLoader.loadClass and inspect newly-loaded classes.
        try {
            hookAllMethods(ClassLoader.class, "loadClass", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object res = param.getResult();
                        if (!(res instanceof Class)) {
                            return;
                        }
                        final Class<?> cls = (Class<?>) res;
                        if (cls == null) return;
                        final String cname = cls.getName();
                        if (!patchedIsDredClassNames.add(cname)) {
                            return; // already processed
                        }

                        Field[] fields = cls.getDeclaredFields();
                        for (final Field f : fields) {
                            try {
                                String fname = f.getName();
                                if (fname == null) continue;
                                String ln = fname.toLowerCase();
                                if (!ln.contains("isdred")) continue;

                                f.setAccessible(true);
                                if (Modifier.isStatic(f.getModifiers())) {
                                    if (f.getType().equals(Boolean.TYPE) || f.getType().equals(Boolean.class)) {
                                        try {
                                            f.setBoolean(null, false);
                                            XposedBridge.log(TAG + "set static field " + fname + "=false on " + cname);
                                        } catch (Throwable ignored) {
                                        }
                                    }
                                } else {
                                    // Hook constructors to clear instance fields on creation
                                    try {
                                        final Field fieldRef = f;
                                        XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
                                            @Override
                                            protected void afterHookedMethod(MethodHookParam param2) {
                                                try {
                                                    Object inst = param2.thisObject;
                                                    if (inst == null) return;
                                                    fieldRef.setAccessible(true);
                                                    if (fieldRef.getType().equals(Boolean.TYPE)) {
                                                        fieldRef.setBoolean(inst, false);
                                                    } else if (fieldRef.getType().equals(Boolean.class)) {
                                                        fieldRef.set(inst, Boolean.FALSE);
                                                    }
                                                } catch (Throwable ignored) {
                                                }

                                                // Hook android.util.Log to catch any runtime printed "isDRed" messages
                                                // and aggressively clear the flag on classes found in the call stack.
                                                try {
                                                    final Class<?> logCls = XposedHelpers.findClass("android.util.Log", ClassLoader.getSystemClassLoader());
                                                    XC_MethodHook logHook = new XC_MethodHook() {
                                                        @Override
                                                        protected void beforeHookedMethod(MethodHookParam param) {
                                                            try {
                                                                if (param.args == null || param.args.length < 2) return;
                                                                Object tagObj = param.args[0];
                                                                Object msgObj = param.args[1];
                                                                String tag = tagObj == null ? "" : tagObj.toString();
                                                                String msg = msgObj == null ? "" : msgObj.toString();
                                                                String low = msg.toLowerCase();
                                                                if (!(low.contains("isdred") || low.contains("xsescr") || (tag != null && tag.contains("SystemXses")))) {
                                                                    return;
                                                                }
                                                                XposedBridge.log(TAG + "caught log tag=" + tag + " msg=" + msg);
                                                                StackTraceElement[] st = new Throwable().getStackTrace();
                                                                for (StackTraceElement e : st) {
                                                                    try {
                                                                        String clsName = e.getClassName();
                                                                        if (clsName == null) continue;
                                                                        String lc = clsName.toLowerCase();
                                                                        if (!(lc.contains("xgses") || lc.contains("xses") || lc.contains("systemxses"))) continue;
                                                                        try {
                                                                            Class<?> cls = Class.forName(clsName, false, ClassLoader.getSystemClassLoader());
                                                                            forceClearIsDRedOn(cls, null);
                                                                            XposedBridge.log(TAG + "cleared isDRed on class from stack=" + clsName);
                                                                        } catch (Throwable t) {
                                                                            // best-effort: class may be loaded by different classloader
                                                                        }
                                                                    } catch (Throwable ignored) {
                                                                    }
                                                                }
                                                            } catch (Throwable ignored) {
                                                            }
                                                        }
                                                    };
                                                    XposedBridge.hookAllMethods(logCls, "d", logHook);
                                                    XposedBridge.hookAllMethods(logCls, "i", logHook);
                                                    XposedBridge.hookAllMethods(logCls, "w", logHook);
                                                    XposedBridge.hookAllMethods(logCls, "e", logHook);
                                                    XposedBridge.log(TAG + "installed Log hooks to detect isDRed messages");
                                                } catch (Throwable ignored) {
                                                }
                                            }
                                        });
                                        XposedBridge.log(TAG + "hooked constructors to clear " + fname + " on " + cname);
                                    } catch (Throwable ignored) {
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private void forceClearIsDRedOn(Class<?> cls, Object instance) {
        if (cls == null) return;
        int patched = 0;
        Class<?> cur = cls;
        while (cur != null && !Object.class.equals(cur)) {
            Field[] fields = cur.getDeclaredFields();
            for (Field f : fields) {
                try {
                    String name = f.getName();
                    if (name == null) continue;
                    String ln = name.toLowerCase();
                    if (!ln.equals("isdred") && !ln.contains("isdred") && !ln.contains("isd_red")) {
                        continue;
                    }
                    f.setAccessible(true);
                    if (instance == null) {
                        if (f.getType().equals(Boolean.TYPE) || f.getType().equals(Boolean.class)) {
                            f.setBoolean(null, false);
                            patched++;
                            XposedBridge.log(TAG + "set static field " + name + "=false on " + cur.getName());
                        }
                    } else {
                        if (f.getType().equals(Boolean.TYPE) || f.getType().equals(Boolean.class)) {
                            f.setBoolean(instance, false);
                            patched++;
                            XposedBridge.log(TAG + "set instance field " + name + "=false on " + cur.getName());
                        }
                    }
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
            cur = cur.getSuperclass();
        }
        if (patched > 0) {
            XposedBridge.log(TAG + "forceClearIsDRedOn patched fields=" + patched + " for " + cls.getName());
        }
    }
}

