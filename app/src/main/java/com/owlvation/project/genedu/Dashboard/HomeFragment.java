package com.owlvation.project.genedu.Dashboard;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.owlvation.project.genedu.Dashboard.HomeHelper.RecentNotesAdapter;
import com.owlvation.project.genedu.Dashboard.HomeHelper.RecentTasksAdapter;
import com.owlvation.project.genedu.Dashboard.HomeHelper.StudySessionManager;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.Note.CreateNoteActivity;
import com.owlvation.project.genedu.Note.NoteActivity;
import com.owlvation.project.genedu.Note.NoteModel;
import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.TaskModel;
import com.owlvation.project.genedu.Task.TaskActivity;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private TextView tvGreeting, tvNotesCount, tvViewAll, tvViewAllTasks, tvStreakCount, tvTotalTime, tvMostProductiveDay, tvTasksCount, tvNoNotes, tvNoTasks;
    private RecyclerView rvRecentNotes, rvRecentTasks;
    private FirebaseFirestore db;
    private CircleImageView imageAccount;
    private View rootView;
    private RecentNotesAdapter recentNotesAdapter;
    private RecentTasksAdapter recentTasksAdapter;
    private List<NoteModel> recentNotesList;
    private List<TaskModel> recentTasksList;
    private LineChart usageChart;
    private ProgressDialog progressDialog;
    private NetworkChangeReceiver networkChangeReceiver;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        networkChangeReceiver = new NetworkChangeReceiver();

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);

        initializeViews();
        setupUsageChart();
        setupClickListeners();
        setupSearchView();
        setupRecyclerView();
        setupTaskRecyclerView();

        loadProfileName();
        loadProfileImage();
        loadNotesData();
        loadTasksData();

        return rootView;
    }

    private void initializeViews() {
        imageAccount = rootView.findViewById(R.id.imageAccount);
        tvGreeting = rootView.findViewById(R.id.tvGreeting);
        tvNotesCount = rootView.findViewById(R.id.tvNotesCount);
        tvTasksCount = rootView.findViewById(R.id.tvTasksCount);
        tvStreakCount = rootView.findViewById(R.id.tvStreakCount);
        tvTotalTime = rootView.findViewById(R.id.tvTotalTime);
        tvMostProductiveDay = rootView.findViewById(R.id.tvMostProductiveDay);
        tvViewAll = rootView.findViewById(R.id.tvViewAll);
        tvViewAllTasks = rootView.findViewById(R.id.tvViewAllTask);
        rvRecentNotes = rootView.findViewById(R.id.rvRecentNotes);
        rvRecentTasks = rootView.findViewById(R.id.rvRecentTasks);
        tvNoNotes = rootView.findViewById(R.id.tvNoNotes);
        tvNoTasks = rootView.findViewById(R.id.tvNoTasks);
        usageChart = rootView.findViewById(R.id.usageChart);
    }

    private void setupClickListeners() {
        imageAccount.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigation);
            bottomNav.setSelectedItemId(R.id.bottom_profile);
        });

        tvViewAll.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NoteActivity.class))
        );

        tvViewAllTasks.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), TaskActivity.class)));
    }

    private void setupSearchView() {
        SearchView searchView = rootView.findViewById(R.id.svMain);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void setupUsageChart() {
        if (!isAdded()) return;

        StudySessionManager.getInstance(requireContext()).getStudySummary().observe(getViewLifecycleOwner(), summary -> {
            if (!isAdded()) return;

            tvStreakCount.setText(getString(R.string.streak_count, summary.getCurrentStreak()));

            int hours = summary.getTotalWeeklyMinutes() / 60;
            int minutes = summary.getTotalWeeklyMinutes() % 60;
            if (hours > 0) {
                tvTotalTime.setText(getString(R.string.time_hours_minutes, hours, minutes));
            } else {
                tvTotalTime.setText(getString(R.string.time_minutes, minutes));
            }

            try {
                SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat output = new SimpleDateFormat("EEEE", Locale.getDefault());
                Date date = input.parse(summary.getMostProductiveDay());
                String dayName = output.format(date);
                tvMostProductiveDay.setText(getString(R.string.most_productive_day, dayName));
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date", e);
            }
        });

        StudySessionManager.getInstance(requireContext()).getWeeklyStats().observe(getViewLifecycleOwner(), dailyStats -> {
            if (!isAdded()) return;

            ArrayList<Entry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("EEE", Locale.getDefault());

            for (int i = 0; i < dailyStats.size(); i++) {
                StudySessionManager.DailyStudyData data = dailyStats.get(i);
                entries.add(new Entry(i, data.getMinutes()));

                try {
                    Date date = input.parse(data.getDate());
                    labels.add(output.format(date));
                } catch (ParseException e) {
                    labels.add("Day " + (i + 1));
                }
            }

            updateChart(entries, labels);
        });
    }

    private void updateChart(ArrayList<Entry> entries, ArrayList<String> labels) {
        if (!isAdded()) return;

        try {
            LineDataSet dataSet = new LineDataSet(entries, "Study Time");
            dataSet.setColor(getResources().getColor(R.color.colorPrimary));
            dataSet.setValueTextColor(getResources().getColor(R.color.colorPrimaryDark));
            dataSet.setLineWidth(2f);
            dataSet.setCircleColor(getResources().getColor(R.color.colorPrimaryDark));
            dataSet.setCircleRadius(4f);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format(Locale.getDefault(), "%.0f", value);
                }
            });

            dataSet.setDrawValues(true);
            dataSet.setHighlightEnabled(true);
            dataSet.setDrawHighlightIndicators(true);
            dataSet.setHighLightColor(getResources().getColor(R.color.colorPrimaryDark));

            LineData lineData = new LineData(dataSet);
            usageChart.setData(lineData);

            XAxis xAxis = usageChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setAxisMinimum(0f);
            xAxis.setGranularity(1f);
            xAxis.setDrawGridLines(false);
            xAxis.setTextColor(getResources().getColor(R.color.colorPrimary));
            xAxis.setTextSize(12f);

            YAxis leftAxis = usageChart.getAxisLeft();
            leftAxis.setAxisMinimum(0f);
            leftAxis.setGranularity(1f);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(getResources().getColor(R.color.colorAccent));
            leftAxis.setTextColor(getResources().getColor(R.color.colorPrimary));
            leftAxis.setTextSize(12f);
            CustomMarkerView marker = new CustomMarkerView(requireContext(), R.layout.custom_marker_view);
            marker.setChartView(usageChart);
            usageChart.setMarker(marker);

            usageChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    usageChart.moveViewToX(e.getX());
                    usageChart.highlightValue(h);
                }

                @Override
                public void onNothingSelected() {
                    usageChart.highlightValue(null);
                }
            });

            usageChart.getAxisRight().setEnabled(false);
            usageChart.getDescription().setEnabled(false);
            usageChart.getLegend().setEnabled(false);
            usageChart.setTouchEnabled(true);
            usageChart.setDragEnabled(true);
            usageChart.setScaleEnabled(false);
            usageChart.animateY(1000);
            usageChart.invalidate();



        } catch (Exception e) {
            Log.e(TAG, "Error updating chart", e);
        }
    }

    private void setupRecyclerView() {
        recentNotesList = new ArrayList<>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);

        rvRecentNotes.setLayoutManager(layoutManager);
        rvRecentNotes.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position != parent.getAdapter().getItemCount() - 1) {
                    outRect.right = 24;
                }
            }
        });

        recentNotesAdapter = new RecentNotesAdapter(requireContext(), recentNotesList, note -> {
            Intent intent = new Intent(requireContext(), CreateNoteActivity.class);
            intent.putExtra("noteId", note.getDocumentId());
            intent.putExtra("title", note.getTitle());
            intent.putExtra("content", note.getContent());
            intent.putExtra("imageUrl", note.getImageUrl());
            startActivity(intent);
        });
        rvRecentNotes.setAdapter(recentNotesAdapter);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvRecentNotes);
    }

    private void setupTaskRecyclerView() {
        recentTasksList = new ArrayList<>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);

        rvRecentTasks.setLayoutManager(layoutManager);

        rvRecentTasks.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                if (position != parent.getAdapter().getItemCount() - 1) {
                    outRect.right = 24;
                }
            }
        });

        recentTasksAdapter = new RecentTasksAdapter(requireContext(), recentTasksList, task -> {
            startActivity(new Intent(requireContext(), TaskActivity.class));
        });
        rvRecentTasks.setAdapter(recentTasksAdapter);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvRecentTasks);
    }

    private void loadProfileName() {
        progressDialog.show();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && isAdded()) {
                            String profileName = documentSnapshot.getString("uName");
                            tvGreeting.setText(getString(R.string.greetings, profileName));
                        } else {
                            Log.e("HomeFragment", "User document doesn't exist");
                        }
                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HomeFragment", "Error fetching user profile", e);
                        progressDialog.dismiss();
                    });
        } else {
            Log.e("HomeFragment", "Current user is null");
            progressDialog.dismiss();
        }
    }

    private void loadProfileImage() {
        progressDialog.show();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference().child("users/" + currentUser.getUid() + "/profile.jpg");
            storageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (isAdded()) {
                            Picasso.get().load(uri).into(imageAccount);
                        }
                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(exception -> {
                        Log.e("HomeFragment", "Error loading profile image: " + exception.getMessage());
                        progressDialog.dismiss();
                    });
        } else {
            Log.e("HomeFragment", "Current user is null");
            progressDialog.dismiss();
        }
    }

    private void loadNotesData() {
        progressDialog.show();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("notes")
                    .document(currentUser.getUid())
                    .collection("myNotes")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (isAdded()) {
                            int totalNotes = queryDocumentSnapshots.size();
                            tvNotesCount.setText(getString(R.string.notes_count, totalNotes));

                            recentNotesList.clear();
                            int count = 0;
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                if (count >= 3) break;

                                NoteModel note = document.toObject(NoteModel.class);
                                if (note != null) {
                                    note.setDocumentId(document.getId());
                                    recentNotesList.add(note);
                                    count++;
                                }
                            }
                            recentNotesAdapter.notifyDataSetChanged();

                            if (recentNotesList.isEmpty()) {
                                tvNoNotes.setVisibility(View.VISIBLE);
                            } else {
                                tvNoNotes.setVisibility(View.GONE);
                            }
                        }
                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HomeFragment", "Error fetching notes", e);
                        progressDialog.dismiss();
                    });
        }
    }

    private void loadTasksData() {
        progressDialog.show();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("task")
                    .document(currentUser.getUid())
                    .collection("myTask")
                    .orderBy("dueDate", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (isAdded()) {
                            int totalTasks = queryDocumentSnapshots.size();
                            tvTasksCount.setText(getString(R.string.tasks_count, totalTasks));

                            recentTasksList.clear();
                            int count = 0;
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                Log.d("TaskData", "Document: " + document.getData());
                                if (count >= 3) break;

                                TaskModel task = document.toObject(TaskModel.class);
                                if (task != null) {
                                    task.withId(document.getId());
                                    recentTasksList.add(task);
                                    count++;
                                }
                            }
                            recentTasksAdapter.notifyDataSetChanged();

                            if (recentTasksList.isEmpty()) {
                                tvNoTasks.setVisibility(View.VISIBLE);
                            } else {
                                tvNoTasks.setVisibility(View.GONE);
                            }
                        }
                        progressDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HomeFragment", "Error fetching tasks", e);
                        progressDialog.dismiss();
                    });
        }
    }

    private void filter(String text) {
        if (recentTasksList != null && !text.isEmpty()) {
            List<TaskModel> filteredTasks = new ArrayList<>();
            for (TaskModel task : recentTasksList) {
                if (task.getTask().toLowerCase().contains(text.toLowerCase())) {
                    filteredTasks.add(task);
                }
            }
            recentTasksAdapter.filterTasks(filteredTasks);
        } else if (recentTasksList != null) {
            recentTasksAdapter.filterTasks(recentTasksList);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        requireActivity().registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(networkChangeReceiver);
    }
}