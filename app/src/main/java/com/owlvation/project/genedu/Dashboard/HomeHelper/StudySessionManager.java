package com.owlvation.project.genedu.Dashboard.HomeHelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudySessionManager {
    private static final String TAG = "StudySessionManager";
    private static final String PREFS_NAME = "StudySessionPrefs";
    private static final String KEY_LAST_RESET_DATE = "lastResetDate";
    private static StudySessionManager instance;
    private final MutableLiveData<List<DailyStudyData>> weeklyStats = new MutableLiveData<>();
    private final MutableLiveData<StudySummary> studySummary = new MutableLiveData<>();
    private long sessionStartTime;
    private boolean isSessionActive = false;
    private long totalSessionTime = 0;
    private long pauseStartTime = 0;
    private Handler updateHandler;
    private static final int UPDATE_INTERVAL = 60000;
    private SharedPreferences sharedPreferences;
    private Context context;

    private StudySessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        updateHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized StudySessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new StudySessionManager(context);
        }
        return instance;
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSessionActive && pauseStartTime == 0) {
                long currentTime = System.currentTimeMillis();
                long timeSpent = currentTime - sessionStartTime;
                totalSessionTime += timeSpent;
                sessionStartTime = currentTime;

                saveStudyTime();

                updateHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        }
    };

    public LiveData<StudySummary> getStudySummary() {
        updateWeeklyStats();
        return studySummary;
    }

    public LiveData<List<DailyStudyData>> getWeeklyStats() {
        updateWeeklyStats();
        return weeklyStats;
    }

    private void updateWeeklyStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date startDate = cal.getTime();
        String startDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate);

        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.DAY_OF_YEAR, 6);
        String endDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCal.getTime());

        FirebaseFirestore.getInstance()
                .collection("usage_stats")
                .document(currentUser.getUid())
                .collection("daily")
                .orderBy("date", Query.Direction.ASCENDING)
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThanOrEqualTo("date", endDateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DailyStudyData> stats = new ArrayList<>();
                    Map<String, Integer> dailyMinutes = new HashMap<>();
                    int totalWeeklyMinutes = 0;
                    int currentStreak = 0;
                    int maxMinutes = 0;
                    String mostProductiveDay = "";

                    String lastDateWithStudy = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        Long minutes = document.getLong("minutes");
                        if (date != null && minutes != null) {
                            dailyMinutes.put(date, minutes.intValue());
                            totalWeeklyMinutes += minutes;

                            if (minutes > 0) {
                                if (lastDateWithStudy != null) {
                                    try {
                                        Calendar lastCal = Calendar.getInstance();
                                        lastCal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(lastDateWithStudy));
                                        lastCal.add(Calendar.DAY_OF_YEAR, 1);

                                        Calendar currentCal = Calendar.getInstance();
                                        currentCal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date));

                                        if (currentCal.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR) &&
                                                currentCal.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR)) {
                                            currentStreak++;
                                        } else {
                                            currentStreak = 1;
                                        }
                                    } catch (ParseException e) {
                                        Log.e(TAG, "Error parsing date", e);
                                        currentStreak = 0;
                                    }
                                } else {
                                    currentStreak = 1;
                                }
                                lastDateWithStudy = date;
                            } else {
                                currentStreak = 0;
                            }

                            if (minutes > maxMinutes) {
                                maxMinutes = minutes.intValue();
                                mostProductiveDay = date;
                            }
                        }
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(startDate);

                    for (int i = 0; i < 7; i++) {
                        String dateStr = sdf.format(calendar.getTime());
                        int minutesStudied = dailyMinutes.containsKey(dateStr) ? dailyMinutes.get(dateStr) : 0;
                        stats.add(new DailyStudyData(dateStr, minutesStudied));
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    weeklyStats.setValue(stats);

                    StudySummary summary = new StudySummary(
                            currentStreak,
                            totalWeeklyMinutes,
                            mostProductiveDay,
                            maxMinutes
                    );
                    studySummary.setValue(summary);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching weekly stats", e));
    }

    private void deleteLastWeekData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Date startDate = cal.getTime();
        String startDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate);

        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.DAY_OF_YEAR, 6);
        String endDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCal.getTime());

        FirebaseFirestore.getInstance()
                .collection("usage_stats")
                .document(currentUser.getUid())
                .collection("daily")
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThanOrEqualTo("date", endDateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted last week's data: " + document.getId()))
                                .addOnFailureListener(e -> Log.e(TAG, "Error deleting last week's data", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching last week's data", e));
    }

    public void startSession() {
        if (!isSessionActive) {
            if (isNewWeek()) {
                deleteLastWeekData();
                setLastResetDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
                resetSession();
                weeklyStats.setValue(new ArrayList<>());
                studySummary.setValue(new StudySummary(0, 0, "", 0));
            }

            sessionStartTime = System.currentTimeMillis();
            isSessionActive = true;
            pauseStartTime = 0;
            Log.d(TAG, "Study session started at: " + new Date(sessionStartTime));

            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
        }
    }

    public void pauseSession() {
        if (isSessionActive && pauseStartTime == 0) {
            pauseStartTime = System.currentTimeMillis();
            long timeSpent = pauseStartTime - sessionStartTime;
            totalSessionTime += timeSpent;

            updateHandler.removeCallbacks(updateRunnable);

            Log.d(TAG, "Study session paused at: " + new Date(pauseStartTime));

            saveStudyTime();
        }
    }

    public void resumeSession() {
        if (isSessionActive && pauseStartTime != 0) {
            sessionStartTime = System.currentTimeMillis();
            pauseStartTime = 0;
            Log.d(TAG, "Study session resumed at: " + new Date(sessionStartTime));

            updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
        }
    }

    public void endSession() {
        if (isSessionActive) {
            long endTime = System.currentTimeMillis();
            if (pauseStartTime == 0) {
                totalSessionTime += (endTime - sessionStartTime);
            }

            updateHandler.removeCallbacks(updateRunnable);

            saveStudyTime();
            resetSession();
            Log.d(TAG, "Study session ended. Total time: " + (totalSessionTime / 1000 / 60) + " minutes");
        }
    }

    private void saveStudyTime() {
        int minutes = (int) (totalSessionTime / (1000 * 60));
        if (minutes <= 0) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        DocumentReference dailyRef = FirebaseFirestore.getInstance()
                .collection("usage_stats")
                .document(currentUser.getUid())
                .collection("daily")
                .document(today);

        dailyRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    int existingMinutes = 0;
                    if (documentSnapshot.exists()) {
                        Long currentMinutes = documentSnapshot.getLong("minutes");
                        existingMinutes = currentMinutes != null ? currentMinutes.intValue() : 0;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("minutes", existingMinutes + minutes);
                    data.put("date", today);
                    data.put("lastUpdated", new Date());

                    dailyRef.set(data)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Study time saved successfully");
                                totalSessionTime = 0;
                                updateWeeklyStats();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error saving study time", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error reading existing study time", e));
    }

    private void resetSession() {
        isSessionActive = false;
        totalSessionTime = 0;
        sessionStartTime = 0;
        pauseStartTime = 0;
        updateHandler.removeCallbacks(updateRunnable);
    }

    private boolean isNewWeek() {
        String lastResetDateStr = getLastResetDate();
        if (lastResetDateStr.isEmpty()) {
            setLastResetDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            return false;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date lastResetDate = sdf.parse(lastResetDateStr);
            Date currentDate = new Date();

            Calendar lastResetCal = Calendar.getInstance();
            lastResetCal.setTime(lastResetDate);

            Calendar currentCal = Calendar.getInstance();
            currentCal.setTime(currentDate);

            if (currentCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY &&
                    currentCal.get(Calendar.HOUR_OF_DAY) == 0 &&
                    currentCal.get(Calendar.MINUTE) == 0) {
                return true;
            }

            return currentCal.get(Calendar.WEEK_OF_YEAR) != lastResetCal.get(Calendar.WEEK_OF_YEAR);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
            return false;
        }
    }

    private String getLastResetDate() {
        return sharedPreferences.getString(KEY_LAST_RESET_DATE, "");
    }

    private void setLastResetDate(String date) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_RESET_DATE, date);
        editor.apply();
    }

    public void cleanup() {
        if (updateHandler != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    public static class StudySummary {
        private final int currentStreak;
        private final int totalWeeklyMinutes;
        private final String mostProductiveDay;
        private final int maxDailyMinutes;

        public StudySummary(int currentStreak, int totalWeeklyMinutes, String mostProductiveDay, int maxDailyMinutes) {
            this.currentStreak = currentStreak;
            this.totalWeeklyMinutes = totalWeeklyMinutes;
            this.mostProductiveDay = mostProductiveDay;
            this.maxDailyMinutes = maxDailyMinutes;
        }

        public int getCurrentStreak() {
            return currentStreak;
        }

        public int getTotalWeeklyMinutes() {
            return totalWeeklyMinutes;
        }

        public String getMostProductiveDay() {
            return mostProductiveDay;
        }

        public int getMaxDailyMinutes() {
            return maxDailyMinutes;
        }
    }

    public static class DailyStudyData {
        private final String date;
        private final int minutes;

        public DailyStudyData(String date, int minutes) {
            this.date = date;
            this.minutes = minutes;
        }

        public String getDate() {
            return date;
        }

        public int getMinutes() {
            return minutes;
        }
    }
}