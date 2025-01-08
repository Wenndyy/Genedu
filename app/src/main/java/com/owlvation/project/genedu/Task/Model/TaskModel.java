package com.owlvation.project.genedu.Task.Model;

public class TaskModel extends TaskId {

    private String task, dueDate, dueTime,dueDateReminder, dueTimeReminder;
    private int status;
    private long alarmId;

    public String getTask() {
        return task;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getDueTime() {
        return dueTime;
    }

    public int getStatus() {
        return status;
    }

    public String getDueDateReminder() {
        return dueDateReminder;
    }

    public String getDueTimeReminder() {
        return dueTimeReminder;
    }

    public long getAlarmId() {
        return alarmId;
    }
}
