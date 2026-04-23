package com.example.crosstune;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class ProfileActivity extends AppCompatActivity {

    private boolean isPlatformExpanded = false;
    private String CLIENT_ID = "c2eb5a6730aa447f9972ae85006f5981";
    private String CLIENT_SECRET = "9d09837f5fd44e8ba4168169006df5e6";
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    private static final int REQUEST_CODE = 1337; // Just a random ID number
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                case TOKEN:
                    // SUCCESS! You now have the "Access Token"
                    String token = response.getAccessToken();
                    System.out.println("Got the token: " + token);
                    break;

                case ERROR:
                    // Something went wrong (check your SHA-1 or Redirect URI)
                    break;
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Back Button Logic
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 2. Platform Selection Logic
        RelativeLayout btnChoosePlatform = findViewById(R.id.btnChoosePlatform);
        LinearLayout platformOptionsContainer = findViewById(R.id.platformOptionsContainer);
        ImageView ivPlatformArrow = findViewById(R.id.ivPlatformArrow);
        RelativeLayout optionSpotify = findViewById(R.id.optionSpotify);

        btnChoosePlatform.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlatformExpanded) {
                    platformOptionsContainer.setVisibility(View.GONE);
                    ivPlatformArrow.animate().rotation(0).setDuration(200).start();
                } else {
                    platformOptionsContainer.setVisibility(View.VISIBLE);
                    ivPlatformArrow.animate().rotation(90).setDuration(200).start();
                }
                isPlatformExpanded = !isPlatformExpanded;
            }
        });

        optionSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "Spotify Selected", Toast.LENGTH_SHORT).show();
                AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
                builder.setScopes(new String[]{"user-read-private", "playlist-modify-public", "playlist-modify-private", "user-modify-playback-state"});
                AuthorizationRequest request = builder.build();
                AuthorizationClient.openLoginActivity(ProfileActivity.this, REQUEST_CODE, request);
            }
        });

        // 3. Logout Logic
        TextView btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();

                // Sign out from Google to allow account selection next time
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .build();
                GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(ProfileActivity.this, gso);
                
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    Toast.makeText(ProfileActivity.this, "LOGGED OUT SUCCESSFULLY", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        });
    }
}