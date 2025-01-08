package com.owlvation.project.genedu.User;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.owlvation.project.genedu.R;

public class TOSActivity extends AppCompatActivity {

    ImageView icBack;
    TextView title, tos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tos);

        title = findViewById(R.id.textTitle);
        tos = findViewById(R.id.textTos);
        icBack = findViewById(R.id.ic_back);

        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}