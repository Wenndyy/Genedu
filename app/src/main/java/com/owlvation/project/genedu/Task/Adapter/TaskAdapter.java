package com.owlvation.project.genedu.Task.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.AddNewTask;
import com.owlvation.project.genedu.Task.Model.TaskModel;
import com.owlvation.project.genedu.Task.OnDialogCloseListner;
import com.owlvation.project.genedu.Task.TaskActivity;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.MyViewHolder> {

    private List<TaskModel> todoList;
    private TaskActivity activity;
    private FirebaseFirestore firestore;
    private String userId;
    private OnDialogCloseListner taskClickListener;
    public TaskAdapter(TaskActivity taskActivity, List<TaskModel> todoList, OnDialogCloseListner taskClickListener) {
        this.todoList = todoList;
        activity = taskActivity;
        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.taskClickListener = taskClickListener;

    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.task_layout, parent, false);
        return new MyViewHolder(view);
    }

    public void filterTask(List<TaskModel> filteredList) {
        todoList = filteredList;
        notifyDataSetChanged();
    }
    public void deleteTask(int position) {
        TaskModel taskModel = todoList.get(position);
        firestore.collection("task").document(userId)
                .collection("myTask").document(taskModel.TaskId)
                .delete();
        todoList.remove(position);

        notifyItemRemoved(position);
    }

    public Context getContext() {
        return activity;
    }

    public void editTask(int position) {
        TaskModel taskModel = todoList.get(position);

        Bundle bundle = new Bundle();
        bundle.putString("task", taskModel.getTask());
        bundle.putString("dueDate", taskModel.getDueDate());
        bundle.putString("dueTime", taskModel.getDueTime());
        bundle.putString("dueDateReminder", taskModel.getDueDateReminder());
        bundle.putString("dueTimeReminder", taskModel.getDueTimeReminder());
        bundle.putString("id", taskModel.TaskId);
        bundle.putLong("alarmId", taskModel.getAlarmId());

        AddNewTask addNewTask = new AddNewTask();
        addNewTask.setArguments(bundle);
        addNewTask.show(activity.getSupportFragmentManager(), addNewTask.getTag());
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        TaskModel taskModel = todoList.get(position);
        holder.mCheckBox.setText(taskModel.getTask());

        holder.mDueDateTv.setText(activity.getString(R.string.due_on) + taskModel.getDueDate());

        holder.mDueTimeTv.setText(activity.getString(R.string.at) + taskModel.getDueTime());

        holder.mCheckBox.setChecked(toBoolean(taskModel.getStatus()));

        holder.itemView.setOnClickListener(v -> {
            if (taskClickListener != null) {
                taskClickListener.onTaskClick(taskModel);
            }
        });

        holder.mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    firestore.collection("task").document(userId)
                            .collection("myTask").document(taskModel.TaskId)
                            .update("status", 1);
                } else {
                    firestore.collection("task").document(userId)
                            .collection("myTask").document(taskModel.TaskId)
                            .update("status", 0);
                }
            }
        });



    }

    private boolean toBoolean(int status) {
        return status != 0;
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView mDueDateTv;
        TextView mDueTimeTv;
        CheckBox mCheckBox;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            mDueTimeTv = itemView.findViewById(R.id.tv_due_time);
            mDueDateTv = itemView.findViewById(R.id.tv_due_date);
            mCheckBox = itemView.findViewById(R.id.mcheckbox);

        }
    }
}
