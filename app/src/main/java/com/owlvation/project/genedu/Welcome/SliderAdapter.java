package com.owlvation.project.genedu.Welcome;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.ViewHolder> {
    private Context context;
    private int[] images = {
            R.drawable.slider1,
            R.drawable.slider2,
            R.drawable.slider3
    };
    private String[] headings;
    private String[] descriptions;

    public SliderAdapter(Context context) {
        this.context = context;
        initializeTextArrays();
    }

    private void initializeTextArrays() {
        headings = new String[]{
                context.getString(R.string.headings_one_slider),
                context.getString(R.string.headings_two_slider),
                context.getString(R.string.headings_three_slider)
        };

        descriptions = new String[]{
                context.getString(R.string.descriptions_one_slider),
                context.getString(R.string.descriptions_two_slider),
                context.getString(R.string.decriptions_three_slider)
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(images[position]);
        holder.headingView.setText(headings[position]);
        holder.descriptionView.setText(descriptions[position]);
    }

    @Override
    public int getItemCount() {
        return headings.length;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView headingView;
        TextView descriptionView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.slideImage);
            headingView = itemView.findViewById(R.id.slideHeading);
            descriptionView = itemView.findViewById(R.id.slideDescription);
        }
    }
}