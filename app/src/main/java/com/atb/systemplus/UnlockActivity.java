package com.atb.systemplus;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;

public final class UnlockActivity extends Activity {

    private TextView tvFrameworkStatus;
    private TextView tvFrameworkDetail;
    private TextView tvModuleStatus;
    private TextView tvModuleDetail;
    private TextView tvRuntimeHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_unlock);
        bindViews();
        updateStatusViews();

        Button btnRefresh = findViewById(R.id.btn_refresh_status);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatusViews();
            }
        });

        Button btnCopyDiagnostics = findViewById(R.id.btn_copy_diagnostics);
        btnCopyDiagnostics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyDiagnostics();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusViews();
    }

    // Hook target: SystemPlusEntry will set this to true when module is active.
    public boolean isModuleEnabled() {
        return false;
    }

    private void bindViews() {
        tvFrameworkStatus = findViewById(R.id.tv_framework_status);
        tvFrameworkDetail = findViewById(R.id.tv_framework_detail);
        tvModuleStatus = findViewById(R.id.tv_module_status);
        tvModuleDetail = findViewById(R.id.tv_module_detail);
        tvRuntimeHint = findViewById(R.id.tv_runtime_hint);
    }

    private void copyDiagnostics() {
        String diagnostics = buildDiagnostics();
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("systemplus-diagnostics", diagnostics));
            Toast.makeText(this, getString(R.string.diagnostics_copied), Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDiagnostics() {
        FrameworkInfo frameworkInfo = detectFrameworkStatus();
        boolean moduleEnabled = isModuleEnabled();

        return "framework=" + frameworkInfo.statusText
                + "\nframeworkDetail=" + frameworkInfo.detail
                + "\nmodule=" + (moduleEnabled ? "enabled" : "disabled")
                + "\nmoduleDetail=" + (moduleEnabled
                ? getString(R.string.status_module_detail_enabled)
                : getString(R.string.status_module_detail_disabled));
    }

    private void updateStatusViews() {
        FrameworkInfo frameworkInfo = detectFrameworkStatus();
        setStatusText(tvFrameworkStatus, frameworkInfo.statusText, frameworkInfo.enabled);
        tvFrameworkDetail.setText(frameworkInfo.detail);

        boolean moduleEnabled = isModuleEnabled();
        setStatusText(
                tvModuleStatus,
                moduleEnabled ? getString(R.string.status_enabled) : getString(R.string.status_disabled),
                moduleEnabled
        );
        tvModuleDetail.setText(
                moduleEnabled
                        ? getString(R.string.status_module_detail_enabled)
                        : getString(R.string.status_module_detail_disabled)
        );

        if (!frameworkInfo.enabled) {
            tvRuntimeHint.setText(getString(R.string.runtime_hint_framework_off));
            tvRuntimeHint.setTextColor(Color.parseColor("#B71C1C"));
            return;
        }

        if (!moduleEnabled) {
            tvRuntimeHint.setText(getString(R.string.runtime_hint_module_off));
            tvRuntimeHint.setTextColor(Color.parseColor("#E65100"));
            return;
        }

        tvRuntimeHint.setText(getString(R.string.runtime_hint_all_ok));
        tvRuntimeHint.setTextColor(Color.parseColor("#1B5E20"));
    }

    private FrameworkInfo detectFrameworkStatus() {
        try {
            Class<?> bridgeClass = Class.forName("de.robv.android.xposed.XposedBridge");
            try {
                Method method = bridgeClass.getDeclaredMethod("getXposedVersion");
                Object result = method.invoke(null);
                if (result instanceof Integer) {
                    int apiVersion = ((Integer) result).intValue();
                    return new FrameworkInfo(
                            true,
                            getString(R.string.status_enabled_api, Integer.valueOf(apiVersion)),
                            getString(R.string.status_framework_detail_enabled_api, Integer.valueOf(apiVersion))
                    );
                }
            } catch (Throwable ignored) {
                // Ignore reflection failures and report generic enabled status.
            }
            return new FrameworkInfo(
                    true,
                    getString(R.string.status_enabled),
                    getString(R.string.status_framework_detail_enabled_generic)
            );
        } catch (Throwable ignored) {
            return new FrameworkInfo(
                    false,
                    getString(R.string.status_disabled),
                    getString(R.string.status_framework_detail_disabled)
            );
        }
    }

    private void setStatusText(TextView target, String text, boolean enabled) {
        target.setText(text);
        target.setTextColor(enabled ? Color.parseColor("#1B5E20") : Color.parseColor("#B71C1C"));
    }

    private static final class FrameworkInfo {
        final boolean enabled;
        final String statusText;
        final String detail;

        FrameworkInfo(boolean enabled, String statusText, String detail) {
            this.enabled = enabled;
            this.statusText = statusText;
            this.detail = detail;
        }
    }
}
