package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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



public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    // Direct JDBC drivers are not reliably supported on Android runtime.
    private static final boolean ENABLE_DIRECT_DB_SYNC = false;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private String webClientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        // Keep login visible even when a prior Firebase session exists.
        FirebaseUser existingUser = firebaseAuth.getCurrentUser();
        if (existingUser != null) {
            saveUserToDb(existingUser.getDisplayName(), existingUser.getEmail(), () -> {
                // No auto-navigation here; user stays on login screen until explicit action.
            });
            Toast.makeText(this, "Session found. Continue with Google to enter app.", Toast.LENGTH_SHORT).show();
        }

        webClientId = getWebClientId();

        GoogleSignInOptions.Builder gsoBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail();

        if (!webClientId.isEmpty()) {
            gsoBuilder.requestIdToken(webClientId);
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        findViewById(R.id.btnSignIn).setOnClickListener(v ->
                startMainActivity("Logged in via Email")
        );

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> launchGoogleSignIn());
    }

    private String getWebClientId() {
        int webClientIdRes = getResources().getIdentifier(
                "default_web_client_id",
                "string",
                getPackageName()
        );
        return webClientIdRes == 0 ? "" : getString(webClientIdRes);
    }

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null || account.getIdToken() == null) {
                        Toast.makeText(
                                this,
                                "Google OAuth not configured in Firebase (missing Web client ID)",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }
                    firebaseAuthWithGoogle(account);
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in canceled", Toast.LENGTH_SHORT).show();
                }
            });

    private void launchGoogleSignIn() {
        signInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToDbAndContinue(account.getDisplayName(), account.getEmail(), "Logged in via Google");
                    } else {
                        Toast.makeText(this, "Firebase auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToDbAndContinue(String name, String email, String successMessage) {
        saveUserToDb(name, email, () -> startMainActivity(successMessage));
    }

    private void saveUserToDb(String name, String email, Runnable onDone) {
        new Thread(() -> {
            String errorMessage = null;
            boolean synced = false;

            try {
                if (!ENABLE_DIRECT_DB_SYNC) {
                    errorMessage = "Cloud sync disabled on device build";
                } else {
                try (Connection conn = DBConnection.connect()) {
                    if (conn != null) {
                    String safeName = (name == null || name.trim().isEmpty()) ? "Test User" : name;
                    String safeEmail = (email == null || email.trim().isEmpty()) ? "test@gmail.com" : email;

                        String query = "INSERT INTO users (name, email) VALUES (?, ?) " +
                                "ON DUPLICATE KEY UPDATE name = VALUES(name)";

                        try (PreparedStatement stmt = conn.prepareStatement(query)) {
                            stmt.setString(1, safeName);
                            stmt.setString(2, safeEmail);
                            stmt.executeUpdate();
                        }

                        synced = true;
                    } else {
                        errorMessage = "Connection failed";
                    }
                }
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                Log.e(TAG, "Failed to sync user to DB", e);
            } catch (Throwable t) {
                errorMessage = "Cloud sync unavailable on this runtime";
                Log.e(TAG, "JDBC runtime not supported on Android device", t);
            }

            String finalErrorMessage = errorMessage;
            boolean finalSynced = synced;
            runOnUiThread(() -> {
                if (finalSynced) {
                    Toast.makeText(this, "User synced to DB", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "DB sync skipped: " + finalErrorMessage, Toast.LENGTH_LONG).show();
                }
                onDone.run();
            });
        }).start();
    }

    private void startMainActivity(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}