package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.GoogleAuthProvider;



public class LoginActivity extends AppCompatActivity {
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private String webClientId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        // Skip login screen if user is already authenticated.
        if (firebaseAuth.getCurrentUser() != null) {
            startMainActivity("Welcome back");
            return;
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

        findViewById(R.id.btnSpotifyDummy).setOnClickListener(v ->
                startMainActivity("Logged in via Spotify")
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
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in canceled", Toast.LENGTH_SHORT).show();
                }
            });

    private void launchGoogleSignIn() {
        signInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startMainActivity("Logged in via Google");
                    } else {
                        Toast.makeText(this, "Firebase auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMainActivity(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}