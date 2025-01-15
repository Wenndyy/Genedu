package com.owlvation.project.genedu.Task;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Adapter.TaskAdapter;
import com.owlvation.project.genedu.Task.Model.AlarmDatabaseHelper;
import com.owlvation.project.genedu.Task.Model.TaskModel;

import java.util.ArrayList;
import java.util.List;

public class TaskActivity extends AppCompatActivity implements OnDialogCloseListner {

    private ImageView icBack;
    private RecyclerView recyclerView;
    private FloatingActionButton mFab;
    private FirebaseFirestore firestore;
    private TaskAdapter adapter;
    private List<TaskModel> mList;
    private Query query;
    SearchView searchView;
    OnQueryTextListener queryTextListener;
    SwipeRefreshLayout swipeRefreshLayout;

    private NetworkChangeReceiver networkChangeReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        searchView = findViewById(R.id.searchTask);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        networkChangeReceiver = new NetworkChangeReceiver();

        queryTextListener = new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTask(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTask(newText);
                return true;
            }
        };

        searchView.setOnQueryTextListener(queryTextListener);

        icBack = findViewById(R.id.ic_back);
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.recycerlview);
        mFab = findViewById(R.id.floatingActionButton);
        firestore = FirebaseFirestore.getInstance();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(TaskActivity.this));

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddNewTask.newInstance().show(getSupportFragmentManager(), AddNewTask.TAG);
            }
        });



        mList = new ArrayList<>();
        adapter = new TaskAdapter(TaskActivity.this, mList,this);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new TouchHelper(adapter, mList));
        itemTouchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showData();
            }
        });

        showData();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("task_name")) {
            String id = intent.getStringExtra("id");
            String taskName = intent.getStringExtra("task_name");
            String dueDate = intent.getStringExtra("due_date");
            String dueTime = intent.getStringExtra("due_time");
            int status = intent.getIntExtra("status",0);

            showDetailTaskDialog(id, taskName, dueDate, dueTime,status);
        } else {
            Log.e("TaskActivity", "Intent is null");
        }
        logAllAlarms(this);
    }

    private void showDetailTaskDialog(String id, String taskName, String dueDate, String dueTime, int status) {
        DetailTaskDialog dialog = new DetailTaskDialog();
        Bundle bundle = new Bundle();
        bundle.putString("id", id);
        bundle.putString("task", taskName);
        bundle.putString("dueDate", dueDate);
        bundle.putString("dueTime", dueTime);
        bundle.putInt("status", status);
        dialog.setArguments(bundle);

        try {
            dialog.show(getSupportFragmentManager(), "DetailTaskDialog");
            Log.d("TaskActivity", "DetailTaskDialog shown successfully");
        } catch (Exception e) {
            Log.e("TaskActivity", "Failed to show DetailTaskDialog", e);
        }
    }
    private void filterTask(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.filterTask(mList);
            return;
        }
        List<TaskModel> filteredList = new ArrayList<>();
        for (TaskModel note : mList) {
            if (note.getTask().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(note);
            }
        }
        adapter.filterTask(filteredList);

        if (filteredList.isEmpty()) {
            Toast.makeText(TaskActivity.this, getString(R.string.no_matching_results_found_please_search_by_title), Toast.LENGTH_SHORT).show();
        }
    }


    private void showData() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        query = firestore.collection("task").document(userId)
                .collection("myTask")
                .orderBy("dueDate", Query.Direction.DESCENDING);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    mList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String id = document.getId();
                        TaskModel taskModel = document.toObject(TaskModel.class).withId(id);
                        mList.add(taskModel);
                    }
                    adapter.notifyDataSetChanged();
                    if (!mList.isEmpty()) {
                        showSwipeHintSnackbar();
                    }
                } else {
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
                swipeRefreshLayout.setRefreshing(false);
            }

        });
    }



    private void showSwipeHintSnackbar() {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                R.string.swipe_left_to_update_and_swipe_right_to_delete, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }

    @Override
    public void onDialogClose(DialogInterface dialogInterface) {
        swipeRefreshLayout.setRefreshing(true);
        showData();
        adapter.notifyDataSetChanged();

    }

    @Override
    public void onTaskClick(TaskModel taskModel) {
        DetailTaskDialog detailDialog = new DetailTaskDialog();

        Bundle bundle = new Bundle();
        bundle.putString("taskId", taskModel.TaskId);
        bundle.putString("task", taskModel.getTask());
        bundle.putString("dueDate", taskModel.getDueDate());
        bundle.putString("dueTime", taskModel.getDueTime());
        bundle.putInt("status", taskModel.getStatus());
        detailDialog.setArguments(bundle);

        detailDialog.show(getSupportFragmentManager(), "DetailTaskDialog");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    public void logAllAlarms(Context context) {
        AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(context);
        Cursor cursor = dbHelper.getAllAlarms();

        if (cursor != null && cursor.moveToFirst()) {
            Log.d("AlarmDatabase", "Listing all alarms:");
            do {
                int indexId = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_ID);
                int indexHour = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_HOUR);
                int indexMinute = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_MINUTE);
                int indexDay = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_DAY);
                int indexMonth = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_MONTH);
                int indexYear = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_YEAR);
                int indexEnabled = cursor.getColumnIndex(AlarmDatabaseHelper.COLUMN_ENABLED);

                if (indexId == -1 || indexHour == -1 || indexMinute == -1 || indexDay == -1 || indexMonth == -1 || indexYear == -1 || indexEnabled == -1) {
                    Log.e("AlarmDatabase", "Column not found in Cursor.");
                    continue;
                }

                long id = cursor.getLong(indexId);
                int hour = cursor.getInt(indexHour);
                int minute = cursor.getInt(indexMinute);
                int day = cursor.getInt(indexDay);
                int month = cursor.getInt(indexMonth);
                int year = cursor.getInt(indexYear);
                boolean enabled = cursor.getInt(indexEnabled) == 1;

                Log.d("AlarmDatabase", "Alarm ID: " + id +
                        ", Time: " + hour + ":" + minute +
                        ", Date: " + day + "/" + (month + 1) + "/" + year +
                        ", Enabled: " + enabled);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            Log.d("AlarmDatabase", "No alarms found.");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);

        swipeRefreshLayout.setRefreshing(true);
        showData();

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
    }
}

