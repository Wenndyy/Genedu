package com.owlvation.project.genedu.Welcome;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.owlvation.project.genedu.Dashboard.BottomNavActivity;
import com.owlvation.project.genedu.R;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("theme_prefs", MODE_PRIVATE);
        int savedThemeMode = sharedPreferences.getInt("theme_mode", -1);

        if (savedThemeMode == -1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    savedThemeMode == 1 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        final ImageView imageApp = findViewById(R.id.imageApp);
        final TextView textView = findViewById(R.id.textView);

        imageApp.startAnimation(fadeInAnimation);
        textView.startAnimation(fadeInAnimation);

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                startActivity(new Intent(SplashActivity.this, BottomNavActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, WelcomingScreenActivity.class));
            }
            finish();
        }, SPLASH_DURATION);
    }
}
