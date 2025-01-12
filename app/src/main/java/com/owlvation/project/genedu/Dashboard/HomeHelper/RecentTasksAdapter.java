package com.owlvation.project.genedu.Dashboard.HomeHelper;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.TaskModel;

import java.util.List;

public class RecentTasksAdapter extends RecyclerView.Adapter<RecentTasksAdapter.TaskViewHolder> {

    private List<TaskModel> taskList;
    private Context context;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    public RecentTasksAdapter(Context context, List<TaskModel> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = taskList.get(position);

        holder.taskTitleView.setText(task.getTask());
        holder.dueDateTextView.setText(context.getString(R.string.due_on) + task.getDueDate());
        holder.dueTimeTextView.setText(context.getString(R.string.at) + task.getDueTime());

        holder.itemView.setOnClickListener(v -> {
            showTaskDetail(task);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void filterTasks(List<TaskModel> filteredList) {
        this.taskList = filteredList;
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitleView;
        TextView dueDateTextView;
        TextView dueTimeTextView;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitleView = itemView.findViewById(R.id.taskTitleView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            dueTimeTextView = itemView.findViewById(R.id.dueTimeTextView);
        }
    }

    private void showTaskDetail(TaskModel task){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_note_detail, null);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        TextView contentTextView = dialogView.findViewById(R.id.contentTextView);


        titleTextView.setText(context.getString(R.string.title) + task.getTask());
        contentTextView.setText(context.getString(R.string.task_deadline)+ " " +  task.getDueDate() +context.getString(R.string.at_) + task.getDueTime() );
        builder.setView(dialogView);


        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            alertDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        });
        ImageView closeDialog = dialogView.findViewById(R.id.close_dialog);
        closeDialog.setOnClickListener(v -> {
            alertDialog.dismiss();
        });
        alertDialog.show();

    }
}