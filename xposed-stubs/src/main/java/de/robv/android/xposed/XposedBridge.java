package de.robv.android.xposed;

import java.util.Collections;
import java.util.Set;

public final class XposedBridge {
    private XposedBridge() {
    }

    public static void log(String text) {
    }

    public static Set<XC_MethodHook.Unhook> hookAllMethods(
            Class<?> targetClass,
            String methodName,
            XC_MethodHook callback
    ) {
        return Collections.emptySet();
    }
}
