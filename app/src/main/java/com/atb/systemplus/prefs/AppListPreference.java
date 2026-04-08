package com.atb.systemplus.prefs;

import android.content.Context;
import android.util.AttributeSet;
import androidx.preference.Preference;

public class AppListPreference extends Preference {

    public AppListPreference(Context context) {
        super(context);
    }

    public AppListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AppListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
