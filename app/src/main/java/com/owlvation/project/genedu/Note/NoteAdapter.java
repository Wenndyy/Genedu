package com.owlvation.project.genedu.Note;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {
    private Context context;
    private List<NoteModel> noteList;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;

    public NoteAdapter(Context context, List<NoteModel> noteList, FirebaseFirestore firebaseFirestore, FirebaseUser firebaseUser) {
        this.context = context;
        this.noteList = noteList;
        this.firebaseFirestore = firebaseFirestore;
        this.firebaseUser = firebaseUser;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notes_layout, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteModel note = noteList.get(position);
        holder.title.setText(note.getTitle());
        holder.content.setText(note.getContent());

        if (!TextUtils.isEmpty(note.getImageUrl())) {
            holder.imageView.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(note.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.imageView);
        } else {
            holder.imageView.setVisibility(View.GONE);
        }

        if (note.getTimestamp() == null) {
            note.setTimestamp(new Date());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(note.getTimestamp());
        holder.timestamp.setText(formattedDate);

        holder.menuPopUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.inflate(R.menu.popup_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menu_view_detail) {
                            showNoteDetail(note);
                            return true;
                        } else if (item.getItemId() == R.id.menu_update) {
                            startUpdateNoteActivity(note);
                            return true;
                        } else if (item.getItemId() == R.id.menu_delete) {
                            deleteNote(note);
                            return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, timestamp;
        ImageView menuPopUpButton, imageView;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.noteTitle);
            content = itemView.findViewById(R.id.noteDesc);
            timestamp = itemView.findViewById(R.id.noteDate);
            menuPopUpButton = itemView.findViewById(R.id.menuPopUpButton);
            imageView = itemView.findViewById(R.id.noteImage);
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
        contentTextView.setText(context.getString(R.string.content) + "\n" + note.getContent() + "\n\n" + context.getString(R.string.created_on) + formattedDate);

        if (!TextUtils.isEmpty(note.getImageUrl())) {
            Glide.with(context)
                    .load(note.getImageUrl())
                    .into(imageView);
        } else {
            imageView.setVisibility(View.GONE);
        }


        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            alertDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        });

        ImageView closeDialog = dialogView.findViewById(R.id.close_dialog);
        closeDialog.setOnClickListener(v -> {
            alertDialog.dismiss();
        });
        alertDialog.show();
    }

    public void filterList(List<NoteModel> filteredList) {
        noteList = filteredList;
        notifyDataSetChanged();
    }


    private void startUpdateNoteActivity(NoteModel note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.update_note);
        builder.setMessage(R.string.are_you_sure_you_want_to_update_this_note);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(context, CreateNoteActivity.class);
                intent.putExtra("noteId", note.getDocumentId());
                intent.putExtra("title", note.getTitle());
                intent.putExtra("content", note.getContent());
                intent.putExtra("imageUrl", note.getImageUrl());
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

    private void deleteNote(NoteModel note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_note);
        builder.setMessage(R.string.are_you_sure_you_want_to_delete_this_note);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                firebaseFirestore.collection("notes").document(firebaseUser.getUid()).collection("myNotes")
                        .document(note.getDocumentId())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onSuccess(Void aVoid) {
                                noteList.remove(note);
                                notifyDataSetChanged();
                                Toast.makeText(context, R.string.note_deleted_successfully, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, context.getString(R.string.failed_to_delete_note) + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();
    }

}
