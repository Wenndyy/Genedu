package com.owlvation.project.genedu.Dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.owlvation.project.genedu.R;


public class BottomNavActivity extends AppCompatActivity {
    private static final String CURRENT_NAV_ITEM_KEY = "current_nav_item";
    private int currentNavItem = R.id.bottom_home;
    private boolean isRecreating = false;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_bottom_nav);

        initializeViews();
        setupNavigation(savedInstanceState);
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void setupNavigation(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            currentNavItem = savedInstanceState.getInt(CURRENT_NAV_ITEM_KEY, R.id.bottom_home);
            loadFragmentForNavItem(currentNavItem);
        } else {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setSelectedItemId(currentNavItem);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (!isRecreating) {
                currentNavItem = item.getItemId();
                loadFragmentForNavItem(currentNavItem);
            }
            return true;
        });
    }

    private void loadFragmentForNavItem(int itemId) {
        Fragment selectedFragment = null;

        if (itemId == R.id.bottom_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.bottom_management) {
            selectedFragment = new ManagementFragment();
        } else if (itemId == R.id.bottom_tools) {
            selectedFragment = new ToolsFragment();
        } else if (itemId == R.id.bottom_profile) {
            selectedFragment = new ProfileFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_NAV_ITEM_KEY, currentNavItem);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(currentNavItem);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecreating = false;
    }

    public void recreateActivity() {
        isRecreating = true;
        recreate();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        showExitConfirmationDialog();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.exit))
                .setMessage(getString(R.string.exit_confirmation))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    dialog.dismiss();
                    super.finishAffinity();
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}