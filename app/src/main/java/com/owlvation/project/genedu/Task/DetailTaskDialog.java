package com.owlvation.project.genedu.Task;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.R;

public class DetailTaskDialog extends DialogFragment {

    private ImageView closeButton;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_detail_task, container, false);

        TextView taskTextView = view.findViewById(R.id.task_name);
        TextView dueDateTextView = view.findViewById(R.id.task_date);
        closeButton = view.findViewById(R.id.close_dialog);
        closeButton.setOnClickListener(v -> dismiss());
        firestore = FirebaseFirestore.getInstance();
        MaterialCheckBox mCheckBox = view.findViewById(R.id.mcheckbox);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String taskId = getArguments() != null ? getArguments().getString("taskId") : null;
       mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    firestore.collection("task").document(userId)
                            .collection("myTask").document(taskId)
                            .update("status", 1);
                } else {
                    firestore.collection("task").document(userId)
                            .collection("myTask").document(taskId)
                            .update("status", 0);
                }
            }
        });


        Bundle bundle = getArguments();
        if (bundle != null) {
            String dueDate =bundle.getString("dueDate");
            String dueTime=bundle.getString("dueTime");
            taskTextView.setText(bundle.getString("task"));
            String dueText = getString(R.string.due_on) + " " + dueDate + " " + getString(R.string.at) + " " + dueTime;
            dueDateTextView.setText(dueText);

        }

        if (getDialog() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9); // 90% lebar layar
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes(params);
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {

            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9); // 90% lebar layar
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes(params);

            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

}

