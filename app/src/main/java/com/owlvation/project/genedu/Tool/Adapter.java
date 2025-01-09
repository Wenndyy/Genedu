package com.owlvation.project.genedu.Tool;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Tool.ChatAi.ChatAiActivity;
import com.owlvation.project.genedu.Tool.CodeGenerator.CodeGeneratorActivity;
import com.owlvation.project.genedu.Tool.CodeScanner.CodeScannerActivity;
import com.owlvation.project.genedu.Tool.Compass.CompassActivity;
import com.owlvation.project.genedu.Tool.DocumentViewer.DocumentViewerActivity;
import com.owlvation.project.genedu.Tool.Stopwatch.StopwatchActivity;
import com.owlvation.project.genedu.Tool.TextRecognition.TextRecognitionActivity;

import java.util.List;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private List<Model> models;
    private Context context;

    public Adapter(List<Model> models, Context context) {
        this.models = models;
        this.context = context;
    }

    public void filterList(List<Model> filteredModels) {
        this.models = filteredModels;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tools, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Model model = models.get(position);

        holder.imageView.setImageResource(model.getImage());
        holder.title.setText(model.getTitle());
        holder.desc.setText(model.getDesc());

        holder.itemView.setOnClickListener(v -> {
            if (model.getTitle() == R.string.compass) {
                Intent moveCompass = new Intent(context, CompassActivity.class);
                context.startActivity(moveCompass);
            } else if (model.getTitle() == R.string.stopwatch) {
                Intent moveStopwatch = new Intent(context, StopwatchActivity.class);
                context.startActivity(moveStopwatch);
            } else if (model.getTitle() == R.string.text_recognition) {
                Intent moveTextRecognition = new Intent(context, TextRecognitionActivity.class);
                context.startActivity(moveTextRecognition);
            } else if (model.getTitle() == R.string.code_scanner) {
                Intent moveCodeScanner = new Intent(context, CodeScannerActivity.class);
                context.startActivity(moveCodeScanner);
            } else if (model.getTitle() == R.string.code_generator) {
                Intent moveCodeGenerator = new Intent(context, CodeGeneratorActivity.class);
                context.startActivity(moveCodeGenerator);
            } else if (model.getTitle() == R.string.document_viewer) {
                Intent movePdfViewer = new Intent(context, DocumentViewerActivity.class);
                context.startActivity(movePdfViewer);
            } else if (model.getTitle() == R.string.geneduai) {
                Intent moveChatAi = new Intent(context, ChatAiActivity.class);
                context.startActivity(moveChatAi);
            }
        });
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView title, desc;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.desc);
        }
    }
}
