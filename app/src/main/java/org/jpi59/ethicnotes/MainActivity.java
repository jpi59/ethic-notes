package org.jpi59.ethicnotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
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

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "ethic_notes_prefs";
    private static final String PREF_SHIELD_ENABLED = "privacy_shield_enabled";

    private static final int REQ_EXPORT_NOTE = 1001;
    private static final int REQ_EXPORT_ALL = 1002;
    private static final int REQ_IMPORT = 1003;

    private NotesDbHelper dbHelper;
    private NotesAdapter adapter;

    private View containerList;
    private View containerEditor;
    private ListView listView;
    private View emptyStateLayout;
    private EditText searchInput;

    private EditText editorTitle;
    private EditText editorContent;
    private TextView editorWordCharCount;

    private long currentNoteId = -1;
    private boolean isPrivacyShieldActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new NotesDbHelper(this);
        initViews();
        setupPrivacyShield();
        setupList();
        setupSearch();
        setupEditor();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void initViews() {
        containerList = findViewById(R.id.container_list);
        containerEditor = findViewById(R.id.container_editor);
        listView = findViewById(R.id.notes_list_view);
        emptyStateLayout = findViewById(R.id.layout_empty_state);
        searchInput = findViewById(R.id.search_input);

        editorTitle = findViewById(R.id.editor_title);
        editorContent = findViewById(R.id.editor_content);
        editorWordCharCount = findViewById(R.id.editor_char_word_count);

        ImageButton fabAdd = findViewById(R.id.fab_add_note);
        fabAdd.setOnClickListener(v -> openEditor(-1));

        ImageButton btnPrivacyShield = findViewById(R.id.btn_privacy_shield);
        btnPrivacyShield.setOnClickListener(v -> togglePrivacyShield());

        ImageButton btnMenu = findViewById(R.id.btn_menu_export_import);
        btnMenu.setOnClickListener(v -> showStorageMenu());
    }

    private void setupPrivacyShield() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isPrivacyShieldActive = prefs.getBoolean(PREF_SHIELD_ENABLED, false);
        applyPrivacyShield();
    }

    private void applyPrivacyShield() {
        if (isPrivacyShieldActive) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void togglePrivacyShield() {
        isPrivacyShieldActive = !isPrivacyShieldActive;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_SHIELD_ENABLED, isPrivacyShieldActive).apply();
        applyPrivacyShield();

        int msg = isPrivacyShieldActive ? R.string.privacy_shield_enabled : R.string.privacy_shield_disabled;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setupList() {
        adapter = new NotesAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Note note = adapter.getItem(position);
            openEditor(note.getId());
        });
        loadNotes(null);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadNotes(String query) {
        List<Note> notes = dbHelper.searchNotes(query);
        adapter.setNotes(notes);
        if (notes.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            TextView desc = findViewById(R.id.empty_desc);
            if (query != null && !query.trim().isEmpty()) {
                desc.setText(R.string.no_search_results);
            } else {
                desc.setText(R.string.no_notes_desc);
            }
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    private void setupEditor() {
        findViewById(R.id.btn_editor_back).setOnClickListener(v -> saveAndCloseEditor());
        findViewById(R.id.btn_editor_save).setOnClickListener(v -> saveAndCloseEditor());

        findViewById(R.id.btn_editor_delete).setOnClickListener(v -> {
            if (currentNoteId != -1) {
                confirmDeleteNote();
            } else {
                closeEditor();
            }
        });

        findViewById(R.id.btn_editor_share).setOnClickListener(v -> shareCurrentNote());
        findViewById(R.id.btn_editor_export).setOnClickListener(v -> exportSingleNoteSaf());

        TextWatcher counterWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWordCharCounter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        editorContent.addTextChangedListener(counterWatcher);
        editorTitle.addTextChangedListener(counterWatcher);
    }

    private void openEditor(long noteId) {
        currentNoteId = noteId;
        if (noteId != -1) {
            Note note = dbHelper.getNote(noteId);
            if (note != null) {
                editorTitle.setText(note.getRawTitle());
                editorContent.setText(note.getContent());
            }
        } else {
            editorTitle.setText("");
            editorContent.setText("");
        }
        updateWordCharCounter();
        containerList.setVisibility(View.GONE);
        containerEditor.setVisibility(View.VISIBLE);
        editorContent.requestFocus();
    }

    private void saveAndCloseEditor() {
        String title = editorTitle.getText().toString().trim();
        String content = editorContent.getText().toString();

        if (!title.isEmpty() || !content.trim().isEmpty()) {
            if (currentNoteId == -1) {
                dbHelper.insertNote(title, content);
            } else {
                dbHelper.updateNote(currentNoteId, title, content);
            }
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        }
        closeEditor();
    }

    private void closeEditor() {
        containerEditor.setVisibility(View.GONE);
        containerList.setVisibility(View.VISIBLE);
        currentNoteId = -1;
        loadNotes(searchInput.getText().toString());
    }

    private void confirmDeleteNote() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteNote(currentNoteId);
                    Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
                    closeEditor();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateWordCharCounter() {
        String content = editorContent.getText().toString();
        int chars = content.length();
        int words = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        editorWordCharCount.setText(getString(R.string.chars_words_count, chars, words));
    }

    private void shareCurrentNote() {
        String title = editorTitle.getText().toString().trim();
        String content = editorContent.getText().toString();
        String fullText = (title.isEmpty() ? "" : title + "\n\n") + content;

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, fullText);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.action_share)));
    }

    // STORAGE ACCESS FRAMEWORK (SAF): ZERO PERMISSIONS REQUIRED
    private void exportSingleNoteSaf() {
        String title = editorTitle.getText().toString().trim();
        String filename = (title.isEmpty() ? "note" : title.replaceAll("[^a-zA-Z0-9_-]", "_")) + ".txt";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(intent, REQ_EXPORT_NOTE);
    }

    private void showStorageMenu() {
        String[] options = new String[]{
                getString(R.string.action_import),
                getString(R.string.action_export_all)
        };
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        importNotesSaf();
                    } else if (which == 1) {
                        exportAllNotesSaf();
                    }
                })
                .show();
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
                // Parse JSON array
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
                // Plain text import
                dbHelper.insertNote("Imported Note", content);
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
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (containerEditor.getVisibility() == View.VISIBLE) {
            saveAndCloseEditor();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}
