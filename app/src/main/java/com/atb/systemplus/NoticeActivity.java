package com.atb.systemplus;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public final class NoticeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_notice);

        TextView titleView = findViewById(R.id.notice_title);
        TextView contentView = findViewById(R.id.notice_content);
        TextView footerView = findViewById(R.id.notice_footer);

        titleView.setText(getString(R.string.notice_simple_title));
        contentView.setText(getString(R.string.notice_simple_content));
        footerView.setText(getString(R.string.notice_simple_footer));
    }
}
