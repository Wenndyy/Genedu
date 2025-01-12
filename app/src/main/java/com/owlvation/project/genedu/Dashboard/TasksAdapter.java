package com.owlvation.project.genedu.Dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.TaskModel;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final Context context;
    private final List<TaskModel> tasks;
    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    public TasksAdapter(Context context, List<TaskModel> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_list_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = tasks.get(position);
        holder.tvTitle.setText(task.getTask());
        holder.tvTime.setText(context.getString(R.string.due_time) + ": " + task.getDueTime());

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}