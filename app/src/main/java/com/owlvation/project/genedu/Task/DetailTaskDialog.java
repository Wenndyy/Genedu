package com.owlvation.project.genedu.Task;


import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.R;

public class DetailTaskDialog extends DialogFragment {
    private ImageView closeButton;
    private static final String TAG = "DetailTaskDialog";
    private FirebaseFirestore firestore;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
    }

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

        Bundle bundle = getArguments();
        if (bundle != null) {
            String id = bundle.getString("taskId");
            String id2 = bundle.getString("id");
            String dueDate = bundle.getString("dueDate");
            String dueTime = bundle.getString("dueTime");
            taskTextView.setText(bundle.getString("task"));
            String dueText = " " + dueDate + " " + dueTime;
            int status = bundle.getInt("status");
            dueDateTextView.setText(dueText);
            mCheckBox.setChecked(toBoolean(status));
            mCheckBox.setOnCheckedChangeListener(null);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String iduser = currentUser.getUid();


            mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    String taskId = (id == null || id.isEmpty()) ? id2 : id;
                    firestore.collection("task").document(iduser)
                            .collection("myTask").document(taskId)
                            .update("status", isChecked ? 1 : 0)
                            .addOnSuccessListener(aVoid -> {
                                mCheckBox.setChecked(isChecked);
                                if (getActivity() instanceof OnDialogCloseListner) {
                                    ((OnDialogCloseListner) getActivity()).onDialogClose(null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                mCheckBox.setChecked(!isChecked);
                            });
                }
            });
        }

        if (getDialog() != null) {
            WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
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
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getDialog().getWindow().setAttributes(params);

        }
    }

    private boolean toBoolean(int status) {
        return status != 0;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
    }
}

