package com.owlvation.project.genedu.User;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.owlvation.project.genedu.Dashboard.BottomNavActivity;
import com.owlvation.project.genedu.R;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    public static final String TAG = "TAG";
    private EditText mEmail, mPassword;
    private ImageView mPasswordToggle;
    private boolean isPasswordVisible = false;
    private Button mLoginBtn;
    private TextView mCreateBtn, forgotTextLink;
    private ProgressBar progressBar;
    private FirebaseAuth fAuth;
    private FirebaseDatabase database;
    Button signInGoogle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupTextWatchers();
        setupFirebase();
        setupPasswordToggle();
        setupLoginButton();
        setupRegisterButton();
        setupForgotPasswordButton();
    }

    private void initializeViews() {
        mEmail = findViewById(R.id.Email);
        mPassword = findViewById(R.id.password);
        mPasswordToggle = findViewById(R.id.passwordToggle);
        progressBar = findViewById(R.id.progressBar);
        mLoginBtn = findViewById(R.id.loginBtn);
        mCreateBtn = findViewById(R.id.createText);
        forgotTextLink = findViewById(R.id.forgotPassword);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                                                Toast.makeText(Login.this, R.string.verification_email_has_been_sent, Toast.LENGTH_SHORT).show();
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
                                                                    Toast.makeText(Login.this, R.string.logged_in_successfully, Toast.LENGTH_SHORT).show();
                                                                    startActivity(new Intent(getApplicationContext(), BottomNavActivity.class));
                                                                    finish();
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    progressBar.setVisibility(View.GONE);
                                                                    Toast.makeText(Login.this, R.string.failed_make_sure_the_account_is_registered, Toast.LENGTH_SHORT).show();
                                                                });
                                                    } else {
                                                        progressBar.setVisibility(View.GONE);
                                                        saveLoginStatus(true);
                                                        Toast.makeText(Login.this, R.string.logged_in_successfully, Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(getApplicationContext(), BottomNavActivity.class));
                                                        finish();
                                                    }
                                                } else {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(Login.this, R.string.failed_make_sure_the_account_is_registered, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(Login.this, R.string.failed_make_sure_the_account_is_registered, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupTextWatchers() {
        mEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });

        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
            }
        });
    }

    private void setupLoginButton() {
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmail.getText().toString().trim();
                String password = mPassword.getText().toString().trim();

                boolean isEmailValid = validateEmail(email);
                boolean isPasswordValid = validatePassword(password);

                if (isEmailValid && isPasswordValid) {
                    performLogin(email, password);
                }
            }
        });
    }

    private void setupFirebase() {
        fAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    private void setupPasswordToggle() {
        mPasswordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            mPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
            mPasswordToggle.setColorFilter(getResources().getColor(R.color.colorAccent));
        } else {
            mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            mPasswordToggle.setImageResource(R.drawable.ic_visibility_on);
            mPasswordToggle.setColorFilter(getResources().getColor(R.color.colorPrimary));
        }
        mPassword.setTypeface(ResourcesCompat.getFont(Login.this, R.font.ubuntu_regular));
        mPassword.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        mPassword.setSelection(mPassword.length());
        isPasswordVisible = !isPasswordVisible;
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

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            mPassword.setError(getString(R.string.password_is_required));
            return false;
        } else if (password.length() < 8) {
            mPassword.setError(getString(R.string.password_must_be_more_than_equal_8_character));
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

    private void performLogin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        fAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    saveLoginStatus(true);
                    Toast.makeText(Login.this, R.string.logged_in_successfully, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), BottomNavActivity.class));
                } else {
                    Toast.makeText(Login.this, R.string.failed_make_sure_the_account_is_registered, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupRegisterButton() {
        SpannableString ss = new SpannableString(getString(R.string.new_here_register));
        ForegroundColorSpan accentColor = new ForegroundColorSpan(getResources().getColor(R.color.colorAccent));
        ForegroundColorSpan primaryColor = new ForegroundColorSpan(getResources().getColor(R.color.colorPrimary));

        Typeface ubuntuMedium = ResourcesCompat.getFont(this, R.font.ubuntu_medium);
        CustomTypefaceSpan ubuntuMediumSpan = new CustomTypefaceSpan("", ubuntuMedium);

        ss.setSpan(accentColor, 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(primaryColor, 10, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(ubuntuMediumSpan, 10, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        mCreateBtn.setText(ss);

        mCreateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), Register.class));
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

    private void setupForgotPasswordButton() {
        forgotTextLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleForgotPassword();
            }
        });
    }

    private void handleForgotPassword() {
        String email = mEmail.getText().toString().trim();

        if (!validateEmail(email)) {
            mEmail.setError(getString(R.string.please_enter_your_email_to_reset_password));
            return;
        }

        showPasswordResetDialog(email);
    }

    private void showPasswordResetDialog(final String email) {
        AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(this);
        passwordResetDialog.setTitle(R.string.reset_password);
        passwordResetDialog.setMessage(getString(R.string.a_reset_link_will_be_sent_to_your_email) + email);

        passwordResetDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendPasswordResetEmail(email);
            }
        });

        passwordResetDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        passwordResetDialog.create().show();
    }

    private void sendPasswordResetEmail(String email) {
        fAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(Login.this, R.string.reset_link_sent_to_your_email, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Login.this, getString(R.string.failed_reset_link_is_not_sent) + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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


}