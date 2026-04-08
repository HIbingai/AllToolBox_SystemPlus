package com.atb.systemplus;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.atb.systemplus.bean.Notice;

public final class NoticeInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_notice_info);

        TextView titleView = findViewById(R.id.title_textview);
        TextView contentView = findViewById(R.id.content_textview);

        Object serialized = getIntent().getSerializableExtra("notice");
        if (!(serialized instanceof Notice)) {
            finish();
            return;
        }

        Notice notice = (Notice) serialized;
        titleView.setText(notice.getTitle());
        contentView.setText(notice.getContent());
    }
}
