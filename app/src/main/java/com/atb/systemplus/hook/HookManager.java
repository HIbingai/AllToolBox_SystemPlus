package com.atb.systemplus.hook;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class HookManager {

    public static final String SETTINGS_PREF_NAME = "hook_loader_settings";
    public static final String KEY_AUTO_ENABLE_IMPORTED_HOOK = "auto_enable_imported_hook";
    public static final String KEY_OVERWRITE_WHEN_NAME_EXISTS = "overwrite_when_name_exists";

    private static volatile HookManager instance;

    private final HookDao hookDao;
    private final ContentResolver contentResolver;
    private final SharedPreferences settingsPreferences;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onSuccess(T value);

        void onError(String message, Throwable throwable);
    }

    private HookManager(Context context) {
        Context appContext = context.getApplicationContext();
        HookDatabase database = HookDatabase.getInstance(appContext);
        hookDao = database.hookDao();
        contentResolver = appContext.getContentResolver();
        settingsPreferences = appContext.getSharedPreferences(SETTINGS_PREF_NAME, Context.MODE_PRIVATE);
        ioExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static HookManager getInstance(Context context) {
        if (instance == null) {
            synchronized (HookManager.class) {
                if (instance == null) {
                    instance = new HookManager(context);
                }
            }
        }
        return instance;
    }

    public void listHooks(@NonNull Callback<List<HookObject>> callback) {
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<HookObject> hooks = hookDao.getAll();
                    postSuccess(callback, hooks);
                } catch (Throwable throwable) {
                    postError(callback, "加载 Hook 列表失败", throwable);
                }
            }
        });
    }

    public void importHook(
            @NonNull Uri uiUri,
            @NonNull Uri hookUri,
            @NonNull String uiXmlContent,
            @NonNull String hookXmlContent,
            @NonNull String suggestedName,
            @NonNull Callback<HookObject> callback
    ) {
        if (TextUtils.isEmpty(uiXmlContent) || TextUtils.isEmpty(hookXmlContent)) {
            postError(callback, "导入失败：XML 内容不能为空", new IllegalArgumentException("empty xml"));
            return;
        }

        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean overwrite = settingsPreferences.getBoolean(KEY_OVERWRITE_WHEN_NAME_EXISTS, false);
                    boolean autoEnable = settingsPreferences.getBoolean(KEY_AUTO_ENABLE_IMPORTED_HOOK, true);

                    String baseName = sanitizeDisplayName(suggestedName);
                    if (TextUtils.isEmpty(baseName)) {
                        baseName = "Custom Hook";
                    }

                    HookObject target = null;
                    String finalName = baseName;
                    if (overwrite) {
                        target = hookDao.findByDisplayName(baseName);
                    } else {
                        finalName = buildUniqueName(baseName);
                    }

                    if (target == null) {
                        target = new HookObject();
                        target.id = UUID.randomUUID().toString();
                        target.createdAt = System.currentTimeMillis();
                    }

                    target.displayName = finalName;
                    target.uiXmlContent = uiXmlContent;
                    target.hookXmlContent = hookXmlContent;
                    target.enabled = autoEnable;
                    target.uiSourceUri = uiUri.toString();
                    target.hookSourceUri = hookUri.toString();

                    hookDao.upsert(target);
                    postSuccess(callback, target);
                } catch (Throwable throwable) {
                    postError(callback, "导入 Hook 失败", throwable);
                }
            }
        });
    }

    public void setEnabled(@NonNull String hookId, boolean enabled, @NonNull Callback<HookObject> callback) {
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HookObject hookObject = requireHook(hookId);
                    hookObject.enabled = enabled;
                    hookDao.upsert(hookObject);
                    postSuccess(callback, hookObject);
                } catch (Throwable throwable) {
                    postError(callback, "更新 Hook 启用状态失败", throwable);
                }
            }
        });
    }

    public void deleteHook(@NonNull String hookId, @NonNull Callback<String> callback) {
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HookObject hookObject = requireHook(hookId);
                    hookDao.delete(hookObject);
                    postSuccess(callback, hookId);
                } catch (Throwable throwable) {
                    postError(callback, "删除 Hook 失败", throwable);
                }
            }
        });
    }

    public void exportHookAsZip(@NonNull String hookId, @NonNull Uri destinationUri, @NonNull Callback<String> callback) {
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HookObject hookObject = requireHook(hookId);

                    OutputStream outputStream = contentResolver.openOutputStream(destinationUri);
                    if (outputStream == null) {
                        throw new IOException("cannot open output stream");
                    }

                    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
                        writeZipEntry(zipOutputStream, "ui.xml", hookObject.uiXmlContent);
                        writeZipEntry(zipOutputStream, "hook.xml", hookObject.hookXmlContent);
                    }

                    postSuccess(callback, "zip");
                } catch (Throwable throwable) {
                    postError(callback, "导出 ZIP 失败", throwable);
                }
            }
        });
    }

    public void exportHookAsXml(
            @NonNull String hookId,
            @NonNull Uri destinationUri,
            boolean exportUiXml,
            @NonNull Callback<String> callback
    ) {
        ioExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    HookObject hookObject = requireHook(hookId);
                    String content = exportUiXml ? hookObject.uiXmlContent : hookObject.hookXmlContent;
                    writeStringToUri(destinationUri, content);
                    postSuccess(callback, exportUiXml ? "ui" : "hook");
                } catch (Throwable throwable) {
                    postError(callback, "导出 XML 失败", throwable);
                }
            }
        });
    }

    private HookObject requireHook(String hookId) {
        HookObject hookObject = hookDao.findById(hookId);
        if (hookObject == null) {
            throw new IllegalStateException("hook not found: " + hookId);
        }
        return hookObject;
    }

    private String buildUniqueName(String baseName) {
        String candidate = baseName;
        int index = 2;
        while (hookDao.findByDisplayName(candidate) != null) {
            candidate = baseName + " (" + index + ")";
            index++;
        }
        return candidate;
    }

    private String sanitizeDisplayName(String source) {
        String value = source == null ? "" : source.trim();
        value = value.replaceAll("\\s+", " ");
        if (value.endsWith(".xml") || value.endsWith(".XML")) {
            value = value.substring(0, value.length() - 4);
        }
        return value;
    }

    private void writeStringToUri(Uri destinationUri, String content) throws IOException {
        OutputStream outputStream = contentResolver.openOutputStream(destinationUri);
        if (outputStream == null) {
            throw new IOException("cannot open output stream");
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(content == null ? "" : content);
            writer.flush();
        }
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        byte[] data = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        zipOutputStream.write(data);
        zipOutputStream.closeEntry();
    }

    private <T> void postSuccess(@NonNull Callback<T> callback, T value) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(value);
            }
        });
    }

    private void postError(@NonNull Callback<?> callback, @NonNull String message, @NonNull Throwable throwable) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(message, throwable);
            }
        });
    }
}
