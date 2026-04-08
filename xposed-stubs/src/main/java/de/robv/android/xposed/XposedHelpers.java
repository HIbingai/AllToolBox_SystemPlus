package de.robv.android.xposed;

public final class XposedHelpers {
    private XposedHelpers() {
    }

    public static Object findAndHookMethod(
            String className,
            ClassLoader classLoader,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return null;
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader == null) {
            return Class.forName(className);
        }
        return Class.forName(className, false, classLoader);
    }

    public static void setStaticBooleanField(Class<?> clazz, String fieldName, boolean value) {
    }
}
