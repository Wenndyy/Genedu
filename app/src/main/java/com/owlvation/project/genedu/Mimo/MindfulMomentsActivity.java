package com.owlvation.project.genedu.Mimo;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.owlvation.project.genedu.R;

import java.util.Random;

public class MindfulMomentsActivity extends AppCompatActivity {
    private long timeSelected = 0;
    private CountDownTimer timeCountDown;
    private long timeProgress = 0;
    private long pauseOffSet = 0;
    private boolean isStart = true;
    ImageView icBack;
    private Dialog currentDialog;
    private MediaPlayer mediaPlayer;
    private int currentTrackIndex = -1;
    private static final int PICK_AUDIO_REQUEST = 1;
    private Uri selectedMusicUri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mindful_moments);

        LinearLayout add = findViewById(R.id.add);
        add.setOnClickListener(v -> setTimeFunction());

        Button startBtn = findViewById(R.id.btnPlayPause);
        startBtn.setOnClickListener(v -> startTimerSetup());

        LinearLayout reset = findViewById(R.id.reset);
        reset.setOnClickListener(v -> resetTime());

        icBack = findViewById(R.id.ic_back);
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


    }

    private void resetTime() {
        if (timeCountDown != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.reset_time_mimo);
            builder.setPositiveButton(R.string.positive_button_mimo, (dialog, which) -> {
                if (timeCountDown != null) {
                    timeCountDown.cancel();
                    timeCountDown = null;
                }
                timeProgress = 0;
                timeSelected = 0;
                pauseOffSet = 0;

                Button startBtn = findViewById(R.id.btnPlayPause);
                startBtn.setText(R.string.start_mimo);
                isStart = true;

                ProgressBar progressBar = findViewById(R.id.pbTimer);
                progressBar.setProgress(0);

                TextView timeLeftTv = findViewById(R.id.tvTimeLeft);
                timeLeftTv.setText(R.string.time_left_mimo);

                LinearLayout add = findViewById(R.id.add);
                add.setEnabled(true);
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                selectedMusicUri = null;
                Toast.makeText(this, R.string.music_selection, Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton(R.string.negative_button_mimo, (dialog, which) -> {
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void timePause() {
        if (timeCountDown != null) {
            timeCountDown.cancel();
            pauseOffSet = timeProgress;
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }


    private void startTimerSetup() {
        Button startBtn = findViewById(R.id.btnPlayPause);
        LinearLayout add = findViewById(R.id.add);
        LinearLayout reset = findViewById(R.id.reset);
        if (timeSelected > timeProgress) {
            if (isStart) {
                startBtn.setText(R.string.pause_mimo);
                startTimer(pauseOffSet);
                add.setEnabled(false);
                reset.setEnabled(true);
                isStart = false;
            } else {
                isStart = true;
                startBtn.setText(R.string.resume_mimo);
                add.setEnabled(false);
                reset.setEnabled(true);
                timePause();
            }
        } else {
            Toast.makeText(this, R.string.toast_warning_start_mimo, Toast.LENGTH_SHORT).show();
        }
    }


    private void setTimeFunction() {
        currentDialog = new Dialog(this);
        currentDialog.setContentView(R.layout.dialog_add_mimo);

        EditText etHours = currentDialog.findViewById(R.id.etHours);
        EditText etMinutes = currentDialog.findViewById(R.id.etMinutes);
        EditText etSeconds = currentDialog.findViewById(R.id.etSeconds);
        Button btnOk = currentDialog.findViewById(R.id.btnOk);
        LinearLayout btnAddMusic = currentDialog.findViewById(R.id.add);


        btnAddMusic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_music)), PICK_AUDIO_REQUEST);
        });


        btnOk.setOnClickListener(v -> {
            long hours = etHours.getText().toString().isEmpty() ? 0 : Long.parseLong(etHours.getText().toString());
            long minutes = etMinutes.getText().toString().isEmpty() ? 0 : Long.parseLong(etMinutes.getText().toString());
            long seconds = etSeconds.getText().toString().isEmpty() ? 0 : Long.parseLong(etSeconds.getText().toString());

            timeSelected = (hours * 3600 + minutes * 60 + seconds) * 1000;
            if (timeSelected <= 0) {
                Toast.makeText(this, R.string.toast_warning_time_not_selected, Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedMusicUri == null) {
                Toast.makeText(this, R.string.toast_warning_music_not_selected, Toast.LENGTH_SHORT).show();
                return;
            }

            String formattedTime = convertTimeToString(timeSelected / 1000);

            TextView timeSet = findViewById(R.id.tvTimeLeft);
            if (timeSet != null) {
                timeSet.setText(formattedTime);
            }
            currentDialog.dismiss();
            currentDialog = null;
        });

        currentDialog.show();
    }


    private void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        try {
            if (selectedMusicUri != null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, selectedMusicUri);
                mediaPlayer.prepare();
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(mp -> {
                if (timeCountDown != null && timeProgress < timeSelected) {
                    playMusic();
                } else {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_playing_music, Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer(long pauseOffSetL) {
        ProgressBar progressBar = findViewById(R.id.pbTimer);
        progressBar.setMax((int) (timeSelected / 1000));
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        } else {
            playMusic();
        }

        timeCountDown = new CountDownTimer(timeSelected - pauseOffSetL, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeProgress = timeSelected - millisUntilFinished;
                progressBar.setProgress((int) (millisUntilFinished / 1000));
                TextView timeLeftTv = findViewById(R.id.tvTimeLeft);
                timeLeftTv.setText(convertTimeToString(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timeProgress = 0;
                timeSelected = 0;
                pauseOffSet = 0;

                Button startBtn = findViewById(R.id.btnPlayPause);
                startBtn.setText(R.string.start_mimo);
                isStart = true;

                ProgressBar progressBar = findViewById(R.id.pbTimer);
                progressBar.setProgress(0);

                TextView timeLeftTv = findViewById(R.id.tvTimeLeft);
                timeLeftTv.setText(R.string.time_left_mimo);

                LinearLayout add = findViewById(R.id.add);
                add.setEnabled(true);

                LinearLayout reset = findViewById(R.id.reset);
                reset.setEnabled(false);

                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                Toast.makeText(MindfulMomentsActivity.this, R.string.toast_times_up_mimo, Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private String convertTimeToString(long timeInSeconds) {
        long hours = timeInSeconds / 3600;
        long minutes = (timeInSeconds % 3600) / 60;
        long seconds = timeInSeconds % 60;

        return String.format(getString(R.string.format_timer_mimo), hours, minutes, seconds);
    }

    @Override
    public void onBackPressed() {
        if ((timeCountDown != null && timeSelected > timeProgress) || (mediaPlayer != null && mediaPlayer.isPlaying())) {
            Toast.makeText(this, R.string.toast_exit_mimo, Toast.LENGTH_SHORT).show();
        } else {
            stopMusicAndTimer();
            selectedMusicUri = null;
            super.onBackPressed();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeCountDown != null) {
            timeCountDown.cancel();
            timeProgress = 0;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        selectedMusicUri = null;
    }

    private void stopMusicAndTimer() {
        if (timeCountDown != null) {
            timeCountDown.cancel();
            timeProgress = 0;
        }

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        selectedMusicUri = null;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri newMusicUri = data.getData();
            if (newMusicUri != null) {
                selectedMusicUri = newMusicUri;
                String fileName = getFileNameFromUri(selectedMusicUri);

                // Debug log
                Log.d("MindfulMoments", "Selected music URI: " + selectedMusicUri);
                Log.d("MindfulMoments", "File name: " + fileName);

                // Memperbarui TextView dalam dialog
                updateDialogMusicName(fileName);
            }
        }
    }
    private void updateDialogMusicName(String fileName) {
        if (currentDialog != null && currentDialog.isShowing()) {
            TextView selectedMusicText = currentDialog.findViewById(R.id.select_music_name);
            if (selectedMusicText != null) {
                selectedMusicText.setText(fileName);
                Log.d("MindfulMoments", "File name: " + fileName);
            } else {
                Log.e("MindfulMoments", "TextView with ID select_music not found in the dialog!");
            }
        }
    }
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        } else if (uri.getScheme().equals("file")) {
            result = uri.getLastPathSegment();
        }
        if (result == null) {
            result = "Unknown file";
        }
        return result;
    }



}