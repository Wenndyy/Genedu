package com.owlvation.project.genedu.Dashboard;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.owlvation.project.genedu.Note.NoteModel;
import com.owlvation.project.genedu.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends ArrayAdapter<NoteModel> {

    private final Context context;
    private final List<NoteModel> notes;

    public NotesAdapter(Context context, List<NoteModel> notes) {
        super(context, 0, notes);
        this.context = context;
        this.notes = notes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dialog_list_item, parent, false);
        }

        NoteModel note = notes.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvTime = convertView.findViewById(R.id.tvTime);

        tvTitle.setText(note.getTitle());

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(note.getTimestamp());
        tvTime.setText(context.getString(R.string.created_at) + ": " + timeStr);

        return convertView;
    }
}

