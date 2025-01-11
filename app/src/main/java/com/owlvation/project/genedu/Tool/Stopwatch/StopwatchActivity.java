package com.owlvation.project.genedu.Tool.Stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StopwatchActivity extends AppCompatActivity {
    private TextView timer;
    private Button startPauseButton;
    private LinearLayout save, reset;
    private ImageView icBack;
    private long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    private Handler handler;
    private RecyclerView recyclerView;
    private TimerAdapter timerAdapter;
    private List<String> timerList;
    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);
        icBack = findViewById(R.id.ic_back);

        timer = findViewById(R.id.timer);
        startPauseButton = findViewById(R.id.start_pauseBtn);
        reset = findViewById(R.id.reset);
        save = findViewById(R.id.save);
        recyclerView  =  findViewById(R.id.recyclerView);

        handler = new Handler();
        timerList = new ArrayList<>();

        timerAdapter = new TimerAdapter(timerList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(timerAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        startPauseButton.setOnClickListener(view -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        reset.setOnClickListener(view -> {
            if (isRunning) {
                Toast.makeText(StopwatchActivity.this, R.string.toast_pause_timer, Toast.LENGTH_LONG).show();
            } else if (TimeBuff == 0L) {
                Toast.makeText(this, R.string.warning_reset_sw_firs, Toast.LENGTH_SHORT).show();
            } else {
                resetTimer();
            }
        });

        save.setOnClickListener(view -> {
            if (!isRunning && TimeBuff == 0L) {
                Toast.makeText(StopwatchActivity.this, R.string.toast_save_laps, Toast.LENGTH_LONG).show();
            } else {
                timerList.add(timer.getText().toString());
                timerAdapter.notifyDataSetChanged();
            }
        });

        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    Toast.makeText(StopwatchActivity.this, R.string.toast_exit_stopwatch, Toast.LENGTH_SHORT).show();
                } else {
                    showExitConfirm();
                }
            }
        });
    }

    private void startTimer() {
        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
        startPauseButton.setText(R.string.pause_mimo);
        isRunning = true;
    }

    private void pauseTimer() {
        TimeBuff += MillisecondTime;
        handler.removeCallbacks(runnable);
        startPauseButton.setText(R.string.resume_mimo);
        isRunning = false;
    }

    private void resetTimer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.warning_reset_timer);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            MillisecondTime = 0L;
            StartTime = 0L;
            TimeBuff = 0L;
            UpdateTime = 0L;

            timer.setText(R.string.time_left_sw);
            timerList.clear();
            timerAdapter.notifyDataSetChanged();

            startPauseButton.setText(R.string.start_mimo);
            isRunning = false;
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public Runnable runnable = new Runnable() {
        public void run() {
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;
            UpdateTime = TimeBuff + MillisecondTime;

            int Hours = (int) (UpdateTime / (1000 * 60 * 60));
            int Minutes = (int) (UpdateTime % (1000 * 60 * 60)) / (1000 * 60);
            int Seconds = (int) ((UpdateTime % (1000 * 60 * 60)) % (1000 * 60) / 1000);
            int MilliSeconds = (int) (UpdateTime % 1000) / 10;

            timer.setText(String.format(getString(R.string.format_stopwatch_timer), Hours, Minutes, Seconds, MilliSeconds));

            handler.postDelayed(this, 0);
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isRunning) {
            Toast.makeText(this, R.string.toast_exit_stopwatch, Toast.LENGTH_SHORT).show();
        } else {
            showExitConfirm();
        }
    }

    private void showExitConfirm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.warning_exit_stopwatch);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            finish();
        });
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
