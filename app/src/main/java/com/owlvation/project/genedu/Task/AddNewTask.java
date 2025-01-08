package com.owlvation.project.genedu.Task;

import static com.owlvation.project.genedu.R.string.notif_reminder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.AlarmDatabaseHelper;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class AddNewTask extends BottomSheetDialogFragment {
    public static final String TAG = "AddNewTask";
    private TextView setDueDate;
    private TextView setDueTime;
    private TextView setDueTimeReminder;
    private TextView setDueReminder;
    private EditText mTaskEdit;
    private LinearLayout mSaveBtn, layoutReminderDueDate, layoutReminderDueTime;
    private FirebaseFirestore firestore;
    private Context context;
    private String userId;
    private String dueDate = "";
    private String dueTime = "";
    private String dueDateReminder = "";
    private String dueTimeReminder = "";
    private String id = "";
    private String dueDateUpdate = "";
    private String dueTimeUpdate = "";
    private String dueDateReminderUpdate = "";
    private String dueTimeReminderUpdate = "";
    private Switch switchToggle;

    private String task = "";
    private long alarmIdUpdate ,alarmId;
    private AlarmDatabaseHelper dbHelper;
    private int jam, menit, hari, bulan, tahun;

    private String date;

    public static AddNewTask newInstance() {
        return new AddNewTask();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_new_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDueTime = view.findViewById(R.id.set_time_tv);
        setDueDate = view.findViewById(R.id.set_due_tv);
        setDueReminder = view.findViewById(R.id.set_reminder_due_tv);
        setDueTimeReminder = view.findViewById(R.id.set_reminder_due_time_tv);

        layoutReminderDueDate = view.findViewById(R.id.layout_set_reminder_due_tv);
        layoutReminderDueTime = view.findViewById(R.id.layout_set_reminder_due_time_tv);
        mTaskEdit = view.findViewById(R.id.task_edittext);
        mSaveBtn = view.findViewById(R.id.save_btn);
        switchToggle = view.findViewById(R.id.material_switch);
        dbHelper = new AlarmDatabaseHelper(context);
        firestore = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        boolean isUpdate = false;

        final Bundle bundle = getArguments();
        if (bundle != null) {
            isUpdate = true;
            task = bundle.getString("task");
            id = bundle.getString("id");
            dueDateUpdate = bundle.getString("dueDate");
            dueTimeUpdate = bundle.getString("dueTime");
            dueDateReminderUpdate = bundle.getString("dueDateReminder");
            dueTimeReminderUpdate = bundle.getString("dueTimeReminder");
            alarmIdUpdate = bundle.getLong("alarmId");

            dueDateReminder = dueDateReminderUpdate;
            dueTimeReminder = dueTimeReminderUpdate;


            Log.d(TAG, "dueDateReminder: "+dueDateReminderUpdate);
            Log.d(TAG, "dueTimeReminder: "+dueTimeReminderUpdate);
            mTaskEdit.setText(task);
            setDueDate.setText(dueDateUpdate);
            setDueTime.setText(dueTimeUpdate);
            if (dueDateReminderUpdate != null && dueTimeReminderUpdate != null
                    && !dueDateReminderUpdate.isEmpty() && !dueTimeReminderUpdate.isEmpty()
                    && switchToggle != null) {
                switchToggle.setChecked(true);
                layoutReminderDueDate.setVisibility(View.VISIBLE);
                layoutReminderDueTime.setVisibility(View.VISIBLE);
                setDueReminder.setText(dueDateReminderUpdate);
                setDueTimeReminder.setText(dueTimeReminderUpdate);

                String[] dateParts = dueDateReminderUpdate.split("/");
                hari = Integer.parseInt(dateParts[0]);
                bulan = Integer.parseInt(dateParts[1]);
                tahun = Integer.parseInt(dateParts[2]);

                String[] timeParts = dueTimeReminderUpdate.split(":");
                jam = Integer.parseInt(timeParts[0]);
                menit = Integer.parseInt(timeParts[1]);
            }

            mSaveBtn.setEnabled(true);
        }

        switchToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutReminderDueDate.setVisibility(View.VISIBLE);
                layoutReminderDueTime.setVisibility(View.VISIBLE);
            } else {
                layoutReminderDueDate.setVisibility(View.GONE);
                layoutReminderDueTime.setVisibility(View.GONE);
            }
        });




        mTaskEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSaveBtn.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        setDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int YEAR = calendar.get(Calendar.YEAR);
                int MONTH = calendar.get(Calendar.MONTH);
                int DAY = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month = month + 1;
                        dueDate = dayOfMonth + "/" + month + "/" + year;
                        setDueDate.setText(dueDate);
                        mSaveBtn.setEnabled(true);
                    }
                }, YEAR, MONTH, DAY);
                datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

                datePickerDialog.show();
            }
        });

        setDueReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int YEAR = calendar.get(Calendar.YEAR);
                int MONTH = calendar.get(Calendar.MONTH);
                int DAY = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month = month + 1;
                        dueDateReminder = dayOfMonth + "/" + month + "/" + year;
                        tahun = year;
                        bulan = month;
                        hari = dayOfMonth;
                        if (isDateBeforeDueDate(year, month, dayOfMonth)) {
                            Toast.makeText(context, notif_reminder, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setDueReminder.setText(dueDateReminder);
                        mSaveBtn.setEnabled(true);
                    }
                }, YEAR, MONTH, DAY);
                String[] dueDateParts = dueDate.split("/");
                int dueDay = Integer.parseInt(dueDateParts[0]);
                int dueMonth = Integer.parseInt(dueDateParts[1]);
                int dueYear = Integer.parseInt(dueDateParts[2]);

                Calendar dueCalendar = Calendar.getInstance();
                dueCalendar.set(dueYear, dueMonth - 1, dueDay);
                datePickerDialog.getDatePicker().setMinDate(dueCalendar.getTimeInMillis());

                datePickerDialog.show();
            }
        });

        setDueTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        dueTime = hourOfDay + ":" + minute;
                        setDueTime.setText(dueTime);
                        mSaveBtn.setEnabled(true);
                    }
                }, hour, minute, true);

                timePickerDialog.show();
            }
        });

        setDueTimeReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        dueTimeReminder = hourOfDay + ":" + minute;
                        jam = hourOfDay;
                        menit = minute;
                        setDueTimeReminder.setText(dueTimeReminder);
                        mSaveBtn.setEnabled(true);
                    }
                }, hour, minute, true);

                timePickerDialog.show();
            }
        });



        boolean finalIsUpdate = isUpdate;
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String task = mTaskEdit.getText().toString();

                if (finalIsUpdate) {

                    Map<String, Object> taskMap = new HashMap<>();
                    boolean updateTime = false, updateDate = false;

                    if (!task.equals(bundle.getString("task"))) {
                        taskMap.put("task", task);
                    }

                    if (!dueDate.equals(dueDateUpdate) && !dueDate.isEmpty()) {
                        taskMap.put("dueDate", dueDate);
                    }

                    if (!dueTime.equals(dueTimeUpdate) && !dueTime.isEmpty()) {
                        taskMap.put("dueTime", dueTime);
                    }

                    String currentDateReminder = !dueDateReminder.isEmpty() ? dueDateReminder : dueDateReminderUpdate;
                    String currentTimeReminder = !dueTimeReminder.isEmpty() ? dueTimeReminder : dueTimeReminderUpdate;

                    // Update jika ada perubahan
                    if (!dueDateReminder.isEmpty() && !dueDateReminder.equals(dueDateReminderUpdate)) {
                        taskMap.put("dueDateReminder", dueDateReminder);
                        updateDate = true;
                    }

                    if (!dueTimeReminder.isEmpty() && !dueTimeReminder.equals(dueTimeReminderUpdate)) {
                        taskMap.put("dueTimeReminder", dueTimeReminder);
                        updateTime = true;
                    }

                    // Jika ada reminder yang sudah diset sebelumnya (baik tanggal atau waktu)
                    if(currentTimeReminder != null && currentDateReminder != null &&
                            !currentTimeReminder.isEmpty() && !currentDateReminder.isEmpty()) {

                        // Parse waktu current (baru atau lama)
                        if (updateTime) {
                            String[] timeParts = currentTimeReminder.split(":");
                            jam = Integer.parseInt(timeParts[0]);
                            menit = Integer.parseInt(timeParts[1]);
                        }

                        // Parse tanggal current (baru atau lama)
                        if (updateDate) {
                            String[] dateParts = currentDateReminder.split("/");
                            hari = Integer.parseInt(dateParts[0]);
                            bulan = Integer.parseInt(dateParts[1]);
                            tahun = Integer.parseInt(dateParts[2]);
                        }

                        // Ambil nilai yang tidak diupdate dari database
                        Cursor cursor = dbHelper.getAlarmById(alarmIdUpdate);
                        if (cursor != null && cursor.moveToFirst()) {
                            int hourIndex = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_HOUR);
                            int minuteIndex = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_MINUTE);
                            int dayIndex = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_DAY);
                            int monthIndex = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_MONTH);
                            int yearIndex = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_YEAR);

                            if (hourIndex != -1 && minuteIndex != -1 && dayIndex != -1 &&
                                    monthIndex != -1 && yearIndex != -1) {

                                if(!updateTime) {
                                    jam = cursor.getInt(hourIndex);
                                    menit = cursor.getInt(minuteIndex);
                                }
                                if(!updateDate) {
                                    hari = cursor.getInt(dayIndex);
                                    bulan = cursor.getInt(monthIndex);
                                    tahun = cursor.getInt(yearIndex);
                                }

                                // Buat alarm baru dengan kombinasi nilai baru dan lama
                                alarmId = dbHelper.addAlarm(jam, menit, hari, bulan, tahun);
                                if (alarmId != -1) {
                                    taskMap.put("alarmId", alarmId);
                                }
                            }
                        }

                        if (cursor != null) {
                            cursor.close();
                        }
                    }



                    if (!taskMap.isEmpty()) {
                        firestore.collection("task").document(userId)
                                .collection("myTask").document(id)
                                .update(taskMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            if (!dueDateReminder.isEmpty() && !dueTimeReminder.isEmpty()) {
                                                if (alarmId != -1) {
                                                    date = hari + "/" + (bulan + 1) + "/" + tahun;
                                                    Toast.makeText(context,
                                                            "Set due date reminder for "+ date + " at " + jam + ":" + menit,
                                                            Toast.LENGTH_SHORT).show();
                                                    setTimer(alarmId);
                                                    cancelPreviousAlarm(alarmIdUpdate);
                                                    notification();
                                                }
                                            }

                                            Toast.makeText(context, R.string.task_updated_successfully, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(context, getString(R.string.no_task_changes_detected), Toast.LENGTH_SHORT).show();
                    }

                } else {
                    if (task.isEmpty() || dueDate.isEmpty() || dueTime.isEmpty()) {
                        Toast.makeText(context, getString(R.string.please_fill_in_all_fields), Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        long alarmIdNew = dbHelper.addAlarm(jam, menit, hari, bulan, tahun);

                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("task", task);
                        taskMap.put("dueDate", dueDate);
                        taskMap.put("dueTime", dueTime);
                        taskMap.put("dueDateReminder", dueDateReminder);
                        taskMap.put("dueTimeReminder", dueTimeReminder);
                        if (alarmIdNew != -1){
                            taskMap.put("alarmId", alarmIdNew);
                        }
                        firestore.collection("task").document(userId)
                                .collection("myTask").add(taskMap)
                                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentReference> task) {
                                        if (task.isSuccessful()) {
                                            if (dueDateReminder != null && !dueDateReminder.isEmpty() &&
                                                    dueTimeReminder != null && !dueTimeReminder.isEmpty()) {

                                                if (alarmIdNew != -1) {
                                                    date = hari + "/" + (bulan + 1) + "/" + tahun;
                                                    Toast.makeText(context,
                                                            "Set due date reminder for "+ date + " at " + jam + ":" + menit,
                                                            Toast.LENGTH_SHORT).show();
                                                    setTimer(alarmIdNew);
                                                    notification();
                                                }
                                            }
                                            Toast.makeText(context, R.string.task_added_successfully, Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(context, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                }
                dismiss();
            }
        });

    }


    private void cancelPreviousAlarm(long alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, MyBroadcastReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT  | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);

        Log.d("Alarm", "Alarm with ID " + alarmId + " has been cancelled.");
    }


    private boolean isDateBeforeDueDate(int year, int month, int dayOfMonth) {
        String[] dueDateParts = dueDate.split("/");
        int dueDay = Integer.parseInt(dueDateParts[0]);
        int dueMonth = Integer.parseInt(dueDateParts[1]);
        int dueYear = Integer.parseInt(dueDateParts[2]);

        Calendar dueCalendar = Calendar.getInstance();
        dueCalendar.set(dueYear, dueMonth - 1, dueDay);

        Calendar reminderCalendar = Calendar.getInstance();
        reminderCalendar.set(year, month - 1, dayOfMonth);


        return reminderCalendar.before(dueCalendar);
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity instanceof  OnDialogCloseListner){
            ((OnDialogCloseListner)activity).onDialogClose(dialog);
        }
    }
    private void notification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "Notify",
                    "Task",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("Alarm Notification");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setBypassDnd(true);

            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.task);
            channel.setSound(soundUri, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);



        }
    }

    private void setTimer(long alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar cal_alarm = Calendar.getInstance();
        Calendar cal_now = Calendar.getInstance();

        cal_alarm.set(Calendar.YEAR, tahun);
        cal_alarm.set(Calendar.MONTH, bulan - 1);
        cal_alarm.set(Calendar.DAY_OF_MONTH, hari);
        cal_alarm.set(Calendar.HOUR_OF_DAY, jam);
        cal_alarm.set(Calendar.MINUTE, menit);
        cal_alarm.set(Calendar.SECOND, 0);

        if (cal_alarm.before(cal_now)) {
            cal_alarm.add(Calendar.DATE, 1);
        }

        Intent i = new Intent(context, MyBroadcastReceiver.class);
        i.putExtra("alarm_id", alarmId);
        i.setAction("com.example.unique_action." + alarmId);

        i.putExtra("alarm_id", alarmId);
        i.putExtra("task_name", task.isEmpty() ? mTaskEdit.getText().toString().trim() : task);
        i.putExtra("due_date", dueDate);
        i.putExtra("due_time", dueTime);

        Log.d("DEBUG", "Task: " + (task.isEmpty() ? mTaskEdit.getText().toString().trim() : task) + ", Due Date: " + dueDate + ", Due Time: " + dueTime);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) alarmId,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.set(AlarmManager.RTC_WAKEUP, cal_alarm.getTimeInMillis(), pendingIntent);
    }
}



