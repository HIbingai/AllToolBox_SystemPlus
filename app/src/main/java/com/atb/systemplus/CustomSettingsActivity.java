package com.atb.systemplus;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import com.atb.systemplus.hook.HookManager;

public final class CustomSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_custom_settings);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.customSettingsContainer, new CustomSettingsFragment());
            transaction.commit();
        }
    }

    public static final class CustomSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(HookManager.SETTINGS_PREF_NAME);
            setPreferencesFromResource(R.xml.prefs_custom_hook, rootKey);
        }
    }
}
