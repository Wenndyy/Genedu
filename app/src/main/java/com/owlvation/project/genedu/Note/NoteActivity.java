package com.owlvation.project.genedu.Note;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.owlvation.project.genedu.R;

import java.util.ArrayList;
import java.util.List;

public class NoteActivity extends AppCompatActivity {

    FloatingActionButton mCreateNoteFab;
    ImageView icBack;
    RecyclerView recyclerView;
    NoteAdapter adapter;
    List<NoteModel> noteList;

    FirebaseFirestore firebaseFirestore;
    FirebaseUser firebaseUser;

    SwipeRefreshLayout swipeRefreshLayout;

    SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        searchView = findViewById(R.id.searchNote);
        icBack = findViewById(R.id.ic_back);
        mCreateNoteFab = findViewById(R.id.createFabNote);
        recyclerView = findViewById(R.id.recycerlviewNote);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        noteList = new ArrayList<>();
        adapter = new NoteAdapter(this, noteList, firebaseFirestore, firebaseUser);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });

        icBack.setOnClickListener(v -> onBackPressed());

        mCreateNoteFab.setOnClickListener(v -> {
            startActivity(new Intent(NoteActivity.this, CreateNoteActivity.class));
            finish();
        });

        swipeRefreshLayout.setOnRefreshListener(this::fetchNotes);

        fetchNotes();
    }

    private void filterNotes(String query) {
        if (adapter == null) {
            return;
        }

        List<NoteModel> filteredList = new ArrayList<>();
        if (!query.isEmpty()) {
            for (NoteModel note : noteList) {
                if (note.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(note);
                }
            }
        } else {
            filteredList.addAll(noteList);
        }

        adapter.filterList(filteredList);

        if (!query.isEmpty() && filteredList.isEmpty()) {
            Toast.makeText(NoteActivity.this, R.string.no_matching_results_found_please_search_by_title, Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNotes() {
        noteList.clear();
        swipeRefreshLayout.setRefreshing(true);

        firebaseFirestore.collection("notes")
                .document(firebaseUser.getUid())
                .collection("myNotes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        NoteModel note = documentSnapshot.toObject(NoteModel.class);
                        note.setDocumentId(documentSnapshot.getId());
                        noteList.add(note);
                    }

                    if (adapter == null) {
                        adapter = new NoteAdapter(this, noteList, firebaseFirestore, firebaseUser);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    searchView.setEnabled(true);
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("notes", new ArrayList<>(noteList));
        outState.putString("searchQuery", searchView.getQuery().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<NoteModel> savedNotes = savedInstanceState.getParcelableArrayList("notes");
        if (savedNotes != null) {
            noteList.clear();
            noteList.addAll(savedNotes);
            adapter = new NoteAdapter(this, noteList, firebaseFirestore, firebaseUser);
            recyclerView.setAdapter(adapter);
        } else {
            fetchNotes();
        }

        String savedQuery = savedInstanceState.getString("searchQuery", "");
        searchView.setQuery(savedQuery, false);
        if (!savedQuery.isEmpty()) {
            filterNotes(savedQuery);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}