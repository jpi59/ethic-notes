package org.jpi59.ethicnotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NotesDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ethic_notes.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_MODIFIED_AT = "modified_at";
    public static final String COL_IS_PINNED = "is_pinned";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NOTES + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_TITLE + " TEXT NOT NULL, " +
                    COL_CONTENT + " TEXT NOT NULL, " +
                    COL_CREATED_AT + " INTEGER NOT NULL, " +
                    COL_MODIFIED_AT + " INTEGER NOT NULL, " +
                    COL_IS_PINNED + " INTEGER NOT NULL DEFAULT 0);";

    public NotesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1: initial schema
    }

    public long insertNote(String title, String content) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title != null ? title.trim() : "");
        values.put(COL_CONTENT, content != null ? content : "");
        long now = System.currentTimeMillis();
        values.put(COL_CREATED_AT, now);
        values.put(COL_MODIFIED_AT, now);
        values.put(COL_IS_PINNED, 0);
        return db.insert(TABLE_NOTES, null, values);
    }

    public boolean updateNote(long id, String title, String content) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title != null ? title.trim() : "");
        values.put(COL_CONTENT, content != null ? content : "");
        values.put(COL_MODIFIED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_NOTES, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public boolean deleteNote(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public int deleteNotes(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        SQLiteDatabase db = getWritableDatabase();
        int count = 0;
        db.beginTransaction();
        try {
            for (Long id : ids) {
                count += db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return count;
    }

    public Note getNote(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NOTES,
                null,
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return readNoteFromCursor(cursor);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public List<Note> getAllNotes() {
        return searchNotes(null);
    }

    public List<Note> searchNotes(String query) {
        List<Note> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor;
        if (query == null || query.trim().isEmpty()) {
            cursor = db.query(
                    TABLE_NOTES,
                    null,
                    null,
                    null,
                    null,
                    null,
                    COL_IS_PINNED + " DESC, " + COL_MODIFIED_AT + " DESC"
            );
        } else {
            String pattern = "%" + query.trim() + "%";
            cursor = db.query(
                    TABLE_NOTES,
                    null,
                    COL_TITLE + " LIKE ? OR " + COL_CONTENT + " LIKE ?",
                    new String[]{pattern, pattern},
                    null,
                    null,
                    COL_IS_PINNED + " DESC, " + COL_MODIFIED_AT + " DESC"
            );
        }

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    list.add(readNoteFromCursor(cursor));
                }
            } finally {
                cursor.close();
            }
        }
        return list;
    }

    private Note readNoteFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT));
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT));
        long modifiedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MODIFIED_AT));
        boolean isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_PINNED)) == 1;
        return new Note(id, title, content, createdAt, modifiedAt, isPinned);
    }

    public String exportToJson() {
        List<Note> notes = getAllNotes();
        JSONArray array = new JSONArray();
        for (Note note : notes) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", note.getId());
                obj.put("title", note.getRawTitle());
                obj.put("content", note.getContent());
                obj.put("created_at", note.getCreatedAt());
                obj.put("modified_at", note.getModifiedAt());
                array.put(obj);
            } catch (Exception ignored) {
            }
        }
        return array.toString();
    }
}
