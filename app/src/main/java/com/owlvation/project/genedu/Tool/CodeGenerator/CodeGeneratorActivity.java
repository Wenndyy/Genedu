package com.owlvation.project.genedu.Tool.CodeGenerator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.owlvation.project.genedu.R;


public class CodeGeneratorActivity extends AppCompatActivity {

    private LinearLayout barCode, qrCode;
    private ImageView icBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_generator);
        icBack = findViewById(R.id.ic_back);

        qrCode = findViewById(R.id.qrCode);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CodeGeneratorActivity.this, QrGenerator.class);
                startActivity(intent);
            }
        });

        barCode = findViewById(R.id.barCode);
        barCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CodeGeneratorActivity.this, BarGenerator.class);
                startActivity(intent);
            }
        });

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