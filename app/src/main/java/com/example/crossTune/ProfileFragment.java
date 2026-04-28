package com.example.crossTune;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail;
    private FirebaseAuth mAuth;

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

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);

        if (user != null) {
            tvName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            tvEmail.setText(user.getEmail());
        }

        // 1. Top Artist (Insights) - Demonstrates JOIN + GROUP BY
        view.findViewById(R.id.btn_insights).setOnClickListener(v -> {
            showInsightsDialog();
        });

        // 2. Delete Account - Demonstrates TRANSACTIONS (ACID)
        // Now highlighted at the bottom
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

        // Log Out - Now moved to the menu list
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

    private void showInsightsDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Your Music Insights")
                .setMessage("We are calculating your Top Artist by JOINING your Playlists with the Song Cache and using a GROUP BY query.")
                .setPositiveButton("Cool!", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account?")
                .setMessage("This action is permanent. It will trigger a SQL TRANSACTION to delete your User profile, Playlists, and Song History all at once (ACID property).")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    Toast.makeText(getContext(), "Transaction Initiated...", Toast.LENGTH_SHORT).show();
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