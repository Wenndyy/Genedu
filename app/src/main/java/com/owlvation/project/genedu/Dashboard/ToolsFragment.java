package com.owlvation.project.genedu.Dashboard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Tool.Adapter;
import com.owlvation.project.genedu.Tool.Model;

import java.util.ArrayList;
import java.util.List;

public class ToolsFragment extends Fragment {
    private RecyclerView recyclerView;
    private Adapter adapter;
    private List<Model> models;
    private List<Model> filteredList;
    private SearchView searchView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);

        searchView = view.findViewById(R.id.searchTool);
        recyclerView = view.findViewById(R.id.recyclerViewTools);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        models = new ArrayList<>();
        models.add(new Model(R.drawable.ic_compas, R.string.compass, R.string.title_compass_act));
        models.add(new Model(R.drawable.ic_stopwatch, R.string.stopwatch, R.string.title_stopwatch_act));
        models.add(new Model(R.drawable.ic_tr, R.string.text_recognition, R.string.title_text_recognition));
        models.add(new Model(R.drawable.ic_scannercode, R.string.code_scanner, R.string.title_scanner_code));
        models.add(new Model(R.drawable.ic_generator, R.string.code_generator, R.string.title_generator_code));
        models.add(new Model(R.drawable.ic_pdf, R.string.pdf_viewer, R.string.title_pdf_viewer));
        models.add(new Model(R.drawable.ic_ai_bot, R.string.geneduai, R.string.geduai_desc));
        adapter = new Adapter(models, requireContext());

        recyclerView.setAdapter(adapter);
        recyclerView.setPadding(24, 0, 24, 0);

        filteredList = new ArrayList<>(models);

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

        return view;
    }

    private void filter(String text) {
        filteredList.clear();
        if (TextUtils.isEmpty(text)) {
            filteredList.addAll(models);
        } else {
            String query = text.toLowerCase().trim();
            for (Model model : models) {
                if (getString(model.getTitle()).toLowerCase().contains(query) ||
                        getString(model.getDesc()).toLowerCase().contains(query)) {
                    filteredList.add(model);
                }
            }
        }
        adapter.filterList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
