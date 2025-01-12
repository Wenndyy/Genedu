package com.owlvation.project.genedu.Task;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Adapter.TaskAdapter;
import com.owlvation.project.genedu.Task.Model.AlarmDatabaseHelper;
import com.owlvation.project.genedu.Task.Model.TaskModel;

import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;


public class TouchHelper extends ItemTouchHelper.SimpleCallback {
    private TaskAdapter adapter;
    private List<TaskModel> taskList;

    public TouchHelper(TaskAdapter adapter, List<TaskModel> taskList) {
        super(0 , ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.adapter = adapter;
        this.taskList = taskList;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        final int position = viewHolder.getAdapterPosition();
         AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(adapter.getContext());
        if (direction == ItemTouchHelper.RIGHT){
            AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext());
            builder.setMessage(R.string.are_you_sure)
                    .setTitle(R.string.delete_task)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            TaskModel tk = taskList.get(position);
                            long alarmId = tk.getAlarmId();
                            String id = tk.TaskId;
                            String task_name = tk.getTask();
                            String idAlarm = String.valueOf(tk.getAlarmId());
                            String due_date = tk.getDueDate();
                            String due_time = tk.getDueTime();

                            Log.d("TouchHelper", "Deleting alarm with ID: " + alarmId);
                            Cursor cursor = dbHelper.getAlarmById(alarmId);
                            if (cursor != null && cursor.moveToFirst()) {
                                Log.d("TouchHelper", "Alarm found in database. Deleting...");
                                dbHelper.deleteAlarm(alarmId);
                                cancelPreviousAlarm(adapter.getContext(),alarmId,id,task_name, due_date,due_time);
                                adapter.deleteTask(position);
                                Toast.makeText(adapter.getContext(), R.string.delete_task, Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("TouchHelper", "Alarm ID " + alarmId + " not found in database.");
                                adapter.notifyItemChanged(position);
                            }
                        }
                    }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    adapter.notifyItemChanged(position);
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }else{
            adapter.editTask(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addSwipeRightActionIcon(R.drawable.ic_delete)
                .addSwipeRightBackgroundColor(Color.RED)
                .addSwipeLeftActionIcon(R.drawable.ic_editt)
                .addSwipeLeftBackgroundColor(ContextCompat.getColor(adapter.getContext() , R.color.colorAccent))
                .create()
                .decorate();
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void cancelPreviousAlarm(Context context,long alarmId, String id, String task, String dueDate, String dueTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context, MyBroadcastReceiver.class);
        i.putExtra("alarm_id", alarmId);
        i.setAction("com.example.unique_action." + alarmId);

        i.putExtra("id", id);
        i.putExtra("alarm_id", alarmId);
        i.putExtra("task_name", task);
        i.putExtra("due_date", dueDate);
        i.putExtra("due_time", dueTime);

        Log.d("cancelAlarmDelete", "idTask: " + id + " task name: "+ task+ " alarm id: "+alarmId+" due date: "+dueDate+" due time: "+dueTime);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) alarmId,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT  | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

}
