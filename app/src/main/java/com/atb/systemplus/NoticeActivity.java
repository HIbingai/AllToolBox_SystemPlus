package com.atb.systemplus;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public final class NoticeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_notice);
    }
}
