package com.example.crossTune;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

public class SettingsFragment extends Fragment {

    // Color Rings (The outer selection border)
    private MaterialCardView ringWhite, ringBlue, ringPurple, ringPink, ringGreen;

    // Color Buttons (The inner clickable circle)
    private MaterialCardView btnWhite, btnBlue, btnPurple, btnPink, btnGreen;

    // Architecture
    private SharedMusicViewModel musicViewModel;

    // Variables for the Secret Easter Egg
    private int tapCount = 0;
    private Toast currentToast;
    private final Handler resetHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetRunnable = () -> tapCount = 0;

    // Predefined Aesthetic Hex Colors
    public static final String COLOR_WHITE = "#FFFFFF";
    public static final String COLOR_BLUE = "#4A90E2";
    public static final String COLOR_PURPLE = "#9B4AE2";
    public static final String COLOR_PINK = "#E24A62";
    public static final String COLOR_GREEN = "#4AE2A4";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupColorPicker();
        setupSecretEasterEgg(view);
    }

    private void initViews(View view) {
        ringWhite = view.findViewById(R.id.ring_color_white);
        ringBlue = view.findViewById(R.id.ring_color_blue);
        ringPurple = view.findViewById(R.id.ring_color_purple);
        ringPink = view.findViewById(R.id.ring_color_pink);
        ringGreen = view.findViewById(R.id.ring_color_green);

        btnWhite = view.findViewById(R.id.btn_color_white);
        btnBlue = view.findViewById(R.id.btn_color_blue);
        btnPurple = view.findViewById(R.id.btn_color_purple);
        btnPink = view.findViewById(R.id.btn_color_pink);
        btnGreen = view.findViewById(R.id.btn_color_green);
    }

    private void setupViewModel() {
        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);

        // Instantly updates the ring selection when the fragment opens based on saved preference
        musicViewModel.getAccentColor().observe(getViewLifecycleOwner(), this::animateRingSelection);
    }

    private void setupColorPicker() {
        // When a user clicks a color, we send the Hex Code directly to the Global Database
        btnWhite.setOnClickListener(v -> musicViewModel.setAccentColor(COLOR_WHITE));
        btnBlue.setOnClickListener(v -> musicViewModel.setAccentColor(COLOR_BLUE));
        btnPurple.setOnClickListener(v -> musicViewModel.setAccentColor(COLOR_PURPLE));
        btnPink.setOnClickListener(v -> musicViewModel.setAccentColor(COLOR_PINK));
        btnGreen.setOnClickListener(v -> musicViewModel.setAccentColor(COLOR_GREEN));
    }

    /**
     * Handles the flawless Apple-style animation.
     * Sets the chosen ring's stroke width to 2dp (visible) and hides the rest (0dp).
     */
    private void animateRingSelection(String activeColorHex) {
        // First, hide all rings
        ringWhite.setStrokeWidth(0);
        ringBlue.setStrokeWidth(0);
        ringPurple.setStrokeWidth(0);
        ringPink.setStrokeWidth(0);
        ringGreen.setStrokeWidth(0);

        // Convert the dp value to pixels for the MaterialCardView API
        int strokeWidthPx = (int) (2 * getResources().getDisplayMetrics().density);

        // Reveal the active ring
        switch (activeColorHex) {
            case COLOR_WHITE:
                ringWhite.setStrokeWidth(strokeWidthPx);
                break;
            case COLOR_BLUE:
                ringBlue.setStrokeWidth(strokeWidthPx);
                break;
            case COLOR_PURPLE:
                ringPurple.setStrokeWidth(strokeWidthPx);
                break;
            case COLOR_PINK:
                ringPink.setStrokeWidth(strokeWidthPx);
                break;
            case COLOR_GREEN:
                ringGreen.setStrokeWidth(strokeWidthPx);
                break;
            default:
                // Fallback to white if something goes wrong
                ringWhite.setStrokeWidth(strokeWidthPx);
                break;
        }
    }

    /**
     * A premium touch: Tapping the background 7 times quickly reveals a
     * secret toast message, mimicking Android's Developer Options unlock.
     */
    private void setupSecretEasterEgg(View rootView) {
        rootView.setOnClickListener(v -> {
            tapCount++;
            resetHandler.removeCallbacks(resetRunnable);
            resetHandler.postDelayed(resetRunnable, 2000);

            if (tapCount >= 3 && tapCount < 7) {
                int stepsAway = 7 - tapCount;
                showToast("You are " + stepsAway + " steps away from being a developer.");
            } else if (tapCount == 7) {
                showToast("✨ Developer Mode Unlocked, Smurphine! ✨");
                tapCount = 0;
            }
        });
    }

    private void showToast(String message) {
        if (currentToast != null) currentToast.cancel();
        currentToast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        currentToast.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        resetHandler.removeCallbacks(resetRunnable);
        if (currentToast != null) currentToast.cancel();
    }
}