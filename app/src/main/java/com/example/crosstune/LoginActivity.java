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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final boolean ENABLE_DIRECT_DB_SYNC = true; // Prototype mode

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        // Prototype UX: If already logged in, instantly route to Home to show a smooth flow
        FirebaseUser existingUser = firebaseAuth.getCurrentUser();
        if (existingUser != null) {
            startMainActivity("Welcome back!");
            syncUserToDbInBackground(existingUser.getDisplayName(), existingUser.getEmail());
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getWebClientId())
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Placeholder for Email Demo
        findViewById(R.id.btnSignIn).setOnClickListener(v ->
                startMainActivity("Demo: Logged in via Email")
        );

        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> launchGoogleSignIn());
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
                    String safeName = (name == null || name.trim().isEmpty()) ? "Demo User" : name;
                    String safeEmail = (email == null || email.trim().isEmpty()) ? "demo@crosstune.com" : email;

                    String query = "INSERT INTO users (name, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name)";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, safeName);
                        stmt.setString(2, safeEmail);
                        stmt.executeUpdate();
                        Log.d(TAG, "Background Sync: User info updated in DB.");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Background Database Sync Error: " + e.getMessage());
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