package com.owlvation.project.genedu.Task;

import android.content.DialogInterface;

import com.owlvation.project.genedu.Task.Model.TaskModel;

public interface OnDialogCloseListner {

    void onDialogClose(DialogInterface dialogInterface);
    void onTaskClick(TaskModel taskModel);
}
