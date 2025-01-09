package com.owlvation.project.genedu.Dashboard;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.owlvation.project.genedu.Mimo.MindfulMomentsActivity;
import com.owlvation.project.genedu.Note.NoteActivity;
import com.owlvation.project.genedu.Note.NoteModel;
import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.TaskModel;
import com.owlvation.project.genedu.Task.TaskActivity;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ManagementFragment extends Fragment {
    private CardView cvTask, cvNote, cvMimo;
    private MaterialCalendarView calendarView;
    private Spinner calendarDropdown;
    private FirebaseFirestore firestore;
    private String userId;
    private Map<String, List<TaskModel>> tasksByDate;
    private Map<String, List<NoteModel>> notesByDate;
    private static final String DATE_FORMAT = "d/M/yyyy";
    private SimpleDateFormat dateFormatter;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_management, container, false);

        initializeViews();
        initializeFirebase();
        setupDropdownAndCalendar();
        setupClickListeners();
        setupSearchView();
        loadTasksFromFirebase();

        return rootView;
    }

    private void initializeViews() {
        cvTask = rootView.findViewById(R.id.cvTask);
        cvNote = rootView.findViewById(R.id.cvNote);
        cvMimo = rootView.findViewById(R.id.cvMimo);
        calendarView = rootView.findViewById(R.id.calendarView);
        calendarDropdown = rootView.findViewById(R.id.calendarDropdown);
    }

    private void initializeFirebase() {
        firestore = FirebaseFirestore.getInstance();
        userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        tasksByDate = new HashMap<>();
        notesByDate = new HashMap<>();
        dateFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
    }

    private void setupDropdownAndCalendar() {
        String[] items = new String[]{getString(R.string.daftar_tugas), getString(R.string.daftar_notes)};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                requireContext(),
                R.layout.spinner_item,
                R.id.spinnerText,
                items
        ) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = view.findViewById(R.id.spinnerText);

                if (position == calendarDropdown.getSelectedItemPosition()) {
                    tv.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    tv.setTextColor(Color.WHITE);
                } else {
                    tv.setBackgroundColor(Color.TRANSPARENT);
                    tv.setTextColor(getResources().getColor(R.color.colorPrimary));
                }

                return view;
            }
        };

        calendarDropdown.setAdapter(spinnerAdapter);
        calendarDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    loadTasksFromFirebase();
                } else {
                    tasksByDate.clear();
                    loadNotesFromFirebase();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                loadTasksFromFirebase();
            }
        });

        setupCalendarView();
    }

    private void setupCalendarView() {
        Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        CalendarDay today = CalendarDay.from(year, month, day);
        calendarView.setCurrentDate(today);
        calendarView.setSelectedDate(today);

        calendarView.addDecorator(new DayViewDecorator() {
            private final CalendarDay today = CalendarDay.today();

            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.equals(today);
            }

            @Override
            public void decorate(DayViewFacade view) {
                if (calendarView.getSelectedDate() != null &&
                        calendarView.getSelectedDate().equals(today)) {
                    int nightModeFlags = requireContext().getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;

                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                        view.addSpan(new ForegroundColorSpan(Color.BLACK));
                    } else {
                        view.addSpan(new ForegroundColorSpan(Color.WHITE));
                    }
                } else {
                    view.addSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)));
                }
            }
        });

        calendarView.addDecorator(new DayViewDecorator() {
            private CalendarDay currentMonth = calendarView.getCurrentDate();

            @Override
            public boolean shouldDecorate(CalendarDay day) {
                return day.getMonth() == currentMonth.getMonth()
                        && day.getYear() == currentMonth.getYear()
                        && !day.equals(CalendarDay.today());
            }

            @Override
            public void decorate(DayViewFacade view) {
                int textColor;
                int nightModeFlags = requireContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;

                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    textColor = Color.WHITE;
                } else {
                    textColor = Color.BLACK;
                }

                view.addSpan(new ForegroundColorSpan(textColor));
            }
        });

        calendarView.addDecorator(new DayViewDecorator() {
            @Override
            public boolean shouldDecorate(CalendarDay day) {
                Calendar cal = Calendar.getInstance();
                cal.set(day.getYear(), day.getMonth() - 1, day.getDay());
                String dateString = dateFormatter.format(cal.getTime());

                return calendarDropdown.getSelectedItemPosition() == 0 ?
                        tasksByDate.containsKey(dateString) && !tasksByDate.get(dateString).isEmpty() :
                        notesByDate.containsKey(dateString) && !notesByDate.get(dateString).isEmpty();
            }

            @Override
            public void decorate(DayViewFacade view) {
                view.addSpan(new DotSpan(8, getResources().getColor(R.color.colorAccent)));
            }
        });

        calendarView.setOnMonthChangedListener((widget, date) -> {
            calendarView.removeDecorators();

            calendarView.addDecorator(new DayViewDecorator() {
                private final CalendarDay today = CalendarDay.today();

                @Override
                public boolean shouldDecorate(CalendarDay day) {
                    return day.equals(today);
                }

                @Override
                public void decorate(DayViewFacade view) {
                    if (calendarView.getSelectedDate() != null &&
                            calendarView.getSelectedDate().equals(today)) {
                        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode &
                                Configuration.UI_MODE_NIGHT_MASK;

                        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                            view.addSpan(new ForegroundColorSpan(Color.BLACK));
                        } else {
                            view.addSpan(new ForegroundColorSpan(Color.WHITE));
                        }
                    } else {
                        view.addSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)));
                    }
                }
            });

            calendarView.addDecorator(new DayViewDecorator() {
                @Override
                public boolean shouldDecorate(CalendarDay day) {
                    return day.getMonth() == date.getMonth()
                            && day.getYear() == date.getYear()
                            && !day.equals(CalendarDay.today());
                }

                @Override
                public void decorate(DayViewFacade view) {
                    int textColor;
                    int nightModeFlags = requireContext().getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;

                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                        textColor = Color.WHITE;
                    } else {
                        textColor = Color.BLACK;
                    }

                    view.addSpan(new ForegroundColorSpan(textColor));
                }
            });

            calendarView.addDecorator(new DayViewDecorator() {
                @Override
                public boolean shouldDecorate(CalendarDay day) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(day.getYear(), day.getMonth() - 1, day.getDay());
                    String dateString = dateFormatter.format(cal.getTime());

                    return calendarDropdown.getSelectedItemPosition() == 0 ?
                            tasksByDate.containsKey(dateString) && !tasksByDate.get(dateString).isEmpty() :
                            notesByDate.containsKey(dateString) && !notesByDate.get(dateString).isEmpty();
                }

                @Override
                public void decorate(DayViewFacade view) {
                    view.addSpan(new DotSpan(8, getResources().getColor(R.color.colorAccent)));
                }
            });
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            calendarView.invalidateDecorators();

            Calendar cal = Calendar.getInstance();
            cal.set(date.getYear(), date.getMonth() - 1, date.getDay());
            String dateString = dateFormatter.format(cal.getTime());

            if (calendarDropdown.getSelectedItemPosition() == 0) {
                showTasksForDate(dateString);
            } else {
                showNotesForDate(dateString);
            }
        });

        calendarView.setHeaderTextAppearance(R.style.CustomHeaderTextAppearance);
        calendarView.setWeekDayTextAppearance(R.style.CustomWeekDayAppearance);
        calendarView.setDateTextAppearance(R.style.CustomDateTextAppearance);
    }

    private void refreshCalendarMarkers() {
        if (calendarView != null) {
            calendarView.invalidateDecorators();
        }
    }

    private void loadTasksFromFirebase() {
        firestore.collection("task").document(userId)
                .collection("myTask")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && getActivity() != null) {
                        tasksByDate.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            TaskModel taskModel = document.toObject(TaskModel.class)
                                    .withId(document.getId());
                            String dueDate = taskModel.getDueDate();
                            try {
                                Date date = dateFormatter.parse(dueDate);
                                if (date != null) {
                                    dueDate = dateFormatter.format(date);
                                    if (!tasksByDate.containsKey(dueDate)) {
                                        tasksByDate.put(dueDate, new ArrayList<>());
                                    }
                                    tasksByDate.get(dueDate).add(taskModel);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                        refreshCalendarMarkers();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.error_loading_tasks) + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadNotesFromFirebase() {
        firestore.collection("notes").document(userId)
                .collection("myNotes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && getActivity() != null) {
                        notesByDate.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NoteModel noteModel = document.toObject(NoteModel.class);
                            noteModel.setDocumentId(document.getId());
                            Date timestamp = noteModel.getTimestamp();
                            if (timestamp != null) {
                                String dateStr = dateFormatter.format(timestamp);
                                if (!notesByDate.containsKey(dateStr)) {
                                    notesByDate.put(dateStr, new ArrayList<>());
                                }
                                notesByDate.get(dateStr).add(noteModel);
                            }
                        }
                        refreshCalendarMarkers();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(),
                                getString(R.string.error_loading_tasks) + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTasksForDate(String dateString) {
        List<TaskModel> tasksForDate = tasksByDate.get(dateString);

        if (tasksForDate != null && !tasksForDate.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_notes_header, null);

            TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
            tvDialogTitle.setText(getString(R.string.tasks_for_date, dateString));

            ListView listView = dialogView.findViewById(R.id.listView);
            TasksAdapter adapter = new TasksAdapter(requireContext(), tasksForDate);
            listView.setAdapter(adapter);

            listView.setDivider(new ColorDrawable(requireContext().getColor(R.color.grey)));
            listView.setDividerHeight(2);

            listView.setPadding(8, 8, 8, 8);
            listView.setOnItemClickListener((parent, view, position, id) -> {
                TaskModel selectedTask = tasksForDate.get(position);
                Intent intent = new Intent(getActivity(), TaskActivity.class);
                intent.putExtra("taskId", selectedTask.TaskId);
                startActivity(intent);
            });

            builder.setView(dialogView);
            ImageView closeDialog = dialogView.findViewById(R.id.close_dialog);

            AlertDialog alertDialog = builder.create();

            closeDialog.setOnClickListener(v -> alertDialog.dismiss());

            alertDialog.show();
        } else {
            Toast.makeText(requireContext(),
                    R.string.no_tasks_for_date,
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void showNotesForDate(String dateString) {
        List<NoteModel> notesForDate = notesByDate.get(dateString);
        if (notesForDate != null && !notesForDate.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_notes_header, null);

            TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
            tvDialogTitle.setText(getString(R.string.notes_for_date, dateString));

            ListView listView = dialogView.findViewById(R.id.listView);
            NotesAdapter adapter = new NotesAdapter(requireContext(), notesForDate);
            listView.setAdapter(adapter);

            listView.setDivider(new ColorDrawable(requireContext().getColor(R.color.grey)));
            listView.setDividerHeight(2);
            listView.setPadding(8, 8, 8, 8);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                NoteModel selectedNote = notesForDate.get(position);
                showNoteDetail(selectedNote);
            });

            builder.setView(dialogView);
            ImageView closeDialog = dialogView.findViewById(R.id.close_dialog);

            AlertDialog alertDialog = builder.create();

            closeDialog.setOnClickListener(v -> alertDialog.dismiss());

            alertDialog.show();
        } else {
            Toast.makeText(requireContext(),
                    R.string.no_notes_for_date,
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void showNoteDetail(NoteModel note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_note_detail, null);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        TextView contentTextView = dialogView.findViewById(R.id.contentTextView);
        ImageView imageView = dialogView.findViewById(R.id.imageView);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String formattedDate = sdf.format(note.getTimestamp());

        titleTextView.setText(getString(R.string.title) + note.getTitle());
        contentTextView.setText(getString(R.string.content) + "\n" + note.getContent() +
                "\n\n" + getString(R.string.created_on) + formattedDate);

        if (!TextUtils.isEmpty(note.getImageUrl())) {
            Glide.with(requireContext())
                    .load(note.getImageUrl())
                    .into(imageView);
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.GONE);
        }

        builder.setView(dialogView);


        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            alertDialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        });

        ImageView closeDialog = dialogView.findViewById(R.id.close_dialog);
        closeDialog.setOnClickListener(v -> {
            alertDialog.dismiss();
        });
        alertDialog.show();
    }

    private void setupClickListeners() {
        cvTask.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TaskActivity.class);
            startActivity(intent);
        });

        cvNote.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NoteActivity.class);
            startActivity(intent);
        });

        cvMimo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MindfulMomentsActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearchView() {
        SearchView searchView = rootView.findViewById(R.id.searchManagement);
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

    private void filter(String query) {
        String taskTitle = getString(R.string.main_task);
        String noteTitle = getString(R.string.main_note);
        String mimoTitle = getString(R.string.main_mimo);

        if (taskTitle.toLowerCase().contains(query.toLowerCase())) {
            cvTask.setVisibility(View.VISIBLE);
        } else {
            cvTask.setVisibility(View.GONE);
        }

        if (noteTitle.toLowerCase().contains(query.toLowerCase())) {
            cvNote.setVisibility(View.VISIBLE);
        } else {
            cvNote.setVisibility(View.GONE);
        }

        if (mimoTitle.toLowerCase().contains(query.toLowerCase())) {
            cvMimo.setVisibility(View.VISIBLE);
        } else {
            cvMimo.setVisibility(View.GONE);
        }

        if (cvTask.getVisibility() == View.GONE &&
                cvNote.getVisibility() == View.GONE &&
                cvMimo.getVisibility() == View.GONE &&
                isAdded()) {
            Toast.makeText(getActivity(),
                    R.string.no_matching_results_found_please_search_by_title,
                    Toast.LENGTH_SHORT).show();
        }
    }
}