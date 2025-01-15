package com.owlvation.project.genedu.Dashboard;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.User.EditProfile;
import com.owlvation.project.genedu.User.PrivpolActivity;
import com.owlvation.project.genedu.User.TOSActivity;
import com.owlvation.project.genedu.Welcome.WelcomingScreenActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String THEME_PREFS = "theme_prefs";
    private static final String THEME_MODE = "theme_mode";
    private SharedPreferences sharedPreferences;
    private static final int MODE_SYSTEM = -1;
    private static final int MODE_LIGHT = 0;
    private static final int MODE_DARK = 1;
    private static final int EDIT_PROFILE_REQUEST = 1;
    private ProgressDialog progressDialog;
    private ConstraintLayout mainContent;
    private View view;
    private TextView userName, email, verifyMessage;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private Button btnLogout, verifyNow;
    private RelativeLayout resetPassLocal, changeProfileImage, termsOfUse, privacyPolicy, layoutLanguage;
    private FirebaseUser user;
    private CircleImageView profileImage;
    private StorageReference storageReference;
    private SwitchCompat switchTheme;
    private ImageView icTheme;
    private ListenerRegistration userListener;

    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedPreferences = requireActivity().getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE);
        networkChangeReceiver = new NetworkChangeReceiver();
        initializeViews();
        setupFirebase();
        setupListeners();
        return view;
    }

    private void initializeViews() {
        userName = view.findViewById(R.id.profileName);
        email = view.findViewById(R.id.profileEmail);
        resetPassLocal = view.findViewById(R.id.layoutAccount1);
        btnLogout = view.findViewById(R.id.btnlogout);
        verifyMessage = view.findViewById(R.id.verifyMessage);
        verifyNow = view.findViewById(R.id.verifyNow);
        profileImage = view.findViewById(R.id.profileImage);
        changeProfileImage = view.findViewById(R.id.layoutAccount);
        termsOfUse = view.findViewById(R.id.layoutSettings);
        privacyPolicy = view.findViewById(R.id.layoutSettings1);
        layoutLanguage = view.findViewById(R.id.layoutSettings3);
        switchTheme = view.findViewById(R.id.switchTheme);
        icTheme = view.findViewById(R.id.ic_theme);

        int savedThemeMode = sharedPreferences.getInt(THEME_MODE, MODE_SYSTEM);

        if (savedThemeMode == MODE_SYSTEM) {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            switchTheme.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);
        } else {
            switchTheme.setChecked(savedThemeMode == MODE_DARK);
        }

        updateThemeIcon(switchTheme.isChecked());

        mainContent = view.findViewById(R.id.layout_body);
        mainContent.setVisibility(View.INVISIBLE);

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setCancelable(false);
    }

    private void setupFirebase() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        user = fAuth.getCurrentUser();
        userId = fAuth.getCurrentUser().getUid();

        loadUserProfile();
        checkEmailVerificationStatus();
    }

    private void setupListeners() {
        layoutLanguage.setOnClickListener(v -> showLanguageSelectionDialog());

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(THEME_MODE, isChecked ? MODE_DARK : MODE_LIGHT);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            updateThemeIcon(isChecked);
        });

        termsOfUse.setOnClickListener(v -> startActivity(new Intent(getActivity(), TOSActivity.class)));

        privacyPolicy.setOnClickListener(v -> startActivity(new Intent(getActivity(), PrivpolActivity.class)));

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        verifyNow.setOnClickListener(v -> resendVerificationEmail());
        if (user != null) {
            boolean isEmailUser = false;
            boolean hasGoogleProvider = false;


            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                if (providerId.equals("password")) {
                    isEmailUser = true;
                }
                if (providerId.equals("google.com")) {
                    hasGoogleProvider = true;
                }
            }


            if (isEmailUser && !hasGoogleProvider) {
                resetPassLocal.setVisibility(View.VISIBLE);
                resetPassLocal.setOnClickListener(v -> {
                    if (user.isEmailVerified()) {
                        resetPassLocal.setEnabled(true);
                        resetPassword();
                    } else {
                        resetPassLocal.setEnabled(false);
                        Toast.makeText(getActivity(), R.string.please_verify_your_email_to_reset_your_password, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                resetPassLocal.setVisibility(View.GONE);
            }
        }

        changeProfileImage.setOnClickListener(v -> {
            if (user.isEmailVerified()) {
                changeProfileImage.setEnabled(true);
                Intent intent = new Intent(getActivity(), EditProfile.class);
                intent.putExtra("userName", userName.getText().toString());
                intent.putExtra("email", email.getText().toString());
                startActivityForResult(intent, EDIT_PROFILE_REQUEST);
            } else {
                changeProfileImage.setEnabled(false);
                Toast.makeText(getActivity(), R.string.please_verify_your_email_to_manage_your_account, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void loadUserProfile() {
        if (!isAdded() || getActivity() == null) return;

        progressDialog.show();
        mainContent.setVisibility(View.INVISIBLE);

        final int[] loadingTasks = {2};

        StorageReference profileRef = storageReference.child("users/" + userId + "/profile.jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            if (isAdded() && getActivity() != null) {
                Picasso.get().load(uri)
                        .into(profileImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                checkLoadingComplete(loadingTasks);
                            }

                            @Override
                            public void onError(Exception e) {
                                checkLoadingComplete(loadingTasks);
                            }
                        });
            }
        }).addOnFailureListener(e -> {
            checkLoadingComplete(loadingTasks);
        });
        setupRealtimeUpdates(loadingTasks);
    }

    private void setupRealtimeUpdates(final int[] loadingTasks) {
        DocumentReference documentReference = fStore.collection("users").document(userId);
        userListener = documentReference.addSnapshotListener((documentSnapshot, e) -> {
            if (!isAdded() || getActivity() == null) return;

            if (e != null) {
                Log.w("ProfileFragment", "Listen failed.", e);
                checkLoadingComplete(loadingTasks);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                userName.setText(documentSnapshot.getString("uName"));
                email.setText(documentSnapshot.getString("email"));
                checkLoadingComplete(loadingTasks);
            } else {
                checkLoadingComplete(loadingTasks);
            }
        });
    }

    private void showLanguageSelectionDialog() {
        final String[] languages = {"English", "Bahasa Indonesia"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.select_language);
        builder.setItems(languages, (dialog, which) -> {
            String selectedLanguage = languages[which];
            changeAppLanguage(selectedLanguage);
        });
        builder.show();
    }

    private void changeAppLanguage(String language) {
        if (!isAdded()) return;

        Context context = requireContext();
        Locale locale = language.equals("Bahasa Indonesia") ? new Locale("in") : new Locale("en");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());

        if (getActivity() instanceof BottomNavActivity) {
            ((BottomNavActivity) getActivity()).recreateActivity();
        }
    }

    private void updateThemeIcon(boolean isDarkMode) {
        icTheme.setImageResource(isDarkMode ?
                R.drawable.ic_night :
                R.drawable.ic_day);
    }

    private void resetPassword() {
        String emailText = email.getText().toString().trim();
        if (emailText.isEmpty()) {
            Toast.makeText(getActivity(), R.string.email_not_available_please_try_again, Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(requireContext());
        passwordResetDialog.setTitle(R.string.reset_password);
        passwordResetDialog.setMessage(getString(R.string.a_reset_link_will_be_sent_to_your_email) + emailText);

        passwordResetDialog.setPositiveButton(R.string.yes, (dialog, which) -> {
            fAuth.sendPasswordResetEmail(emailText).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getActivity(), R.string.reset_link_sent_to_your_email, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), R.string.failed_reset_link_is_not_sent + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        passwordResetDialog.setNegativeButton(R.string.no, (dialog, which) -> {
        });
        passwordResetDialog.create().show();
    }

    private void checkEmailVerificationStatus() {
        user.reload().addOnCompleteListener(task -> {
            if (user.isEmailVerified()) {
                verifyMessage.setVisibility(View.GONE);
                verifyNow.setVisibility(View.GONE);
            } else {
                verifyMessage.setVisibility(View.VISIBLE);
                verifyNow.setVisibility(View.VISIBLE);
            }
        });
    }

    private void resendVerificationEmail() {
        if (user != null) {
            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            long currentTimestamp = System.currentTimeMillis();
            long cooldownPeriod = 60 * 1000;

            if (currentTimestamp - creationTimestamp > cooldownPeriod) {
                user.sendEmailVerification()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getActivity(), R.string.verification_has_been_sent_please_check_your_email, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getActivity(), R.string.verification_failed_please_try_again_later, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(getString(R.string.logout));
        builder.setMessage(R.string.logout_confirmation);
        builder.setPositiveButton(R.string.yes, (dialog, which) -> logout());
        builder.setNegativeButton(R.string.no, (dialog, which) -> {
        });
        builder.create().show();
    }

    private void logout() {
        fAuth.signOut();

        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(getActivity());
        if (googleSignInAccount != null) {
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(),
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build());

            googleSignInClient.signOut().addOnCompleteListener(task -> {
                redirectToWelcomingScreen();
            });
        } else {
            redirectToWelcomingScreen();
        }
    }

    private void redirectToWelcomingScreen() {
        Intent intent = new Intent(getActivity(), WelcomingScreenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        requireActivity().unregisterReceiver(networkChangeReceiver);
    }

    private void checkLoadingComplete(int[] loadingTasks) {
        loadingTasks[0]--;
        if (loadingTasks[0] <= 0) {
            if (isAdded() && getActivity() != null) {
                mainContent.setVisibility(View.VISIBLE);
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }

        if (profileImage != null) {
            Picasso.get().cancelRequest(profileImage);
        }

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        view = null;
        userName = null;
        email = null;
        profileImage = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == getActivity().RESULT_OK) {
            requireActivity().recreate();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        requireActivity().registerReceiver(networkChangeReceiver, intentFilter);
    }


}