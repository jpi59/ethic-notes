package org.jpi59.ethicnotes;

import android.content.Context;
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

    public NotesAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.notes = new ArrayList<>();
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
        TextView title;
        TextView snippet;
        TextView date;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_note, parent, false);
            holder = new ViewHolder();
            holder.title = convertView.findViewById(R.id.note_item_title);
            holder.snippet = convertView.findViewById(R.id.note_item_snippet);
            holder.date = convertView.findViewById(R.id.note_item_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Note note = getItem(position);
        holder.title.setText(note.getTitle());
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
