package com.owlvation.project.genedu.Task;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
                            dbHelper.deleteAlarm(alarmId);
                            cancelAlarm(adapter.getContext(), alarmId);
                            adapter.deleteTask(position);
                            Toast.makeText(adapter.getContext(),
                                   R.string.delete_task,
                                    Toast.LENGTH_SHORT).show();
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

    private void cancelAlarm(Context context, long alarmId) {
        if (alarmId != -1) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(context, MyBroadcastReceiver.class);
            intent.putExtra("alarm_id", alarmId);


            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (int) alarmId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }

            Log.d("Alarm", "Alarm with ID " + alarmId + " has been cancelled.");
        }
    }





}
