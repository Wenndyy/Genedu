package com.owlvation.project.genedu.User;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.owlvation.project.genedu.R;

public class PrivpolActivity extends AppCompatActivity {

    ImageView icBack;
    TextView title, privpol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privpol);

        title = findViewById(R.id.textTitle);
        privpol = findViewById(R.id.textprivpol);

        icBack = findViewById(R.id.ic_back);
        icBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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