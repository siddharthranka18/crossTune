package com.example.crossTune;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize UI components and set click listeners here if needed
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            // Handle Logout
        });
        
        view.findViewById(R.id.btn_contact_us).setOnClickListener(v -> {
            // Handle Contact Us
        });
        
        view.findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> {
            // Handle Privacy Policy
        });
        
        view.findViewById(R.id.btn_your_playlist).setOnClickListener(v -> {
            // Handle Your Playlist
        });
    }
}
