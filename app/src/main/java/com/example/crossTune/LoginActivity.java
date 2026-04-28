package com.example.crossTune;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity{

    private static final int RC_SIGN_IN = 9001;
    private static final boolean FORCE_GOOGLE_CONSENT = false;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_login);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(v -> startSignIn());

        if (FORCE_GOOGLE_CONSENT) {
            auth.signOut();
            googleSignInClient.signOut().addOnCompleteListener(task ->
                    googleSignInClient.revokeAccess().addOnCompleteListener(task2 -> {}));
        }

        if (!FORCE_GOOGLE_CONSENT && auth.getCurrentUser() != null) {
            saveUserToDb(auth.getCurrentUser());
            goToMain();
        }

    }

    private void startSignIn() {
        googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInClient.revokeAccess().addOnCompleteListener(task2 -> {
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, RC_SIGN_IN);
                })
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    if (account.getIdToken() == null) {
                        Log.e("LoginActivity", "Google sign-in missing ID token. Check default_web_client_id.");
                        return;
                    }
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.e("LoginActivity", "Google sign-in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        saveUserToDb(user);
                    }
                    goToMain();
                })
                .addOnFailureListener(e -> Log.e("LoginActivity", "Firebase sign-in failed", e));
    }

    private void saveUserToDb(FirebaseUser user) {
        String uid = user.getUid();
        String name = user.getDisplayName();
        String email = user.getEmail();

        if (email == null) return;
        String userId = escapeSql(uid);
        String userName = escapeSql(name);
        String userEmail = escapeSql(email);
        String query = "INSERT INTO Users (UserID, name, email) VALUES (" +
                "'" + userId + "'," +
                "'" + userName + "'," +
                "'" + userEmail + "') " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name), email=VALUES(email)";
        DB.execute(query);
    }

    private String escapeSql(String input) {
        if (input == null) return "";
        return input.replace("'", "''");
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
