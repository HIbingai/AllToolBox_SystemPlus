package com.atb.systemplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AppListActivity extends AppCompatActivity {

    public static final String EXTRA_PREF_KEY = "pref_key";
    private static final String PREF_FILE = "conf";

    private final List<AppItem> items = new ArrayList<AppItem>();

    private String prefKey;
    private SharedPreferences prefs;
    private RecyclerView recyclerView;
    private android.widget.ProgressBar progressBar;
    private AppAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_app_list);

        prefKey = getIntent().getStringExtra(EXTRA_PREF_KEY);
        if (TextUtils.isEmpty(prefKey)) {
            prefKey = getIntent().getStringExtra("pref");
        }
        if (TextUtils.isEmpty(prefKey)) {
            Toast.makeText(this, "缺少配置键", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        prefs = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        recyclerView = findViewById(R.id.appsRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter();
        recyclerView.setAdapter(adapter);

        loadAppsAsync();
    }

    @Override
    protected void onStop() {
        super.onStop();
        persistSelection();
    }

    private void loadAppsAsync() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        new Thread(() -> {
            List<AppItem> loaded = queryApps();
            runOnUiThread(() -> {
                items.clear();
                items.addAll(loaded);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(android.view.View.GONE);
            });
        }).start();
    }

    private List<AppItem> queryApps() {
        PackageManager pm = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        Set<String> selected = prefs.getStringSet(prefKey, new HashSet<String>());
        List<ResolveInfo> infos = pm.queryIntentActivities(launcherIntent, 0);

        Collections.sort(infos, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo o1, ResolveInfo o2) {
                CharSequence l1 = o1.loadLabel(getPackageManager());
                CharSequence l2 = o2.loadLabel(getPackageManager());
                String s1 = l1 == null ? "" : l1.toString();
                String s2 = l2 == null ? "" : l2.toString();
                return s1.compareToIgnoreCase(s2);
            }
        });

        ArrayList<AppItem> result = new ArrayList<AppItem>();
        for (ResolveInfo info : infos) {
            ActivityInfo activityInfo = info.activityInfo;
            if (activityInfo == null || TextUtils.isEmpty(activityInfo.packageName)) {
                continue;
            }

            String packageName = activityInfo.packageName;
            String label = String.valueOf(info.loadLabel(pm));
            AppItem item = new AppItem();
            item.appLabel = label;
            item.packageName = packageName;
            item.selected = selected != null && selected.contains(packageName);
            result.add(item);
        }
        return result;
    }

    private void persistSelection() {
        HashSet<String> selected = new HashSet<String>();
        for (AppItem item : items) {
            if (item.selected) {
                selected.add(item.packageName);
            }
        }
        prefs.edit().putStringSet(prefKey, selected).apply();
    }

    private static final class AppItem {
        String appLabel;
        String packageName;
        boolean selected;
    }

    private final class AppAdapter extends RecyclerView.Adapter<AppViewHolder> {

        @Override
        public AppViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_app_select, parent, false);
            return new AppViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AppViewHolder holder, int position) {
            AppItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private final class AppViewHolder extends RecyclerView.ViewHolder {

        private final android.widget.CheckBox checkBox;
        private final android.widget.TextView title;
        private final android.widget.TextView subTitle;

        AppViewHolder(android.view.View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.app_check);
            title = itemView.findViewById(R.id.app_title);
            subTitle = itemView.findViewById(R.id.app_pkg);
        }

        void bind(AppItem item) {
            title.setText(item.appLabel);
            subTitle.setText(item.packageName);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(item.selected);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.selected = isChecked);

            itemView.setOnClickListener(v -> {
                boolean next = !checkBox.isChecked();
                checkBox.setChecked(next);
            });
        }
    }
}
