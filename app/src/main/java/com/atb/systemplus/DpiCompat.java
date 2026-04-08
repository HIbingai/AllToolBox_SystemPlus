package com.atb.systemplus;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public final class DpiCompat {

    public static final int TARGET_DPI = 200;

    private DpiCompat() {
    }

    public static void apply(Activity activity) {
        if (activity == null) {
            return;
        }

        Resources resources = activity.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        if (displayMetrics.densityDpi == TARGET_DPI) {
            return;
        }

        Configuration configuration = new Configuration(resources.getConfiguration());
        configuration.densityDpi = TARGET_DPI;
        resources.updateConfiguration(configuration, displayMetrics);

        DisplayMetrics updated = resources.getDisplayMetrics();
        float targetDensity = TARGET_DPI / 160f;
        updated.densityDpi = TARGET_DPI;
        updated.density = targetDensity;
        updated.scaledDensity = targetDensity * configuration.fontScale;
    }
}
