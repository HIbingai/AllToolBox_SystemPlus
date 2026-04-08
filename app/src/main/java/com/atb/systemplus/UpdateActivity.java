package com.atb.systemplus;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public final class UpdateActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private View confirmCard;
    private ProgressBar requestingIndicator;
    private ProgressBar progressBar;
    private TextView downloadProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_update);

        TextView versionView = findViewById(R.id.version_view);
        Button checkButton = findViewById(R.id.check_update_button);
        requestingIndicator = findViewById(R.id.requesting_indicator);
        confirmCard = findViewById(R.id.confirm_card);
        Button updateButton = findViewById(R.id.update_button);
        Button cancelButton = findViewById(R.id.cancel_button);
        progressBar = findViewById(R.id.progress_bar);
        downloadProgress = findViewById(R.id.download_progress);

        versionView.setText(getString(R.string.current_version, getVersionName(), getVersionCode()));
        progressBar.setMax(100);
        progressBar.setProgress(0);
        downloadProgress.setText(getString(R.string.progress_not_download));

        checkButton.setOnClickListener(v -> checkForUpdate());
        updateButton.setOnClickListener(v -> startFakeDownload());
        cancelButton.setOnClickListener(v -> {
            confirmCard.setVisibility(View.GONE);
        });
    }

    private void checkForUpdate() {
        requestingIndicator.setVisibility(View.VISIBLE);
        handler.postDelayed(() -> {
            requestingIndicator.setVisibility(View.GONE);
            confirmCard.setVisibility(View.VISIBLE);
        }, 800);
    }

    private void startFakeDownload() {
        progressBar.setProgress(0);
        updateProgress(0);
        runProgressStep(0);
    }

    private void runProgressStep(int progress) {
        int next = progress + 10;
        updateProgress(next);
        if (next >= 100) {
            return;
        }
        handler.postDelayed(() -> runProgressStep(next), 180);
    }

    private void updateProgress(int progress) {
        progressBar.setProgress(progress);
        downloadProgress.setText(getString(R.string.downloaded_progress, progress + "%"));
    }

    private int getVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return 0;
        }
    }

    private String getVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            return "0.0.0";
        }
    }
}
