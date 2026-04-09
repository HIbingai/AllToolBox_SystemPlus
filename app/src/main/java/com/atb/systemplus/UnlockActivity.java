package com.atb.systemplus;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.lang.reflect.Method;

public final class UnlockActivity extends Activity {

    private TextView tvFrameworkStatus;
    private TextView tvFrameworkDetail;
    private TextView tvModuleStatus;
    private TextView tvModuleDetail;
    private TextView tvDeviceCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_unlock);
        bindViews();
        updateStatusViews();
        updateDeviceCode();

        Button btnRefresh = findViewById(R.id.btn_refresh_status);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateStatusViews();
                updateDeviceCode();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusViews();
        updateDeviceCode();
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
        tvDeviceCode = findViewById(R.id.tv_device_code);
    }

    private void updateDeviceCode() {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId == null || androidId.isEmpty()) {
            androidId = "unknown";
        }
        if (androidId.length() > 8) {
            androidId = androidId.substring(0, 8);
        }
        tvDeviceCode.setText(getString(R.string.device_code, androidId));
    }

    private void updateStatusViews() {
        FrameworkInfo frameworkInfo = detectFrameworkStatus();
        String frameworkState = frameworkInfo.enabled
                ? getString(R.string.status_framework_enabled)
                : getString(R.string.status_framework_disabled);
        setStatusText(tvFrameworkStatus, getString(R.string.status_prefix, frameworkState), frameworkInfo.enabled);
        tvFrameworkDetail.setText(frameworkInfo.detail);

        boolean moduleEnabled = isModuleEnabled();
        String moduleState = moduleEnabled
                ? getString(R.string.status_module_enabled)
                : getString(R.string.status_module_disabled);
        setStatusText(tvModuleStatus, getString(R.string.status_prefix, moduleState), moduleEnabled);
        tvModuleDetail.setText(
                moduleEnabled
                        ? getString(R.string.status_module_detail_enabled)
                        : getString(R.string.status_module_detail_disabled)
        );
    }

    private FrameworkInfo detectFrameworkStatus() {
        try {
            Class<?> bridgeClass = Class.forName("de.robv.android.xposed.XposedBridge");
            try {
                Method method = bridgeClass.getDeclaredMethod("getXposedVersion");
                Object result = method.invoke(null);
                if (result instanceof Integer) {
                    return new FrameworkInfo(true, getString(R.string.status_framework_detail_enabled_api));
                }
            } catch (Throwable ignored) {
                // Ignore reflection failures and report generic enabled status.
            }
            return new FrameworkInfo(true, getString(R.string.status_framework_detail_enabled_generic));
        } catch (Throwable ignored) {
            return new FrameworkInfo(false, getString(R.string.status_framework_detail_disabled));
        }
    }

    private void setStatusText(TextView target, String text, boolean enabled) {
        target.setText(text);
        target.setTextColor(enabled ? Color.parseColor("#198D7D") : Color.parseColor("#A64545"));
    }

    private static final class FrameworkInfo {
        final boolean enabled;
        final String detail;

        FrameworkInfo(boolean enabled, String detail) {
            this.enabled = enabled;
            this.detail = detail;
        }
    }
}