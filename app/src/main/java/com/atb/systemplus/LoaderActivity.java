package com.atb.systemplus;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.atb.systemplus.hook.HookManager;
import com.atb.systemplus.hook.HookObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;

public final class LoaderActivity extends AppCompatActivity {

    private static final int REQUEST_NONE = 0;
    private static final int REQUEST_IMPORT_UI_XML = 1;
    private static final int REQUEST_IMPORT_HOOK_XML = 2;
    private static final int REQUEST_EXPORT_ZIP = 3;
    private static final int REQUEST_EXPORT_UI_XML = 4;
    private static final int REQUEST_EXPORT_HOOK_XML = 5;

    private final List<HookObject> hooks = new ArrayList<HookObject>();

    private HookManager hookManager;

    private Button importButton;
    private Button exportButton;
    private Button customSettingsButton;
    private RecyclerView hooksRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;

    private HookAdapter adapter;

    private int requestMode = REQUEST_NONE;
    private Uri pendingUiUri;
    private String pendingUiXml;
    private HookObject pendingExportHook;

    private final ActivityResultLauncher<Intent> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleOpenDocumentResult
    );

    private final ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleCreateDocumentResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DpiCompat.apply(this);
        setContentView(R.layout.activity_loader);

        hookManager = HookManager.getInstance(this);
        setupViews();
        refreshHooks();
    }

    private void setupViews() {
        importButton = findViewById(R.id.btnImportHook);
        exportButton = findViewById(R.id.btnExportHook);
        customSettingsButton = findViewById(R.id.btnCustomSettings);
        hooksRecyclerView = findViewById(R.id.loadedHooksRecyclerView);
        progressBar = findViewById(R.id.loaderProgress);
        emptyText = findViewById(R.id.emptyHooksText);

        hooksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HookAdapter();
        hooksRecyclerView.setAdapter(adapter);

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginImportFlow();
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHookChooserForExport();
            }
        });

        customSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoaderActivity.this, CustomSettingsActivity.class));
            }
        });
    }

    private void beginImportFlow() {
        resetImportFlow();
        launchOpenXmlPicker(REQUEST_IMPORT_UI_XML);
    }

    private void launchOpenXmlPicker(int mode) {
        requestMode = mode;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/xml", "application/xml"});
        openDocumentLauncher.launch(intent);
    }

    private void launchCreateDocument(int mode, String filename, String mimeType) {
        requestMode = mode;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        createDocumentLauncher.launch(intent);
    }

    private void handleOpenDocumentResult(ActivityResult result) {
        final int handledMode = requestMode;

        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            requestMode = REQUEST_NONE;
            if (handledMode == REQUEST_IMPORT_UI_XML || handledMode == REQUEST_IMPORT_HOOK_XML) {
                resetImportFlow();
            }
            return;
        }

        Uri selectedUri = result.getData().getData();
        takePersistablePermission(result.getData(), selectedUri);

        String xmlContent;
        try {
            xmlContent = readTextFromUri(selectedUri);
            validateXmlContent(xmlContent);
        } catch (Exception exception) {
            requestMode = REQUEST_NONE;
            resetImportFlow();
            showToast(getString(R.string.loader_error_read_xml, exception.getMessage()));
            return;
        }

        if (handledMode == REQUEST_IMPORT_UI_XML) {
            pendingUiUri = selectedUri;
            pendingUiXml = xmlContent;
            showToast(getString(R.string.loader_toast_pick_hook_xml));
            launchOpenXmlPicker(REQUEST_IMPORT_HOOK_XML);
            return;
        }

        if (handledMode == REQUEST_IMPORT_HOOK_XML) {
            if (pendingUiUri == null || TextUtils.isEmpty(pendingUiXml)) {
                requestMode = REQUEST_NONE;
                resetImportFlow();
                showToast(getString(R.string.loader_error_import_state));
                return;
            }

            Uri uiUri = pendingUiUri;
            String uiXml = pendingUiXml;
            String hookXml = xmlContent;
            String suggestedName = buildSuggestedName(uiUri, selectedUri);

            requestMode = REQUEST_NONE;
            resetImportFlow();
            setBusy(true);

            hookManager.importHook(uiUri, selectedUri, uiXml, hookXml, suggestedName, new HookManager.Callback<HookObject>() {
                @Override
                public void onSuccess(HookObject value) {
                    setBusy(false);
                    showToast(getString(R.string.loader_toast_import_success, value.displayName));
                    refreshHooks();
                }

                @Override
                public void onError(String message, Throwable throwable) {
                    setBusy(false);
                    showToast(message);
                }
            });
            return;
        }

        requestMode = REQUEST_NONE;
    }

    private void handleCreateDocumentResult(ActivityResult result) {
        final int handledMode = requestMode;

        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null || result.getData().getData() == null) {
            requestMode = REQUEST_NONE;
            resetExportFlow();
            return;
        }

        if (pendingExportHook == null) {
            requestMode = REQUEST_NONE;
            showToast(getString(R.string.loader_error_no_export_target));
            return;
        }

        Uri destinationUri = result.getData().getData();
        takePersistablePermission(result.getData(), destinationUri);
        String hookId = pendingExportHook.id;

        if (handledMode == REQUEST_EXPORT_ZIP) {
            requestMode = REQUEST_NONE;
            setBusy(true);
            hookManager.exportHookAsZip(hookId, destinationUri, new HookManager.Callback<String>() {
                @Override
                public void onSuccess(String value) {
                    setBusy(false);
                    showToast(getString(R.string.loader_toast_export_success));
                    resetExportFlow();
                }

                @Override
                public void onError(String message, Throwable throwable) {
                    setBusy(false);
                    showToast(message);
                    resetExportFlow();
                }
            });
            return;
        }

        if (handledMode == REQUEST_EXPORT_UI_XML) {
            setBusy(true);
            hookManager.exportHookAsXml(hookId, destinationUri, true, new HookManager.Callback<String>() {
                @Override
                public void onSuccess(String value) {
                    setBusy(false);
                    showToast(getString(R.string.loader_toast_export_ui_done));
                    if (pendingExportHook == null) {
                        resetExportFlow();
                        return;
                    }
                    String baseName = toSafeFileStem(pendingExportHook.displayName);
                    launchCreateDocument(REQUEST_EXPORT_HOOK_XML, baseName + "_hook.xml", "text/xml");
                }

                @Override
                public void onError(String message, Throwable throwable) {
                    setBusy(false);
                    showToast(message);
                    resetExportFlow();
                }
            });
            return;
        }

        if (handledMode == REQUEST_EXPORT_HOOK_XML) {
            requestMode = REQUEST_NONE;
            setBusy(true);
            hookManager.exportHookAsXml(hookId, destinationUri, false, new HookManager.Callback<String>() {
                @Override
                public void onSuccess(String value) {
                    setBusy(false);
                    showToast(getString(R.string.loader_toast_export_success));
                    resetExportFlow();
                }

                @Override
                public void onError(String message, Throwable throwable) {
                    setBusy(false);
                    showToast(message);
                    resetExportFlow();
                }
            });
            return;
        }

        requestMode = REQUEST_NONE;
    }

    private void showHookChooserForExport() {
        if (hooks.isEmpty()) {
            showToast(getString(R.string.loader_error_empty_list));
            return;
        }

        CharSequence[] labels = new CharSequence[hooks.size()];
        for (int i = 0; i < hooks.size(); i++) {
            labels[i] = hooks.get(i).displayName;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.loader_export_choose_hook)
                .setItems(labels, (dialog, which) -> showExportModeDialog(hooks.get(which)))
                .show();
    }

    private void showExportModeDialog(HookObject hookObject) {
        pendingExportHook = hookObject;
        CharSequence[] modes = new CharSequence[]{
                getString(R.string.loader_export_mode_zip),
                getString(R.string.loader_export_mode_dual_xml)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.loader_export_mode_title)
                .setItems(modes, (dialog, which) -> {
                    String baseName = toSafeFileStem(hookObject.displayName);
                    if (which == 0) {
                        launchCreateDocument(REQUEST_EXPORT_ZIP, baseName + ".zip", "application/zip");
                    } else {
                        launchCreateDocument(REQUEST_EXPORT_UI_XML, baseName + "_ui.xml", "text/xml");
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> resetExportFlow())
                .show();
    }

    private void showDeleteDialog(HookObject hookObject) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.loader_delete_title)
                .setMessage(getString(R.string.loader_delete_message, hookObject.displayName))
                .setPositiveButton(R.string.loader_delete_confirm, (dialog, which) -> {
                    setBusy(true);
                    hookManager.deleteHook(hookObject.id, new HookManager.Callback<String>() {
                        @Override
                        public void onSuccess(String value) {
                            setBusy(false);
                            refreshHooks();
                        }

                        @Override
                        public void onError(String message, Throwable throwable) {
                            setBusy(false);
                            showToast(message);
                        }
                    });
                })
                .setNegativeButton(R.string.loader_delete_cancel, null)
                .show();
    }

    private void refreshHooks() {
        setBusy(true);
        hookManager.listHooks(new HookManager.Callback<List<HookObject>>() {
            @Override
            public void onSuccess(List<HookObject> value) {
                hooks.clear();
                hooks.addAll(value);
                adapter.notifyDataSetChanged();
                setBusy(false);
            }

            @Override
            public void onError(String message, Throwable throwable) {
                hooks.clear();
                adapter.notifyDataSetChanged();
                setBusy(false);
                showToast(message);
            }
        });
    }

    private void setBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        importButton.setEnabled(!busy);
        exportButton.setEnabled(!busy);
        customSettingsButton.setEnabled(!busy);
        hooksRecyclerView.setEnabled(!busy);

        if (busy) {
            emptyText.setVisibility(View.GONE);
        } else {
            updateEmptyState();
        }
    }

    private void updateEmptyState() {
        emptyText.setVisibility(hooks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void resetImportFlow() {
        pendingUiUri = null;
        pendingUiXml = null;
    }

    private void resetExportFlow() {
        pendingExportHook = null;
        if (requestMode == REQUEST_EXPORT_ZIP || requestMode == REQUEST_EXPORT_UI_XML || requestMode == REQUEST_EXPORT_HOOK_XML) {
            requestMode = REQUEST_NONE;
        }
    }

    private void takePersistablePermission(Intent data, Uri uri) {
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (flags == 0) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, flags);
        } catch (SecurityException ignored) {
        }
    }

    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("stream is null");
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
        }
        return stringBuilder.toString();
    }

    private void validateXmlContent(String xmlContent) throws Exception {
        if (TextUtils.isEmpty(xmlContent) || TextUtils.isEmpty(xmlContent.trim())) {
            throw new IllegalArgumentException(getString(R.string.loader_error_empty_xml));
        }

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xmlContent));
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            // Only parse to validate structure.
        }
    }

    private String buildSuggestedName(Uri uiUri, Uri hookUri) {
        String hookName = trimExtension(queryDisplayName(hookUri));
        String uiName = trimExtension(queryDisplayName(uiUri));

        if (!TextUtils.isEmpty(hookName)) {
            return "Hook-" + hookName;
        }
        if (!TextUtils.isEmpty(uiName)) {
            return "Hook-" + uiName;
        }
        return "Hook-" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        return cursor.getString(index);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        String segment = uri.getLastPathSegment();
        return segment == null ? "" : segment;
    }

    private String trimExtension(String source) {
        if (TextUtils.isEmpty(source)) {
            return "";
        }
        int index = source.lastIndexOf('.');
        if (index <= 0) {
            return source;
        }
        return source.substring(0, index);
    }

    private String toSafeFileStem(String source) {
        if (TextUtils.isEmpty(source)) {
            return "custom_hook";
        }
        String cleaned = source.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        return TextUtils.isEmpty(cleaned) ? "custom_hook" : cleaned;
    }

    private String formatCreatedAt(long createdAt) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(createdAt));
    }

    private String shortId(String id) {
        if (TextUtils.isEmpty(id)) {
            return "unknown";
        }
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private final class HookAdapter extends RecyclerView.Adapter<HookViewHolder> {

        @NonNull
        @Override
        public HookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_loaded_hook, parent, false);
            return new HookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HookViewHolder holder, int position) {
            holder.bind(hooks.get(position));
        }

        @Override
        public int getItemCount() {
            return hooks.size();
        }
    }

    private final class HookViewHolder extends RecyclerView.ViewHolder {

        private final TextView hookNameText;
        private final TextView hookMetaText;
        private final SwitchCompat enabledSwitch;
        private final Button exportItemButton;
        private final Button deleteItemButton;

        HookViewHolder(@NonNull View itemView) {
            super(itemView);
            hookNameText = itemView.findViewById(R.id.hookNameText);
            hookMetaText = itemView.findViewById(R.id.hookMetaText);
            enabledSwitch = itemView.findViewById(R.id.switchHookEnabled);
            exportItemButton = itemView.findViewById(R.id.btnHookExportItem);
            deleteItemButton = itemView.findViewById(R.id.btnHookDeleteItem);
        }

        void bind(HookObject hookObject) {
            hookNameText.setText(hookObject.displayName);
            hookMetaText.setText(getString(R.string.loader_hook_meta, formatCreatedAt(hookObject.createdAt), shortId(hookObject.id)));

            enabledSwitch.setOnCheckedChangeListener(null);
            enabledSwitch.setChecked(hookObject.enabled);
            enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                hookManager.setEnabled(hookObject.id, isChecked, new HookManager.Callback<HookObject>() {
                    @Override
                    public void onSuccess(HookObject value) {
                        hookObject.enabled = value.enabled;
                    }

                    @Override
                    public void onError(String message, Throwable throwable) {
                        showToast(message);
                        refreshHooks();
                    }
                });
            });

            exportItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showExportModeDialog(hookObject);
                }
            });

            deleteItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteDialog(hookObject);
                }
            });
        }
    }
}
