package de.robv.android.xposed;

public interface IXposedHookZygoteInit {

    final class StartupParam {
        public String modulePath;
    }

    void initZygote(StartupParam startupParam) throws Throwable;
}
