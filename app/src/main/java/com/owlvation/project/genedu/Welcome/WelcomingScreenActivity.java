package com.owlvation.project.genedu.Welcome;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.User.Login;
import com.owlvation.project.genedu.User.Register;

public class WelcomingScreenActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private SliderAdapter sliderAdapter;
    private Button loginButton, registerButton;
    private ImageView[] dots;
    private Handler sliderHandler = new Handler();
    private Runnable sliderRunnable;
    private static final long SLIDER_DELAY = 3000;
    private boolean isLoginSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcoming_screen);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        sliderAdapter = new SliderAdapter(this);
        viewPager.setAdapter(sliderAdapter);

        setUpDots(3);
        setupAutoSlider();
        enableManualSliding();

        updateButtonStates(isLoginSelected);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                selectedDot(position);
                super.onPageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    stopAutoSliding();
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    startAutoSliding();
                }
            }
        });

        playAnimation();
        loginButton.setOnClickListener(v -> {
            isLoginSelected = true;
            updateButtonStates(isLoginSelected);
            startActivity(new Intent(WelcomingScreenActivity.this, Login.class));
        });

        registerButton.setOnClickListener(v -> {
            isLoginSelected = false;
            updateButtonStates(isLoginSelected);
            startActivity(new Intent(WelcomingScreenActivity.this, Register.class));
        });
    }

    private void playAnimation() {
        final long ANIM_DURATION = 800;

        ObjectAnimator registerFade = ObjectAnimator.ofFloat(
                registerButton,
                "alpha",
                0f,
                1f
        );
        registerFade.setDuration(ANIM_DURATION);

        ObjectAnimator registerSlide = ObjectAnimator.ofFloat(
                registerButton,
                "translationY",
                -50f,
                0f
        );
        registerSlide.setDuration(ANIM_DURATION);

        ObjectAnimator loginFade = ObjectAnimator.ofFloat(
                loginButton,
                "alpha",
                0f,
                1f
        );
        loginFade.setDuration(ANIM_DURATION);

        ObjectAnimator loginSlide = ObjectAnimator.ofFloat(
                loginButton,
                "translationY",
                -50f,
                0f
        );
        loginSlide.setDuration(ANIM_DURATION);

        AnimatorSet registerAnimSet = new AnimatorSet();
        registerAnimSet.playTogether(registerFade, registerSlide);

        AnimatorSet loginAnimSet = new AnimatorSet();
        loginAnimSet.playTogether(loginFade, loginSlide);

        AnimatorSet finalAnimSet = new AnimatorSet();
        finalAnimSet.playSequentially(registerAnimSet, loginAnimSet);
        finalAnimSet.start();
    }


    private void updateButtonStates(boolean isLoginSelected) {
        if (isLoginSelected) {
            loginButton.setBackground(getResources().getDrawable(R.drawable.background_button_filled));
            loginButton.setTextColor(getResources().getColor(android.R.color.white));
            registerButton.setBackground(getResources().getDrawable(R.drawable.background_button_outlined));
            registerButton.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            registerButton.setBackground(getResources().getDrawable(R.drawable.background_button_filled));
            registerButton.setTextColor(getResources().getColor(android.R.color.white));
            loginButton.setBackground(getResources().getDrawable(R.drawable.background_button_outlined));
            loginButton.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private void setupAutoSlider() {
        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = viewPager.getCurrentItem();
                if (currentItem < sliderAdapter.getItemCount() - 1) {
                    viewPager.setCurrentItem(currentItem + 1);
                } else {
                    viewPager.setCurrentItem(0);
                }
            }
        };
        startAutoSliding();
    }

    private void startAutoSliding() {
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, SLIDER_DELAY);
    }

    private void stopAutoSliding() {
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    private void enableManualSliding() {
        viewPager.setUserInputEnabled(true);
    }

    private void setUpDots(int count) {
        dots = new ImageView[count];
        dotsLayout.removeAllViews();

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(R.drawable.inactive_dot);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dots[i], params);
        }

        if (dots.length > 0) {
            dots[0].setImageResource(R.drawable.active_dot);
        }
    }

    private void selectedDot(int position) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageResource(i == position ? R.drawable.active_dot : R.drawable.inactive_dot);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoSliding();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAutoSliding();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoSliding();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(this)
                .setMessage(R.string.exit_confirmation)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishAffinity();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }
}