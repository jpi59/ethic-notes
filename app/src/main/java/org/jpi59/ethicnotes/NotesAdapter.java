/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethicnotes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<Note> notes;
    private boolean darkMode;

    private boolean selectionMode = false;
    private final Set<Long> selectedIds = new HashSet<>();

    public NotesAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.notes = new ArrayList<>();
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        notifyDataSetChanged();
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes != null ? notes : new ArrayList<Note>();
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean mode) {
        if (this.selectionMode != mode) {
            this.selectionMode = mode;
            if (!mode) {
                selectedIds.clear();
            }
            notifyDataSetChanged();
        }
    }

    public void toggleSelection(long noteId) {
        if (selectedIds.contains(noteId)) {
            selectedIds.remove(noteId);
        } else {
            selectedIds.add(noteId);
        }
        if (selectedIds.isEmpty()) {
            selectionMode = false;
        }
        notifyDataSetChanged();
    }

    public void selectNote(long noteId) {
        selectedIds.add(noteId);
        selectionMode = true;
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedIds.clear();
        for (Note note : notes) {
            selectedIds.add(note.getId());
        }
        selectionMode = true;
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
    }

    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    @Override
    public int getCount() {
        return notes.size();
    }

    @Override
    public Note getItem(int position) {
        return notes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return notes.get(position).getId();
    }

    private static class ViewHolder {
        View root;
        TextView title;
        ImageView selectCheck;
        TextView snippet;
        TextView date;
    }

    private int dp(int n) {
        return Math.round(n * context.getResources().getDisplayMetrics().density);
    }

    private GradientDrawable createCardBackground(boolean selected) {
        GradientDrawable gd = new GradientDrawable();
        int action = darkMode ? context.getColor(R.color.accent_dark) : context.getColor(R.color.accent);

        if (selected) {
            int fill = darkMode ? Color.rgb(26, 51, 44) : Color.rgb(230, 242, 239);
            gd.setColor(fill);
            gd.setCornerRadius(dp(14));
            gd.setStroke(dp(2), action);
        } else {
            int fill = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
            int stroke = darkMode ? Color.rgb(44, 51, 46) : Color.rgb(228, 226, 220);
            gd.setColor(fill);
            gd.setCornerRadius(dp(14));
            gd.setStroke(dp(1), stroke);
        }
        return gd;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
            holder = new ViewHolder();
            holder.root = convertView.findViewById(R.id.note_card_root);
            holder.title = convertView.findViewById(R.id.note_item_title);
            holder.selectCheck = convertView.findViewById(R.id.note_item_select_check);
            holder.snippet = convertView.findViewById(R.id.note_item_snippet);
            holder.date = convertView.findViewById(R.id.note_item_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = getItem(position);
        boolean isSelected = selectedIds.contains(note.getId());

        holder.root.setBackground(createCardBackground(isSelected));

        int ink = darkMode ? Color.rgb(226, 232, 226) : Color.rgb(22, 23, 26);
        int muted = darkMode ? Color.rgb(177, 184, 177) : Color.rgb(95, 98, 95);
        int action = darkMode ? context.getColor(R.color.accent_dark) : context.getColor(R.color.accent);

        holder.title.setTextColor(ink);
        holder.snippet.setTextColor(muted);
        holder.date.setTextColor(muted);

        holder.title.setText(note.getDisplayTitle(context.getString(R.string.untitled_note)));

        if (selectionMode) {
            holder.selectCheck.setVisibility(View.VISIBLE);
            holder.selectCheck.setColorFilter(isSelected ? action : muted);
            holder.selectCheck.setAlpha(isSelected ? 1.0f : 0.35f);
        } else {
            holder.selectCheck.setVisibility(View.GONE);
        }

        String snippet = note.getSnippet();
        if (snippet.isEmpty()) {
            holder.snippet.setVisibility(View.GONE);
        } else {
            holder.snippet.setVisibility(View.VISIBLE);
            holder.snippet.setText(snippet);
        }
        holder.date.setText(note.getFormattedDate());

        return convertView;
    }
}
