/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
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

    public String getRawTitle() {
        return title;
    }

    public String getDisplayTitle(String fallback) {
        if (!title.isEmpty()) {
            return title;
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return fallback != null ? fallback : "Nota sin título";
        }
        String[] lines = trimmed.split("\\r?\\n");
        for (String line : lines) {
            String l = line.trim();
            if (!l.isEmpty()) {
                if (l.length() <= 45) {
                    return l;
                }
                return l.substring(0, 45) + "…";
            }
        }
        return fallback != null ? fallback : "Nota sin título";
    }

    public String getTitle() {
        return getDisplayTitle("Nota sin título");
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
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!title.isEmpty()) {
            if (trimmed.length() <= 120) {
                return trimmed;
            }
            return trimmed.substring(0, 120) + "…";
        }

        // When title was derived from first non-empty line, snippet displays subsequent lines
        String[] lines = trimmed.split("\\r?\\n");
        StringBuilder remaining = new StringBuilder();
        boolean skippedFirst = false;
        for (String line : lines) {
            String l = line.trim();
            if (!skippedFirst && !l.isEmpty()) {
                skippedFirst = true;
                continue;
            }
            if (skippedFirst && !l.isEmpty()) {
                if (remaining.length() > 0) remaining.append(" ");
                remaining.append(l);
            }
        }
        String rem = remaining.toString().trim();
        if (rem.isEmpty()) {
            return "";
        }
        if (rem.length() <= 120) {
            return rem;
        }
        return rem.substring(0, 120) + "…";
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("d MMM · HH:mm", Locale.getDefault());
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
