package com.atb.systemplus;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public final class UpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_update);

        TextView versionView = findViewById(R.id.version_view);
        TextView updateMessage = findViewById(R.id.update_message);

        int versionCode = getVersionCode();
        if (versionCode <= 1) {
            versionCode = 30320;
        }

        versionView.setText(getString(R.string.current_version, Integer.valueOf(versionCode)));
        updateMessage.setText(getString(R.string.update_no_available));
    }

    private int getVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return 0;
        }
    }
}