package com.atb.systemplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

public final class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new SettingsFragment());
            transaction.commit();
        }
    }

    public static final class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            view.setBackgroundColor(Color.WHITE);
            RecyclerView listView = getListView();
            if (listView != null) {
                listView.setBackgroundColor(Color.WHITE);
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName("conf");
            setPreferencesFromResource(R.xml.prefs, rootKey);
            bindEntryNavigation();
        }

        private void bindEntryNavigation() {
            bindGoToActivity("Unlock", UnlockActivity.class);
            bindGoToActivity("ActiveSelf", ActiveSelfActivity.class);
            bindGoToActivity("Notice", NoticeActivity.class);
            bindGoToActivity("Update", UpdateActivity.class);
            bindAppList("disableSwipeApps");
            bindAppList("DontKillApps");
        }

        private void bindGoToActivity(String prefKey, final Class<?> targetActivity) {
            Preference preference = findPreference(prefKey);
            if (preference == null) {
                return;
            }
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(requireContext(), targetActivity));
                    return true;
                }
            });
        }

        private void bindAppList(final String prefKey) {
            Preference preference = findPreference(prefKey);
            if (preference == null) {
                return;
            }
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(requireContext(), AppListActivity.class);
                    intent.putExtra(AppListActivity.EXTRA_PREF_KEY, prefKey);
                    intent.putExtra("pref", prefKey);
                    startActivity(intent);
                    return true;
                }
            });
        }
    }
}
