package com.atb.systemplus;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public final class ActiveSelfActivity extends AppCompatActivity {

    private boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_active_self);

        findViewById(R.id.active_button).setOnClickListener(v -> startActivation("com.atb.systemplus"));
        findViewById(R.id.active_button2).setOnClickListener(v -> startActivation("com.coderstory.toolkit"));
        findViewById(R.id.active_button3).setOnClickListener(v -> startActivation("me.weishu.corepatch"));
    }

    private void startActivation(String packageName) {
        if (running) {
            Toast.makeText(this, "已经点击过了，还在执行中", Toast.LENGTH_SHORT).show();
            return;
        }

        running = true;
        Toast.makeText(this, "开始执行：" + packageName, Toast.LENGTH_SHORT).show();

        View root = findViewById(android.R.id.content);
        root.postDelayed(() -> {
            running = false;
            Toast.makeText(this, "执行完成（模拟流程）", Toast.LENGTH_SHORT).show();
        }, 1200);
    }
}
