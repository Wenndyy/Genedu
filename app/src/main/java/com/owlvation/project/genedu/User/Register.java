package com.owlvation.project.genedu.User;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.Dashboard.BottomNavActivity;
import com.owlvation.project.genedu.Network.NetworkChangeReceiver;
import com.owlvation.project.genedu.R;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    public static final String TAG = "TAG";
    private EditText mUserName, mEmail, mPassword, mConfirmPassword;
    private ImageView mPasswordToggle, mConfirmPasswordToggle;
    boolean isPasswordVisible = false;
    Button mRegisterBtn;
    TextView mLoginBtn;
    FirebaseAuth fAuth;
    ProgressBar progressBar;
    FirebaseFirestore fStore;
    String userID;
    Button signInGoogle;
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupTextWatchers();
        setupPasswordToggles();
        setupFirebase();
        setupRegisterButton();
        setupLoginButton();
    }

    private void initializeViews() {
        mUserName = findViewById(R.id.userName);
        mEmail = findViewById(R.id.Email);
        mPassword = findViewById(R.id.password);
        mConfirmPassword = findViewById(R.id.confirmPassword);
        mPasswordToggle = findViewById(R.id.passwordToggle);
        mConfirmPasswordToggle = findViewById(R.id.passwordConfirmToggle);
        mRegisterBtn = findViewById(R.id.registerBtn);
        mLoginBtn = findViewById(R.id.createText);
        progressBar = findViewById(R.id.progressBar);
        networkChangeReceiver = new NetworkChangeReceiver();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInGoogle = findViewById(R.id.btn_sign_in_google);

        signInGoogle = findViewById(R.id.btn_sign_in_google);
        signInGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    private void signInWithGoogle() {
        signInGoogle.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, getString(R.string.google_sign_in_failed) + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        fAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = fAuth.getCurrentUser();
                            if (user != null) {
                                FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (!document.exists()) {
                                                        user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(Register.this, R.string.verification_email_has_been_sent, Toast.LENGTH_SHORT).show();
                                                            }
                                                        }).addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.d(TAG, "onFailure: Email not sent " + e.getMessage());
                                                            }
                                                        });
                                                        Map<String, Object> userData = new HashMap<>();
                                                        userData.put("uName", user.getDisplayName());
                                                        userData.put("email", user.getEmail());


                                                        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                                                                .set(userData)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    progressBar.setVisibility(View.GONE);
                                                                    saveLoginStatus(true);
                                                                    Toast.makeText(Register.this, R.string.user_created, Toast.LENGTH_SHORT).show();
                                                                    startActivity(new Intent(getApplicationContext(), BottomNavActivity.class));
                                                                    finish();
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    progressBar.setVisibility(View.GONE);
                                                                    Toast.makeText(Register.this, R.string.failed_the_registered_account_already_exists, Toast.LENGTH_SHORT).show();
                                                                });
                                                    } else {
                                                        progressBar.setVisibility(View.GONE);
                                                        saveLoginStatus(true);
                                                        Toast.makeText(Register.this, R.string.logged_in_successfully, Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(getApplicationContext(), BottomNavActivity.class));
                                                        finish();
                                                    }
                                                } else {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(Register.this, R.string.failed_the_registered_account_already_exists, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Register.this, R.string.failed_the_registered_account_already_exists, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveLoginStatus(boolean isLoggedIn) {
        SharedPreferences sharedPreferences = getSharedPreferences("login_status", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("is_logged_in", isLoggedIn);
        editor.apply();
    }

    private void setupTextWatchers() {
        mUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateUsername(s.toString());
            }
        });

        mEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });

        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validatePassword(s.toString(), mUserName.getText().toString());
                validateConfirmPassword(mConfirmPassword.getText().toString(), s.toString());
            }
        });

        mConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateConfirmPassword(s.toString(), mPassword.getText().toString());
            }
        });
    }

    private void setupPasswordToggles() {
        mPasswordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(mPassword, mPasswordToggle);
            }
        });

        mConfirmPasswordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(mConfirmPassword, mConfirmPasswordToggle);
            }
        });
    }

    private void togglePasswordVisibility(EditText passwordField, ImageView toggleButton) {
        if (isPasswordVisible) {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
            toggleButton.setColorFilter(getResources().getColor(R.color.colorAccent));
        } else {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_on);
            toggleButton.setColorFilter(getResources().getColor(R.color.colorPrimary));
        }
        passwordField.setTypeface(ResourcesCompat.getFont(Register.this, R.font.ubuntu_regular));
        passwordField.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        passwordField.setSelection(passwordField.length());
        isPasswordVisible = !isPasswordVisible;
    }

    private void setupFirebase() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    private boolean validateUsername(String username) {
        if (username.isEmpty()) {
            mUserName.setError(getString(R.string.username_is_required));
            return false;
        } else if (!username.matches("[a-zA-Z0-9]+")) {
            mUserName.setError(getString(R.string.username_can_only_contain_letters_and_numbers));
            return false;
        } else if (username.length() < 3) {
            mUserName.setError(getString(R.string.username_must_be_at_least_3_characters));
            return false;
        } else if (username.length() > 16) {
            mUserName.setError(getString(R.string.username_must_be_less_than_or_equal_to_16_characters));
            return false;
        }
        mUserName.setError(null);
        return true;
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            mEmail.setError(getString(R.string.email_is_required));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmail.setError(getString(R.string.invalid_email_address));
            return false;
        }
        mEmail.setError(null);
        return true;
    }

    private boolean validatePassword(String password, String username) {
        if (TextUtils.isEmpty(password)) {
            mPassword.setError(getString(R.string.password_is_required));
            return false;
        } else if (password.length() < 8) {
            mPassword.setError(getString(R.string.password_must_be_more_than_equal_8_character));
            return false;
        } else if (password.equals(username)) {
            mPassword.setError(getString(R.string.password_cannot_be_the_same_as_username));
            return false;
        } else if (!password.matches(".*[a-zA-Z].*")) {
            mPassword.setError(getString(R.string.password_must_contain_at_least_one_character));
            return false;
        } else if (!password.matches(".*\\d.*")) {
            mPassword.setError(getString(R.string.password_must_contain_at_least_one_number));
            return false;
        }
        mPassword.setError(null);
        return true;
    }

    private boolean validateConfirmPassword(String confirmPassword, String password) {
        if (TextUtils.isEmpty(confirmPassword)) {
            mConfirmPassword.setError(getString(R.string.please_confirm_your_password));
            return false;
        } else if (!confirmPassword.equals(password)) {
            mConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            return false;
        }
        mConfirmPassword.setError(null);
        return true;
    }

    private void setupRegisterButton() {
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = mEmail.getText().toString().trim();
                final String password = mPassword.getText().toString().trim();
                final String confirmPassword = mConfirmPassword.getText().toString().trim();
                final String username = mUserName.getText().toString();

                boolean isUsernameValid = validateUsername(username);
                boolean isEmailValid = validateEmail(email);
                boolean isPasswordValid = validatePassword(password, username);
                boolean isConfirmPasswordValid = validateConfirmPassword(confirmPassword, password);

                if (isUsernameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid) {
                    progressBar.setVisibility(View.VISIBLE);
                    registerUser(email, password, username);
                }
            }
        });
    }

    private void registerUser(final String email, String password, final String username) {
        fAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    FirebaseUser fuser = fAuth.getCurrentUser();
                    sendVerificationEmail(fuser);
                    saveUserToFirestore(username, email);
                    startActivity(new Intent(getApplicationContext(), BottomNavActivity.class));
                } else {
                    Toast.makeText(Register.this, R.string.failed_the_registered_account_already_exists, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(Register.this, R.string.verification_email_has_been_sent, Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: Email not sent " + e.getMessage());
            }
        });
    }

    private void saveUserToFirestore(String username, String email) {
        Toast.makeText(Register.this, R.string.user_created, Toast.LENGTH_SHORT).show();
        userID = fAuth.getCurrentUser().getUid();
        DocumentReference documentReference = fStore.collection("users").document(userID);
        Map<String, Object> user = new HashMap<>();
        user.put("uName", username);
        user.put("email", email);
        documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: user Profile is created for " + userID);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: " + e.toString());
            }
        });
    }

    private void setupLoginButton() {
        SpannableString ss = new SpannableString(getString(R.string.already_registered_login_here));
        ForegroundColorSpan accentColor = new ForegroundColorSpan(getResources().getColor(R.color.colorAccent));
        ForegroundColorSpan primaryColor = new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary));

        Typeface ubuntuMedium = ResourcesCompat.getFont(this, R.font.ubuntu_medium);
        CustomTypefaceSpan ubuntuMediumSpan = new CustomTypefaceSpan("", ubuntuMedium);

        ss.setSpan(accentColor, 0, 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(primaryColor, 19, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(ubuntuMediumSpan, 19, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mLoginBtn.setText(ss);

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });
    }

    private class CustomTypefaceSpan extends TypefaceSpan {
        private final Typeface typeface;

        public CustomTypefaceSpan(String family, Typeface typeface) {
            super(family);
            this.typeface = typeface;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            applyCustomTypeface(ds, typeface);
        }

        @Override
        public void updateMeasureState(TextPaint paint) {
            applyCustomTypeface(paint, typeface);
        }

        private void applyCustomTypeface(Paint paint, Typeface tf) {
            paint.setTypeface(tf);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkChangeReceiver);
    }
}