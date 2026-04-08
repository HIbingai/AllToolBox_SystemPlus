package com.atb.systemplus;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public final class ActiveModuleActivity extends AppCompatActivity {

    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("模块自激活");
        title.setTextSize(20f);

        statusView = new TextView(this);
        statusView.setTextSize(15f);
        statusView.setPadding(0, 18, 0, 18);

        Button runButton = new Button(this);
        runButton.setText("开始执行");
        runButton.setAllCaps(false);
        runButton.setOnClickListener(v -> runActivation());

        root.addView(title);
        root.addView(statusView);
        root.addView(runButton);

        setContentView(root);
        statusView.setText("状态：等待执行");
    }

    private void runActivation() {
        statusView.setText("状态：执行中...");

        new Thread(() -> {
            boolean rootReady = canUseSu();
            runOnUiThread(() -> {
                if (rootReady) {
                    SharedPreferences prefs = getSharedPreferences("conf", MODE_PRIVATE);
                    prefs.edit().putLong("active_module_last_time", System.currentTimeMillis()).apply();
                    statusView.setText("状态：执行完成（已写入激活记录）");
                    Toast.makeText(this, "执行成功", Toast.LENGTH_SHORT).show();
                } else {
                    statusView.setText("状态：执行失败（未获取 root）");
                    Toast.makeText(this, "未检测到可用 root", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private boolean canUseSu() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su -c exit");
            int code = process.waitFor();
            return code == 0;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
