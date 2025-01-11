package com.owlvation.project.genedu.Dashboard.HomeHelper;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.owlvation.project.genedu.Note.NoteModel;
import com.owlvation.project.genedu.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecentNotesAdapter extends RecyclerView.Adapter<RecentNotesAdapter.ViewHolder> {
    private Context context;
    private List<NoteModel> noteList;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(NoteModel note);
    }

    public RecentNotesAdapter(Context context, List<NoteModel> noteList, OnNoteClickListener listener) {
        this.context = context;
        this.noteList = noteList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteModel note = noteList.get(position);

        holder.tvNoteTitle.setText(note.getTitle());
        holder.tvNoteContent.setText(note.getContent());
        if (!TextUtils.isEmpty(note.getImageUrl())) {
            holder.noteImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(note.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.noteImage);
        } else {
            holder.noteImage.setVisibility(View.GONE);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        holder.tvNoteDate.setText(sdf.format(note.getTimestamp()));

        holder.itemView.setOnClickListener(v -> showNoteDetail(note));
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoteTitle, tvNoteContent, tvNoteDate;
        ImageView noteImage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvNoteContent = itemView.findViewById(R.id.tvNoteContent);
            tvNoteDate = itemView.findViewById(R.id.tvNoteDate);
            noteImage = itemView.findViewById(R.id.noteImage);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showNoteDetail(NoteModel note) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(note.getTimestamp());

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_note_detail, null);
        builder.setView(dialogView);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        TextView contentTextView = dialogView.findViewById(R.id.contentTextView);
        ImageView imageView = dialogView.findViewById(R.id.imageView);

        titleTextView.setText(context.getString(R.string.title) + note.getTitle());
        contentTextView.setText(context.getString(R.string.content) + "\n" + note.getContent() + "\n\n" +
                context.getString(R.string.created_on) + formattedDate);

        if (!TextUtils.isEmpty(note.getImageUrl())) {
            imageView.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(note.getImageUrl())
                    .into(imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            alertDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        });
        ImageView closeDialog = dialogView.findViewById(R.id.close_dialog);
        closeDialog.setOnClickListener(v -> {
            alertDialog.dismiss();
        });
        alertDialog.show();
    }
}