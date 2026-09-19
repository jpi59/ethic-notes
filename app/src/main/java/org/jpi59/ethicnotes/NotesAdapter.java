/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethicnotes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private List<Note> notes;
    private boolean darkMode;

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
        TextView snippet;
        TextView date;
    }

    private int dp(int n) {
        return Math.round(n * context.getResources().getDisplayMetrics().density);
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable gd = new GradientDrawable();
        int fill = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        int stroke = darkMode ? Color.rgb(44, 51, 46) : Color.rgb(228, 226, 220);
        gd.setColor(fill);
        gd.setCornerRadius(dp(14));
        gd.setStroke(dp(1), stroke);
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
            holder.snippet = convertView.findViewById(R.id.note_item_snippet);
            holder.date = convertView.findViewById(R.id.note_item_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = getItem(position);
        holder.root.setBackground(createCardBackground());

        int ink = darkMode ? Color.rgb(226, 232, 226) : Color.rgb(22, 23, 26);
        int muted = darkMode ? Color.rgb(177, 184, 177) : Color.rgb(95, 98, 95);

        holder.title.setTextColor(ink);
        holder.snippet.setTextColor(muted);
        holder.date.setTextColor(muted);

        holder.title.setText(note.getDisplayTitle(context.getString(R.string.untitled_note)));
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
