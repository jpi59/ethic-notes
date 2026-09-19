/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethicnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Ethic Notes: Ultra-secure, zero-permission, copyleft personal notes manager.
 * Inspired by the clean, ergonomic, copyleft visual aesthetic of Ethic Tuner.
 */
public class MainActivity extends Activity {

    private static final String PREFS_NAME = "appearance";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_SHIELD = "privacy_shield";

    private static final int REQ_EXPORT_NOTE = 1001;
    private static final int REQ_EXPORT_ALL = 1002;
    private static final int REQ_IMPORT = 1003;

    private static final String[][] ECOSYSTEM_APPS = {
            {"Ethic Tuner", "org.jpi59.ethictuner", "Afinador cromático de máxima precisión", "https://github.com/jpi59/ethic-tuner"},
            {"Ethic Compass", "org.jpi59.ethiccompass", "Brújula offline de sensores puros", "https://github.com/jpi59/ethic-compass"},
            {"Ethic QR Scanner", "org.jpi59.ethicqrscanner", "Lector QR sin rastreadores", "https://github.com/jpi59/ethic-qr-scanner"},
            {"Ethic Keyboard", "org.jpi59.teclado", "Teclado privado sin conexión a red", "https://github.com/jpi59/ethic-keyboard"},
            {"Ethic APK Guard", "org.jpi59.ethicupdatesafe", "Instalador y verificador seguro de APKs", "https://github.com/jpi59/ethic-apk-guard"},
            {"Ethic One Call", "org.jpi59.ethichandoff", "Gestor ético de llamadas de emergencia", "https://github.com/jpi59/ethic-one-call"}
    };

    private NotesDbHelper dbHelper;
    private NotesAdapter adapter;

    private View rootLayout;
    private View containerList;
    private View containerEditor;
    private ListView listView;
    private View emptyStateLayout;
    private TextView emptyTitle;
    private TextView emptyDesc;
    private EditText searchInput;
    private TextView appTitle;
    private TextView appSubtitle;

    private View headerBar;
    private View headerSelectionBar;
    private TextView selectionTitle;
    private ImageButton btnSelectionCancel;
    private ImageButton btnSelectionAll;
    private ImageButton btnSelectionDelete;

    private ImageButton btnMenu;
    private ImageButton btnThemeToggle;
    private ImageButton btnPrivacyShield;
    private ImageButton fabAddNote;

    private ImageButton btnEditorBack;
    private ImageButton btnEditorShare;
    private ImageButton btnEditorExport;
    private ImageButton btnEditorDelete;
    private ImageButton btnEditorSave;
    private EditText editorTitle;
    private EditText editorContent;
    private TextView editorWordCharCount;
    private View editorDivider;
    private View editorActionBar;

    private AlertDialog settingsDialog;

    private long currentNoteId = -1;
    private boolean darkMode = false;
    private boolean isPrivacyShieldActive = false;

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = this::autoSaveCurrentNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        darkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        isPrivacyShieldActive = prefs.getBoolean(PREF_SHIELD, false);

        dbHelper = new NotesDbHelper(this);

        initViews();
        setupWindowInsets();
        applyPrivacyShield();
        setupList();
        setupSearch();
        setupEditor();
        applyTheme();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void initViews() {
        rootLayout = findViewById(R.id.main_root);
        containerList = findViewById(R.id.container_list);
        containerEditor = findViewById(R.id.container_editor);
        listView = findViewById(R.id.notes_list_view);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        emptyTitle = findViewById(R.id.empty_title);
        emptyDesc = findViewById(R.id.empty_desc);
        searchInput = findViewById(R.id.search_input);
        appTitle = findViewById(R.id.app_title);
        appSubtitle = findViewById(R.id.app_subtitle);

        headerBar = findViewById(R.id.header_bar);
        headerSelectionBar = findViewById(R.id.header_selection_bar);
        selectionTitle = findViewById(R.id.selection_title);
        btnSelectionCancel = findViewById(R.id.btn_selection_cancel);
        btnSelectionAll = findViewById(R.id.btn_selection_all);
        btnSelectionDelete = findViewById(R.id.btn_selection_delete);

        btnSelectionCancel.setOnClickListener(v -> exitSelectionMode());
        btnSelectionAll.setOnClickListener(v -> toggleSelectAll());
        btnSelectionDelete.setOnClickListener(v -> confirmDeleteSelectedNotes());

        btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> showEcosystemMenu());

