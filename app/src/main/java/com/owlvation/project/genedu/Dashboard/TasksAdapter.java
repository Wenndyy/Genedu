package com.owlvation.project.genedu.Dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.TaskModel;

import java.util.List;

public class TasksAdapter extends ArrayAdapter<TaskModel> {

    private final Context context;
    private final List<TaskModel> tasks;

    public TasksAdapter(Context context, List<TaskModel> tasks) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks = tasks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dialog_list_item, parent, false);
        }

        TaskModel task = tasks.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tvTitle);
        TextView tvTime = convertView.findViewById(R.id.tvTime);

        tvTitle.setText(task.getTask());

        String dueTime = task.getDueTime();
        tvTime.setText(context.getString(R.string.due_time) + ": " + dueTime);

        return convertView;
    }
}