package com.owlvation.project.genedu.Dashboard;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.Note.NoteModel;
import com.owlvation.project.genedu.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final Context context;
    private final List<NoteModel> notes;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(NoteModel note);
    }

    public NotesAdapter(Context context, List<NoteModel> notes, OnNoteClickListener listener) {
        this.context = context;
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_list_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteModel note = notes.get(position);
        holder.tvTitle.setText(note.getTitle());

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeStr = timeFormat.format(note.getTimestamp());
        holder.tvTime.setText(context.getString(R.string.created_at) + ": " + timeStr);

        holder.itemView.setOnClickListener(v -> listener.onNoteClick(note));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}