        btnThemeToggle = findViewById(R.id.btn_theme_toggle);
        btnThemeToggle.setOnClickListener(v -> toggleTheme());

        btnPrivacyShield = findViewById(R.id.btn_privacy_shield);
        btnPrivacyShield.setOnClickListener(v -> togglePrivacyShield());

        fabAddNote = findViewById(R.id.fab_add_note);
        fabAddNote.setOnClickListener(v -> openEditor(-1));

        editorActionBar = findViewById(R.id.editor_action_bar);
        btnEditorBack = findViewById(R.id.btn_editor_back);
        btnEditorShare = findViewById(R.id.btn_editor_share);
        btnEditorExport = findViewById(R.id.btn_editor_export);
        btnEditorDelete = findViewById(R.id.btn_editor_delete);
        btnEditorSave = findViewById(R.id.btn_editor_save);
        editorTitle = findViewById(R.id.editor_title);
        editorContent = findViewById(R.id.editor_content);
        editorWordCharCount = findViewById(R.id.editor_char_word_count);
        editorDivider = findViewById(R.id.editor_divider);
    }

    private void setupWindowInsets() {
        if (rootLayout == null) return;
        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = 0, top = 0, right = 0, bottom = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                );
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        rootLayout.requestApplyInsets();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundedBackground(int fill, int stroke, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill);
        gd.setCornerRadius(dp(radiusDp));
        gd.setStroke(dp(1), stroke);
        return gd;
    }

    private int actionColor() {
        return darkMode ? getColor(R.color.accent_dark) : getColor(R.color.accent);
    }

    private void styleImageButton(ImageButton btn, int iconRes, int tintColor) {
        if (btn == null) return;
        Drawable d = getDrawable(iconRes);
        if (d != null) {
            d = d.mutate();
            d.setTint(tintColor);
            btn.setImageDrawable(d);
        }
        btn.setBackgroundColor(Color.TRANSPARENT);
    }

    private void applyTheme() {
        int ink = darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink);
        int muted = darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted);
        int surface = darkMode ? Color.rgb(20, 23, 21) : getColor(R.color.surface);
        int cardFill = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        int cardStroke = darkMode ? Color.rgb(44, 51, 46) : Color.rgb(228, 226, 220);
        int action = actionColor();

        getWindow().setStatusBarColor(surface);
        getWindow().setNavigationBarColor(surface);
        int systemBars = darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!darkMode && Build.VERSION.SDK_INT >= 26) {
            systemBars |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(systemBars);

        rootLayout.setBackgroundColor(surface);
        containerList.setBackgroundColor(surface);
        containerEditor.setBackgroundColor(surface);
        headerBar.setBackgroundColor(surface);
        headerSelectionBar.setBackgroundColor(surface);
        editorActionBar.setBackgroundColor(surface);

        appTitle.setTextColor(ink);
        appSubtitle.setTextColor(muted);
        emptyTitle.setTextColor(ink);
        emptyDesc.setTextColor(muted);
        selectionTitle.setTextColor(ink);
        editorWordCharCount.setTextColor(muted);

        searchInput.setBackground(roundedBackground(cardFill, cardStroke, 14));
        searchInput.setTextColor(ink);
        searchInput.setHintTextColor(muted);

        styleImageButton(btnMenu, R.drawable.ic_settings_menu, action);
        styleImageButton(btnThemeToggle, darkMode ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon, action);
        btnThemeToggle.setContentDescription(getString(darkMode ? R.string.light_mode : R.string.dark_mode));

        applyPrivacyShield();

        styleImageButton(btnSelectionCancel, R.drawable.ic_close, action);
        styleImageButton(btnSelectionAll, R.drawable.ic_select_all, action);
        styleImageButton(btnSelectionDelete, R.drawable.ic_delete, getColor(R.color.danger));

        int fabFill = action;
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setShape(GradientDrawable.OVAL);
        fabBg.setColor(fabFill);
        fabAddNote.setBackground(fabBg);
        Drawable addIcon = getDrawable(R.drawable.ic_add);
        if (addIcon != null) {
            addIcon = addIcon.mutate();
            addIcon.setTint(darkMode ? Color.rgb(20, 23, 21) : Color.rgb(255, 255, 255));
            fabAddNote.setImageDrawable(addIcon);
        }

        editorTitle.setTextColor(ink);
        editorTitle.setHintTextColor(muted);
        editorContent.setTextColor(ink);
        editorContent.setHintTextColor(muted);
        editorDivider.setBackgroundColor(cardStroke);

        styleImageButton(btnEditorBack, R.drawable.ic_back, action);
        styleImageButton(btnEditorShare, R.drawable.ic_share, action);
        styleImageButton(btnEditorExport, R.drawable.ic_shield, action);
        styleImageButton(btnEditorDelete, R.drawable.ic_delete, getColor(R.color.danger));
        styleImageButton(btnEditorSave, R.drawable.ic_save, action);

        if (adapter != null) {
            adapter.setDarkMode(darkMode);
        }
    }

    private void toggleTheme() {
        darkMode = !darkMode;
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_DARK_MODE, darkMode)
                .apply();
        applyTheme();
    }

    private void applyPrivacyShield() {
        if (isPrivacyShieldActive) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        int action = actionColor();
        int muted = darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted);

        if (btnPrivacyShield != null) {
            if (isPrivacyShieldActive) {
                styleImageButton(btnPrivacyShield, R.drawable.ic_shield_active, action);
                int activeBgFill = darkMode ? Color.argb(60, 114, 205, 187) : Color.argb(45, 20, 92, 82);
                btnPrivacyShield.setBackground(roundedBackground(activeBgFill, action, 14));
                String desc = getString(R.string.privacy_shield_active_desc);
                btnPrivacyShield.setContentDescription(desc);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    btnPrivacyShield.setTooltipText(desc);
                }
            } else {
                styleImageButton(btnPrivacyShield, R.drawable.ic_shield_off, muted);
                btnPrivacyShield.setBackgroundColor(Color.TRANSPARENT);
                String desc = getString(R.string.privacy_shield_inactive_desc);
                btnPrivacyShield.setContentDescription(desc);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    btnPrivacyShield.setTooltipText(desc);
                }
            }
        }
    }

    private void togglePrivacyShield() {
        isPrivacyShieldActive = !isPrivacyShieldActive;
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_SHIELD, isPrivacyShieldActive)
                .apply();
        applyPrivacyShield();

        int msg = isPrivacyShieldActive ? R.string.privacy_shield_enabled : R.string.privacy_shield_disabled;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void triggerLightHaptic() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(35);
                }
            }
        } catch (Exception ignored) { }
    }

    private void setupList() {
        adapter = new NotesAdapter(this);
        adapter.setDarkMode(darkMode);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Note note = adapter.getItem(position);
            if (adapter.isSelectionMode()) {
                adapter.toggleSelection(note.getId());
                updateSelectionState();
            } else {
                openEditor(note.getId());
            }
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Note note = adapter.getItem(position);
            triggerLightHaptic();
            if (!adapter.isSelectionMode()) {
                enterSelectionMode(note.getId());
            } else {
                adapter.toggleSelection(note.getId());
                updateSelectionState();
            }
            return true;
        });

        loadNotes(null);
    }

    private void enterSelectionMode(long initialNoteId) {
        adapter.selectNote(initialNoteId);
        headerBar.setVisibility(View.GONE);
        headerSelectionBar.setVisibility(View.VISIBLE);
        fabAddNote.setVisibility(View.GONE);
        updateSelectionState();
    }

    private void exitSelectionMode() {
        adapter.clearSelection();
        headerSelectionBar.setVisibility(View.GONE);
        headerBar.setVisibility(View.VISIBLE);
        fabAddNote.setVisibility(View.VISIBLE);
    }

    private void updateSelectionState() {
        int count = adapter.getSelectedCount();
        if (count == 0) {
            exitSelectionMode();
            return;
        }
        selectionTitle.setText(String.format(getString(R.string.selected_count), count));
    }

    private void toggleSelectAll() {
        if (adapter.getSelectedCount() == adapter.getCount()) {
            adapter.clearSelection();
            exitSelectionMode();
        } else {
            adapter.selectAll();
            updateSelectionState();
        }
    }

    private void confirmDeleteSelectedNotes() {
        final Set<Long> selectedIds = adapter.getSelectedIds();
        final int count = selectedIds.size();
        if (count == 0) return;

        int surface = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        int action = actionColor();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(String.format(getString(R.string.confirm_delete_selected_title), count))
                .setMessage(getString(R.string.confirm_delete_selected_msg))
                .setPositiveButton(R.string.delete, (d, which) -> {
                    dbHelper.deleteNotes(selectedIds);
                    exitSelectionMode();
                    loadNotes(searchInput != null ? searchInput.getText().toString() : null);
                    Toast.makeText(this, String.format(getString(R.string.notes_deleted_count), count), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
        }
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (pos != null) pos.setTextColor(getColor(R.color.danger));
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (neg != null) neg.setTextColor(action);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter.isSelectionMode()) {
                    exitSelectionMode();
                }
                loadNotes(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadNotes(String query) {
        List<Note> notes = dbHelper.searchNotes(query);
        adapter.setNotes(notes);
        if (notes.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            if (query != null && !query.trim().isEmpty()) {
                emptyDesc.setText(R.string.no_search_results);
            } else {
                emptyDesc.setText(R.string.no_notes_desc);
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    private void setupEditor() {
        btnEditorBack.setOnClickListener(v -> {
            autoSaveCurrentNote();
            closeEditor();
        });

        btnEditorSave.setOnClickListener(v -> {
            boolean saved = autoSaveCurrentNote();
            closeEditor();
            if (saved) {
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
            }
        });

        btnEditorDelete.setOnClickListener(v -> {
            if (currentNoteId != -1) {
                confirmDeleteNote();
            } else {
                closeEditor();
            }
        });

        btnEditorShare.setOnClickListener(v -> shareCurrentNote());
        btnEditorExport.setOnClickListener(v -> exportSingleNoteSaf());

        TextWatcher editorWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCharCounter();
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable, 1200);
            }
            @Override public void afterTextChanged(Editable s) { }
        };

        editorContent.addTextChangedListener(editorWatcher);
        editorTitle.addTextChangedListener(editorWatcher);
    }

    private void openEditor(long noteId) {
        if (adapter.isSelectionMode()) {
            exitSelectionMode();
        }
        currentNoteId = noteId;
        if (noteId != -1) {
            Note note = dbHelper.getNote(noteId);
            if (note != null) {
                editorTitle.setText(note.getRawTitle());
                editorContent.setText(note.getContent());
                btnEditorDelete.setVisibility(View.VISIBLE);
            }
        } else {
            editorTitle.setText("");
            editorContent.setText("");
            btnEditorDelete.setVisibility(View.GONE);
        }
        updateWordCharCounter();
        containerList.setVisibility(View.GONE);
        containerEditor.setVisibility(View.VISIBLE);
        editorContent.requestFocus();
    }

    private synchronized boolean autoSaveCurrentNote() {
        if (containerEditor.getVisibility() != View.VISIBLE) {
            return false;
        }

        String title = editorTitle.getText().toString().trim();
        String content = editorContent.getText().toString();

        if (title.isEmpty() && content.trim().isEmpty()) {
            if (currentNoteId != -1) {
                dbHelper.deleteNote(currentNoteId);
                currentNoteId = -1;
            }
            return false;
        }

        if (currentNoteId == -1) {
            long newId = dbHelper.insertNote(title, content);
            if (newId != -1) {
                currentNoteId = newId;
                btnEditorDelete.setVisibility(View.VISIBLE);
            }
        } else {
            dbHelper.updateNote(currentNoteId, title, content);
        }
        return true;
    }

    private void closeEditor() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        containerEditor.setVisibility(View.GONE);
        containerList.setVisibility(View.VISIBLE);
        currentNoteId = -1;
        loadNotes(searchInput != null ? searchInput.getText().toString() : null);
    }

    private void confirmDeleteNote() {
        int surface = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        int action = actionColor();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton(R.string.delete, (d, which) -> {
                    if (currentNoteId != -1) {
                        dbHelper.deleteNote(currentNoteId);
                        currentNoteId = -1;
                        Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                    }
                    closeEditor();
                })
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
        }
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (pos != null) pos.setTextColor(getColor(R.color.danger));
        Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (neg != null) neg.setTextColor(action);
    }

    private void updateWordCharCounter() {
        String content = editorContent.getText().toString();
        int chars = content.length();
        int words = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        editorWordCharCount.setText(getString(R.string.chars_words_count, chars, words));
    }

    private void shareCurrentNote() {
        autoSaveCurrentNote();
        String title = editorTitle.getText().toString().trim();
        String content = editorContent.getText().toString();
        String fullText = (title.isEmpty() ? "" : title + "\n\n") + content;

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, fullText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.action_share)));
    }

    private void exportSingleNoteSaf() {
        autoSaveCurrentNote();
        String title = editorTitle.getText().toString().trim();
        if (title.isEmpty()) {
            String content = editorContent.getText().toString().trim();
            if (!content.isEmpty()) {
                String firstLine = content.split("\\r?\\n")[0].trim();
                title = firstLine.length() > 30 ? firstLine.substring(0, 30) : firstLine;
            }
        }
        String filename = (title.isEmpty() ? "nota" : title.replaceAll("[^a-zA-Z0-9_-]", "_")) + ".txt";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, REQ_EXPORT_NOTE);
    }

    /* -------------------------------------------------------------
     * MENU DESPLEGABLE Y ECOSISTEMA ÉTICO (3 RAYITAS)
     * ------------------------------------------------------------- */
    private TextView createSectionHeader(String text, int color) {
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextSize(12);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setTextColor(color);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(0, dp(14), 0, dp(6));
        return tv;
    }

    private LinearLayout createCardLayout(int fill, int stroke) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackground(roundedBackground(fill, stroke, 14));
        layout.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(4);
        layout.setLayoutParams(params);
        return layout;
    }

    private View createDivider(int stroke) {
        View v = new View(this);
        v.setBackgroundColor(stroke);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
        p.topMargin = dp(6);
        p.bottomMargin = dp(6);
        v.setLayoutParams(p);
        return v;
    }

    private View createActionRow(int iconRes, String title, String desc, int ink, int muted, int action, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 8));

        ImageView icon = new ImageView(this);
        Drawable d = getDrawable(iconRes);
        if (d != null) {
            d = d.mutate();
            d.setTint(action);
            icon.setImageDrawable(d);
        }
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconParams.setMarginEnd(dp(12));
        row.addView(icon, iconParams);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvTitle.setTextColor(ink);
        textCol.addView(tvTitle);

        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = new TextView(this);
            tvDesc.setText(desc);
            tvDesc.setTextSize(12);
            tvDesc.setTextColor(muted);
            textCol.addView(tvDesc);
        }
        row.addView(textCol);

        ImageView openIcon = new ImageView(this);
        Drawable arrow = getDrawable(R.drawable.ic_open_app);
        if (arrow != null) {
            arrow = arrow.mutate();
            arrow.setTint(muted);
            openIcon.setImageDrawable(arrow);
        }
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        arrowParams.setMarginStart(dp(8));
        row.addView(openIcon, arrowParams);

        row.setOnClickListener(listener);
        return row;
    }

    private View createKeyValueRow(String key, String value, int ink, int muted) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView tvKey = new TextView(this);
        tvKey.setText(key);
        tvKey.setTextSize(12);
        tvKey.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvKey.setTextColor(muted);
        row.addView(tvKey);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(13);
        tvVal.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tvVal.setTextColor(ink);
        row.addView(tvVal);
        return row;
    }

    private void showEcosystemMenu() {
        int surface = darkMode ? Color.rgb(20, 23, 21) : Color.rgb(248, 247, 243);
        int dialogSurface = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        int cardStroke = darkMode ? Color.rgb(44, 51, 46) : Color.rgb(228, 226, 220);
        int ink = darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink);
        int muted = darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted);
        int action = actionColor();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(20));

        // 1. Header with App Icon and Badge
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(14));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(getDrawable(R.drawable.ic_launcher));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.setMarginEnd(dp(12));
        header.addView(icon, iconParams);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText("Ethic Notes · v1.0.0");
        title.setTextSize(18);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setTextColor(ink);
        titleCol.addView(title);

        TextView badge = new TextView(this);
        badge.setText(R.string.zero_permissions_badge);
        badge.setTextSize(12);
        badge.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        badge.setTextColor(action);
        titleCol.addView(badge);
        header.addView(titleCol);
        root.addView(header);

        // 2. Storage & Backup Section
        root.addView(createSectionHeader(getString(R.string.storage_backup_section), action));
        LinearLayout storageCard = createCardLayout(dialogSurface, cardStroke);
        storageCard.addView(createActionRow(
                R.drawable.ic_shield,
                getString(R.string.export_notes_label),
                getString(R.string.export_notes_desc),
                ink, muted, action,
                v -> { if (settingsDialog != null) settingsDialog.dismiss(); exportAllNotesSaf(); }
        ));
        storageCard.addView(createDivider(cardStroke));
        storageCard.addView(createActionRow(
                R.drawable.ic_share,
                getString(R.string.import_notes_label),
                getString(R.string.import_notes_desc),
                ink, muted, action,
                v -> { if (settingsDialog != null) settingsDialog.dismiss(); importNotesSaf(); }
        ));
        root.addView(storageCard);

        // 3. Privacy Shield Section
        root.addView(createSectionHeader(getString(R.string.privacy_shield_label), action));
        LinearLayout shieldCard = createCardLayout(dialogSurface, cardStroke);
        LinearLayout shieldRow = new LinearLayout(this);
        shieldRow.setOrientation(LinearLayout.HORIZONTAL);
        shieldRow.setGravity(Gravity.CENTER_VERTICAL);
        shieldRow.setPadding(0, dp(6), 0, dp(6));

        ImageView shieldDialogIcon = new ImageView(this);
        LinearLayout.LayoutParams shieldIconParams = new LinearLayout.LayoutParams(dp(28), dp(28));
        shieldIconParams.setMarginEnd(dp(12));
        shieldDialogIcon.setLayoutParams(shieldIconParams);
        shieldRow.addView(shieldDialogIcon);

        LinearLayout shieldTextCol = new LinearLayout(this);
        shieldTextCol.setOrientation(LinearLayout.VERTICAL);
        shieldTextCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView shieldTitle = new TextView(this);
        shieldTitle.setText(R.string.privacy_shield_label);
        shieldTitle.setTextSize(15);
        shieldTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        shieldTitle.setTextColor(ink);
        shieldTextCol.addView(shieldTitle);

        TextView shieldStatusBadge = new TextView(this);
        shieldStatusBadge.setTextSize(12);
        shieldStatusBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        shieldStatusBadge.setPadding(0, dp(1), 0, dp(2));
        shieldTextCol.addView(shieldStatusBadge);

        TextView shieldDesc = new TextView(this);
        shieldDesc.setText(R.string.privacy_shield_desc);
        shieldDesc.setTextSize(12);
        shieldDesc.setTextColor(muted);
        shieldTextCol.addView(shieldDesc);
        shieldRow.addView(shieldTextCol);

        Switch shieldSwitch = new Switch(this);
        shieldSwitch.setChecked(isPrivacyShieldActive);

        Runnable updateShieldUi = () -> {
            if (isPrivacyShieldActive) {
                Drawable d = getDrawable(R.drawable.ic_shield_active);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(action);
                    shieldDialogIcon.setImageDrawable(d);
                }
                shieldStatusBadge.setText("● " + getString(R.string.privacy_shield_status_active) + " · " + getString(R.string.privacy_shield_active_badge_desc));
                shieldStatusBadge.setTextColor(action);
            } else {
                Drawable d = getDrawable(R.drawable.ic_shield_off);
                if (d != null) {
                    d = d.mutate();
                    d.setTint(muted);
                    shieldDialogIcon.setImageDrawable(d);
                }
                shieldStatusBadge.setText("○ " + getString(R.string.privacy_shield_status_inactive) + " · " + getString(R.string.privacy_shield_inactive_badge_desc));
                shieldStatusBadge.setTextColor(muted);
            }
        };
        updateShieldUi.run();

        shieldSwitch.setOnCheckedChangeListener((btn, checked) -> {
            if (checked != isPrivacyShieldActive) {
                togglePrivacyShield();
                updateShieldUi.run();
            }
        });
        shieldRow.addView(shieldSwitch);
        shieldCard.addView(shieldRow);
        root.addView(shieldCard);

        // 4. Ethic Ecosystem Section
        root.addView(createSectionHeader(getString(R.string.ecosystem_section), action));
        TextView ecoDesc = new TextView(this);
        ecoDesc.setText(R.string.ecosystem_desc);
        ecoDesc.setTextSize(12);
        ecoDesc.setTextColor(muted);
        ecoDesc.setPadding(0, 0, 0, dp(6));
        root.addView(ecoDesc);

        LinearLayout ecoCard = createCardLayout(dialogSurface, cardStroke);
        PackageManager pm = getPackageManager();
        for (int i = 0; i < ECOSYSTEM_APPS.length; i++) {
            if (i > 0) ecoCard.addView(createDivider(cardStroke));
            final String name = ECOSYSTEM_APPS[i][0];
            final String pkg = ECOSYSTEM_APPS[i][1];
            final String desc = ECOSYSTEM_APPS[i][2];
            final String url = ECOSYSTEM_APPS[i][3];
            final Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
            final boolean isInstalled = launchIntent != null;

            LinearLayout appRow = new LinearLayout(this);
            appRow.setOrientation(LinearLayout.HORIZONTAL);
            appRow.setGravity(Gravity.CENTER_VERTICAL);
            appRow.setPadding(0, dp(8), 0, dp(8));

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

            TextView appNameView = new TextView(this);
            appNameView.setText(name);
            appNameView.setTextSize(15);
            appNameView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            appNameView.setTextColor(ink);
            textCol.addView(appNameView);

            TextView appDescView = new TextView(this);
            appDescView.setText(desc);
            appDescView.setTextSize(12);
            appDescView.setTextColor(muted);
            textCol.addView(appDescView);

            appRow.addView(textCol);

            Button actionBtn = new Button(this);
            actionBtn.setText(isInstalled ? R.string.app_open : R.string.app_view);
            actionBtn.setTextSize(13);
            actionBtn.setAllCaps(false);
            actionBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            if (isInstalled) {
                actionBtn.setTextColor(Color.rgb(255, 255, 255));
                actionBtn.setBackground(roundedBackground(action, action, 14));
            } else {
                actionBtn.setTextColor(action);
                actionBtn.setBackground(roundedBackground(Color.TRANSPARENT, action, 14));
            }
            actionBtn.setPadding(dp(12), dp(4), dp(12), dp(4));
            actionBtn.setOnClickListener(v -> {
                if (isInstalled) {
                    startActivity(launchIntent);
                } else {
                    Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browseIntent);
                }
            });
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(-2, dp(36));
            btnParams.setMarginStart(dp(8));
            appRow.addView(actionBtn, btnParams);
            ecoCard.addView(appRow);
        }
        root.addView(ecoCard);

        // 5. Open Source & Sovereignty
        root.addView(createSectionHeader(getString(R.string.open_source_section), action));
        LinearLayout repoCard = createCardLayout(dialogSurface, cardStroke);
        repoCard.addView(createActionRow(
                R.drawable.ic_code,
                getString(R.string.github_repo_label),
                "github.com/jpi59/ethic-notes",
                ink, muted, action,
                v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jpi59/ethic-notes")))
        ));
        repoCard.addView(createDivider(cardStroke));
        repoCard.addView(createActionRow(
                R.drawable.ic_code,
                getString(R.string.gitlab_repo_label),
                "gitlab.com/jpi59/ethic-notes",
                ink, muted, action,
                v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/jpi59/ethic-notes")))
        ));
        repoCard.addView(createDivider(cardStroke));
        repoCard.addView(createActionRow(
                R.drawable.ic_info,
                getString(R.string.license_label),
                getString(R.string.license_desc),
                ink, muted, action,
                v -> showLicenseDialog()
        ));
        root.addView(repoCard);

        // 6. Technical Integrity
        root.addView(createSectionHeader(getString(R.string.technical_info_section), action));
        LinearLayout techCard = createCardLayout(dialogSurface, cardStroke);
        techCard.addView(createKeyValueRow(getString(R.string.package_id_label), "org.jpi59.ethicnotes", ink, muted));
        techCard.addView(createDivider(cardStroke));
        techCard.addView(createKeyValueRow(getString(R.string.key_fingerprint_label), "72252b0d955ad29dadf2f1219f1990433d6fd3cbb3ce714bdaa69b1342d6d841", ink, muted));
        root.addView(techCard);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(scroll)
                .setPositiveButton(R.string.done, null)
                .create();

        dialog.show();
        settingsDialog = dialog;
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
        }
        Button doneBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (doneBtn != null) {
            doneBtn.setTextColor(action);
            doneBtn.setTextSize(16);
            doneBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
    }

    private void showLicenseDialog() {
        int surface = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        int action = actionColor();

        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.license_label)
                .setMessage("Ethic Notes · Copyright (C) 2026 jpi59\n\n" +
                        "This program is free software: you can redistribute it and/or modify " +
                        "it under the terms of the GNU General Public License as published by " +
                        "the Free Software Foundation, either version 3 of the License, or " +
                        "(at your option) any later version.\n\n" +
                        "This program is distributed in the hope that it will be useful, " +
                        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the " +
                        "GNU General Public License for more details.")
                .setPositiveButton(R.string.done, null)
                .create();
        d.show();
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
        }
        Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
        if (b != null) b.setTextColor(action);
    }

    private void exportAllNotesSaf() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "ethic-notes-backup.json");
        startActivityForResult(intent, REQ_EXPORT_ALL);
    }

    private void importNotesSaf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        if (requestCode == REQ_EXPORT_NOTE) {
            writeSingleNoteToUri(uri);
        } else if (requestCode == REQ_EXPORT_ALL) {
            writeAllNotesToUri(uri);
        } else if (requestCode == REQ_IMPORT) {
            readNotesFromUri(uri);
        }
    }

    private void writeSingleNoteToUri(Uri uri) {
        String title = editorTitle.getText().toString().trim();
        String content = editorContent.getText().toString();
        String text = (title.isEmpty() ? "" : title + "\n\n") + content;

        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(text.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.export_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeAllNotesToUri(Uri uri) {
        String json = dbHelper.exportToJson();
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.export_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void readNotesFromUri(Uri uri) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }

            String content = sb.toString().trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                JSONArray array = new JSONArray(content);
                int count = 0;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String title = obj.optString("title", "");
                    String body = obj.optString("content", "");
                    if (!title.isEmpty() || !body.isEmpty()) {
                        dbHelper.insertNote(title, body);
                        count++;
                    }
                }
                loadNotes(null);
                Toast.makeText(this, getString(R.string.import_success) + " (" + count + ")", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertNote("", content);
                loadNotes(null);
                Toast.makeText(this, R.string.import_success, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.import_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                openEditor(-1);
                editorContent.setText(sharedText);
                editorTitle.setText("");
                autoSaveCurrentNote();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (containerList != null && containerList.getVisibility() == View.VISIBLE) {
            loadNotes(searchInput != null ? searchInput.getText().toString() : null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveCurrentNote();
    }

    @Override
    protected void onStop() {
        super.onStop();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveCurrentNote();
    }

    @Override
    public void onBackPressed() {
        if (adapter != null && adapter.isSelectionMode()) {
            exitSelectionMode();
            return;
        }
        if (containerEditor.getVisibility() == View.VISIBLE) {
            autoSaveCurrentNote();
            closeEditor();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
