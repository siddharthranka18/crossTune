package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final boolean ENABLE_DIRECT_DB_SYNC = true;

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        AppState.init(this);

        firebaseAuth = FirebaseAuth.getInstance();
        AppState.sessionManager = new SessionManager(this);

        FirebaseUser existingUser = firebaseAuth.getCurrentUser();
        if (existingUser != null) {
            AppState.sessionManager.saveGoogleUser(existingUser.getDisplayName(), existingUser.getEmail(), existingUser.getUid());
            startMainActivity("Welcome back!");
            syncUserToDbInBackground(existingUser.getDisplayName(), existingUser.getEmail());
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getWebClientId())
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        EditText emailField = findViewById(R.id.uid);
        EditText passwordField = findViewById(R.id.pid);

        // Sign In Button - Verifies with Firebase
        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            } else {
                signInWithEmail(email, password);
            }
        });

        // Go to Sign Up Page
        findViewById(R.id.btnGoToSignUp).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> launchGoogleSignIn());
    }

    private void signInWithEmail(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            AppState.sessionManager.saveGoogleUser(user.getDisplayName(), user.getEmail(), user.getUid());
                            syncUserToDbInBackground(user.getDisplayName(), user.getEmail());
                        }
                        startMainActivity("Signed in successfully!");
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getWebClientId() {
        int webClientIdRes = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
        return webClientIdRes == 0 ? "" : getString(webClientIdRes);
    }

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account != null && account.getIdToken() != null) {
                        firebaseAuthWithGoogle(account);
                    } else {
                        Toast.makeText(this, "Configuration error: Missing Web client ID", Toast.LENGTH_LONG).show();
                    }
                } catch (ApiException e) {
                    Log.e(TAG, "Google sign-in failed", e);
                    Toast.makeText(this, "Sign-in canceled", Toast.LENGTH_SHORT).show();
                }
            });

    private void launchGoogleSignIn() {
        signInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            AppState.sessionManager.saveGoogleUser(user.getDisplayName(), user.getEmail(), user.getUid());
                        }
                        startMainActivity("Signed in successfully!");
                        syncUserToDbInBackground(account.getDisplayName(), account.getEmail());
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncUserToDbInBackground(String name, String email) {
        if (!ENABLE_DIRECT_DB_SYNC) return;

        executorService.execute(() -> {
            try (Connection conn = DBConnection.connect()) {
                if (conn != null) {
                    String safeName = (name == null || name.trim().isEmpty()) ? "User" : name;
                    String safeEmail = (email == null || email.trim().isEmpty()) ? "user@crosstune.com" : email;

                    String query = "INSERT INTO users (name, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, safeName);
                        stmt.setString(2, safeEmail);
                        stmt.executeUpdate();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Sync Error: " + e.getMessage());
            }
        });
    }

    private void startMainActivity(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}