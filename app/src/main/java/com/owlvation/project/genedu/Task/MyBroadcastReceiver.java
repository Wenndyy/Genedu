package com.owlvation.project.genedu.Task;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.owlvation.project.genedu.R;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String WAKE_LOCK_TAG = "myalarm:wakelock";

    @Override
    public void onReceive(Context context, Intent intent) {

        long alarmId = intent.getLongExtra("alarm_id", -1);
        String taskName = intent.getStringExtra("task_name");
        String id = intent.getStringExtra("id");
        String dueDate = intent.getStringExtra("due_date");
        String dueTime = intent.getStringExtra("due_time");
        int status = intent.getIntExtra("status",0);

        if (alarmId == -1) {
            Log.d("DEBUG", "Received alarm with invalid ID");
            return;
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                WAKE_LOCK_TAG
        );
        wakeLock.acquire(60 * 1000L);

        try {

            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(2000);
            }


            Intent fullScreenIntent = new Intent(context, TaskActivity.class);

            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            fullScreenIntent.putExtra("id", id);
            fullScreenIntent.putExtra("task_name", taskName);
            fullScreenIntent.putExtra("due_date", dueDate);
            fullScreenIntent.putExtra("due_time", dueTime);
            fullScreenIntent.putExtra("status", status);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    (int) alarmId,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );


            Uri alarmSound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.task);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }


            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "Notify")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(context.getString(R.string.task_reminder) + taskName)
                    .setContentText(context.getString(R.string.due_on) + dueDate +" "+ context.getString(R.string.at)+" " + dueTime)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSound(alarmSound, AudioManager.STREAM_ALARM)
                    .setFullScreenIntent(pendingIntent, true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.d("DEBUG", "POST_NOTIFICATIONS permission not granted");
                    return;
                }
            }


            notificationManagerCompat.notify((int) alarmId, builder.build());

        } finally {
            wakeLock.release();
        }
    }

}