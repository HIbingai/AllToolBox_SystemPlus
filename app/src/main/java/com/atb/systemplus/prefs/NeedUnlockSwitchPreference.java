package com.atb.systemplus.prefs;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.widget.Toast;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;

public class NeedUnlockSwitchPreference extends SwitchPreferenceCompat {

    private static final String UNLOCK_HELPER_PACKAGE = "com.zcg.xtcpatch";

    public NeedUnlockSwitchPreference(Context context) {
        super(context);
    }

    public NeedUnlockSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NeedUnlockSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public NeedUnlockSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onClick() {
        if (isUnlocked()) {
            super.onClick();
            return;
        }
        Toast.makeText(getContext(), "需先完成解锁环境后再启用此项", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setAlpha(isUnlocked() ? 1.0f : 0.88f);
    }

    private boolean isUnlocked() {
        try {
            getContext().getPackageManager().getPackageInfo(UNLOCK_HELPER_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
