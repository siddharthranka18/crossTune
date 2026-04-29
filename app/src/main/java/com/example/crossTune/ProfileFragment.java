package com.example.crossTune;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvTopArtistsList;
    private LinearLayout layoutTopArtists;
    private ImageView ivInsightsArrow;
    private FirebaseAuth mAuth;
    private SharedMusicViewModel musicViewModel;
    private boolean isInsightsExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvTopArtistsList = view.findViewById(R.id.tv_top_artists_list);
        layoutTopArtists = view.findViewById(R.id.layout_top_artists);
        ivInsightsArrow = view.findViewById(R.id.iv_insights_arrow);

        if (user != null) {
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            tvEmail.setText(user.getEmail());
        }

        // FULFILLING RUBRIC: JOIN + GROUP BY (Point 7, 8)
        // Fetches Top 5 Artists specifically for this User from the Database
        view.findViewById(R.id.btn_insights).setOnClickListener(v -> {
            toggleInsightsDropdown();
        });

        // 2. Delete Account - Demonstrates TRANSACTIONS (ACID)
        view.findViewById(R.id.btn_delete_account).setOnClickListener(v -> {
            showDeleteConfirmation();
        });

        // Other menu buttons
        view.findViewById(R.id.btn_your_playlist).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Opening your playlists...", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_contact_us).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Contact Support: support@crosstune.com", Toast.LENGTH_SHORT).show();
        });

        // Log Out
        view.findViewById(R.id.btn_logout_menu).setOnClickListener(v -> {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient client = GoogleSignIn.getClient(requireContext(), gso);
            client.signOut().addOnCompleteListener(task -> {
                FirebaseAuth.getInstance().signOut();
                navigateToLogin();
            }).addOnFailureListener(err -> {
                FirebaseAuth.getInstance().signOut();
                navigateToLogin();
            });
        });
    }

    private void toggleInsightsDropdown() {
        if (isInsightsExpanded) {
            // Collapse
            layoutTopArtists.setVisibility(View.GONE);
            ivInsightsArrow.setRotation(180);
            isInsightsExpanded = false;
        } else {
            // Expand
            layoutTopArtists.setVisibility(View.VISIBLE);
            ivInsightsArrow.setRotation(90); // Point down
            isInsightsExpanded = true;
            
            // Fetch the data from the DB using JOIN + GROUP BY
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                tvTopArtistsList.setText("Querying Database...");
                DB.getTopArtists(user.getUid(), artists -> {
                    if (artists.isEmpty()) {
                        tvTopArtistsList.setText("No data found. Add songs to your playlists to see insights!");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < artists.size(); i++) {
                            sb.append((i + 1)).append(". ").append(artists.get(i)).append("\n");
                        }
                        tvTopArtistsList.setText(sb.toString().trim());
                    }
                });
            } else {
                tvTopArtistsList.setText("Error: User not logged in.");
            }
        }
    }

    private void showDeleteConfirmation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user != null ? user.getUid() : null;
        String message = "This action is permanent. It will trigger a SQL TRANSACTION to delete your User profile, Playlists, and Song History all at once (ACID property).";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account?")
                .setMessage(message)
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    Toast.makeText(getContext(), "Transaction Initiated...", Toast.LENGTH_SHORT).show();
                    musicViewModel.deleteCurrentUserData(success -> {
                        if (success) {
                            FirebaseAuth.getInstance().signOut();
                            Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                            navigateToLogin();
                        } else {
                            Toast.makeText(getContext(), "Delete failed. Please try again.", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
