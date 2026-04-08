package com.atb.systemplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.atb.systemplus.bean.Notice;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class NoticeActivity extends AppCompatActivity {

    private final List<Notice> noticeList = new ArrayList<Notice>();
    private NoticeAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearProgressIndicator progressIndicator;
    private boolean usePrimarySource = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_notice);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        progressIndicator = findViewById(R.id.progressIndicator);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NoticeAdapter(noticeList, notice -> {
            Intent intent = new Intent(NoticeActivity.this, NoticeInfoActivity.class);
            intent.putExtra("notice", notice);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::refreshNotices);
        refreshNotices();
    }

    private void refreshNotices() {
        progressIndicator.setVisibility(LinearProgressIndicator.VISIBLE);

        noticeList.clear();
        noticeList.addAll(buildSource(usePrimarySource));
        adapter.notifyDataSetChanged();

        swipeRefresh.setRefreshing(false);
        progressIndicator.setVisibility(LinearProgressIndicator.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notice_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_source) {
            usePrimarySource = !usePrimarySource;
            refreshNotices();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Notice> buildSource(boolean primary) {
        List<Notice> list = new ArrayList<Notice>();
        long now = System.currentTimeMillis();

        if (primary) {
            list.add(newNotice("SystemPlus 3.3.2 发布", "主界面已切回旧版 SettingActivity 结构。", now));
            list.add(newNotice("兼容性说明", "建议在启用关键开关后重启系统。", now - 86400000L));
        } else {
            list.add(newNotice("备用源公告", "当前为备用数据源，内容可能滞后。", now));
            list.add(newNotice("提示", "如需最新公告，请在菜单中切回主数据源。", now - 43200000L));
        }
        return list;
    }

    private Notice newNotice(String title, String content, long time) {
        Notice notice = new Notice();
        notice.setTitle(title + "  " + formatTime(time));
        notice.setContent(content);
        notice.setTime(time);
        return notice;
    }

    private String formatTime(long time) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
    }

    private interface OnNoticeClickListener {
        void onNoticeClick(Notice notice);
    }

    private static final class NoticeAdapter extends RecyclerView.Adapter<NoticeViewHolder> {

        private final List<Notice> data;
        private final OnNoticeClickListener listener;

        NoticeAdapter(List<Notice> data, OnNoticeClickListener listener) {
            this.data = data;
            this.listener = listener;
        }

        @NonNull
        @Override
        public NoticeViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notice, parent, false);
            return new NoticeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NoticeViewHolder holder, int position) {
            Notice notice = data.get(position);
            holder.title.setText(notice.getTitle());
            holder.content.setText(notice.getContent());
            holder.itemView.setOnClickListener(v -> listener.onNoticeClick(notice));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static final class NoticeViewHolder extends RecyclerView.ViewHolder {

        final android.widget.TextView title;
        final android.widget.TextView content;

        NoticeViewHolder(@NonNull android.view.View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notice_title);
            content = itemView.findViewById(R.id.notice_content);
        }
    }
}
