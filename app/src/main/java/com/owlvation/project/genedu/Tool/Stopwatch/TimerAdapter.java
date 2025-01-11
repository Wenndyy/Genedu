package com.owlvation.project.genedu.Tool.Stopwatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class TimerAdapter extends RecyclerView.Adapter<TimerAdapter.TimerViewHolder> {

    private List<String> timerList;

    public TimerAdapter(List<String> timerList) {
        this.timerList = timerList;
    }

    @Override
    public TimerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new TimerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TimerViewHolder holder, int position) {
        holder.timerText.setText(timerList.get(position));
    }

    @Override
    public int getItemCount() {
        return timerList.size();
    }

    static class TimerViewHolder extends RecyclerView.ViewHolder {
        TextView timerText;

        public TimerViewHolder(View itemView) {
            super(itemView);
            timerText = itemView.findViewById(android.R.id.text1);
        }
    }
}