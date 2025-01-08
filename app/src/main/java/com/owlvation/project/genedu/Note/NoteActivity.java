package com.owlvation.project.genedu.Note;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
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
    OnQueryTextListener queryTextListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        searchView = findViewById(R.id.searchNote);
        queryTextListener = new OnQueryTextListener() {
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
        };

        searchView.setOnQueryTextListener(queryTextListener);

        icBack = findViewById(R.id.ic_back);
        mCreateNoteFab = findViewById(R.id.createFabNote);
        recyclerView = findViewById(R.id.recycerlviewNote);
        recyclerView.setHasFixedSize(true);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        noteList = new ArrayList<>();

        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mCreateNoteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NoteActivity.this, CreateNoteActivity.class));
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchNotes();
            }
        });

        fetchNotes();
    }

    private void filterNotes(String query) {
        List<NoteModel> filteredList = new ArrayList<>();
        for (NoteModel note : noteList) {
            if (note.getTitle().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(note);
            }
        }
        adapter.filterList(filteredList);

        if (filteredList.isEmpty()) {
            Toast.makeText(NoteActivity.this, R.string.no_matching_results_found_please_search_by_title, Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNotes() {
        noteList.clear();

        firebaseFirestore.collection("notes")
                .document(firebaseUser.getUid())
                .collection("myNotes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            NoteModel note = documentSnapshot.toObject(NoteModel.class);
                            note.setDocumentId(documentSnapshot.getId());
                            noteList.add(note);
                        }

                        if (adapter == null) {
                            adapter = new NoteAdapter(NoteActivity.this, noteList, firebaseFirestore, firebaseUser);
                            recyclerView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }

                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("NoteActivity", "Error fetching notes: " + e.getMessage());

                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
