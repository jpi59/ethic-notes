package org.jpi59.ethicnotes;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Note {
    private final long id;
    private final String title;
    private final String content;
    private final long createdAt;
    private final long modifiedAt;
    private final boolean isPinned;

    public Note(long id, String title, String content, long createdAt, long modifiedAt, boolean isPinned) {
        this.id = id;
        this.title = title != null ? title.trim() : "";
        this.content = content != null ? content : "";
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.isPinned = isPinned;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title.isEmpty() ? (content.isEmpty() ? "Untitled" : getSnippet()) : title;
    }

    public String getRawTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public String getSnippet() {
        String trimmed = content.trim();
        if (trimmed.length() <= 120) {
            return trimmed;
        }
        return trimmed.substring(0, 120) + "...";
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault());
        return sdf.format(new Date(modifiedAt));
    }

    public int getWordCount() {
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\s+").length;
    }

    public int getCharacterCount() {
        return content.length();
    }
}
