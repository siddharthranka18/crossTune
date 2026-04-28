package com.example.crossTune;

import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private ConstraintLayout sidebar;
    private View activeIndicator;
    private FrameLayout navSongs, navPlaylist, navJam;
    private TextView tvSongs, tvPlaylist, tvJamSidebar;
    private ImageView btnBottomSettings, btnSidebarProfile;

    private ConstraintLayout bottomPlayerBar;
    private SquigglySeekBar bottomProgressBar; // Upgraded to Premium Squiggly Bar
    private ImageView ivBottomThumbnail, btnBottomPlayPause, btnBottomNext;
    private TextView tvBottomTitle, tvBottomArtist;

    // Fragments
    private Fragment searchFragment;
    private Fragment playerFragment;
    private Fragment playlistsFragment;
    private Fragment settingsFragment;
    private Fragment profileFragment;
    private Fragment activeFragment;

    // Architecture & Remote Playback
    private SharedMusicViewModel musicViewModel;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    // State Tracking
    private View currentActiveAnchor;
    private TextView currentActiveText;
    private boolean isPlayerExpanded = false;
    private boolean isTrackingSeekBar = false;

    private int currentAccentColor = Color.WHITE;
    private final String COLOR_INACTIVE = "#5E6168";

    // 125Hz (8ms) Ultra-Smooth Progress Tracker
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                if (!isTrackingSeekBar) {
                    long duration = mediaController.getDuration();
                    long current = mediaController.getCurrentPosition();
                    if (duration > 0) {
                        if (bottomProgressBar.getMax() != (int) duration) {
                            bottomProgressBar.setMax((int) duration);
                        }
                        bottomProgressBar.setProgress((int) current);
                    }
                }
            }
            mainThreadHandler.postDelayed(this, 8); // Butter smooth redraw
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupMediaController();
        setupViewModel();
        setupFragments(savedInstanceState);
        setupClickListeners();
        setupSeekBar();
        setupBackPressHandler();

        // Start the continuous rendering engine
        mainThreadHandler.post(syncRunnable);
    }

    private void initViews() {
        sidebar = findViewById(R.id.sidebar);
        activeIndicator = findViewById(R.id.active_indicator);
        navSongs = findViewById(R.id.nav_songs_wrapper);
        navPlaylist = findViewById(R.id.nav_playlist_wrapper);
        navJam = findViewById(R.id.nav_jam_wrapper);
        tvSongs = findViewById(R.id.btn_songs);
        tvPlaylist = findViewById(R.id.btn_playlist);
        tvJamSidebar = findViewById(R.id.btn_jam_sidebar);
        btnBottomSettings = findViewById(R.id.btn_bottom_settings);
        btnSidebarProfile = findViewById(R.id.btn_sidebar_profile);

        bottomPlayerBar = findViewById(R.id.bottom_player_bar);
        bottomProgressBar = findViewById(R.id.bottom_progress_bar);
        ivBottomThumbnail = findViewById(R.id.iv_bottom_thumbnail);
        tvBottomTitle = findViewById(R.id.tv_bottom_title);
        tvBottomArtist = findViewById(R.id.tv_bottom_artist);
        btnBottomPlayPause = findViewById(R.id.btn_bottom_play_pause);
        btnBottomNext = findViewById(R.id.btn_bottom_next);
    }

    private void setupMediaController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to connect MediaController", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void setupFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            searchFragment = new SearchFragment();
            playerFragment = new PlayerFragment();
            settingsFragment = new SettingsFragment();
            playlistsFragment = new PlaylistsFragment();
            profileFragment = new ProfileFragment();

            fm.beginTransaction()
                    // ULTIMATE FIX: PlayerFragment goes into the TOP LAYER OVERLAY
                    .add(R.id.player_fragment_container, playerFragment, "PLAYER").hide(playerFragment)
                    .add(R.id.main_fragment_container, settingsFragment, "SETTINGS").hide(settingsFragment)
                    .add(R.id.main_fragment_container, playlistsFragment, "PLAYLIST").hide(playlistsFragment)
                    .add(R.id.main_fragment_container, profileFragment, "PROFILE").hide(profileFragment)
                    .add(R.id.main_fragment_container, searchFragment, "SONGS")
                    .commit();

            activeFragment = searchFragment;
            updateSidebarUI(navSongs, tvSongs, false);

        } else {
            searchFragment = fm.findFragmentByTag("SONGS");
            playerFragment = fm.findFragmentByTag("PLAYER");
            settingsFragment = fm.findFragmentByTag("SETTINGS");
            playlistsFragment = fm.findFragmentByTag("PLAYLIST");
            profileFragment = fm.findFragmentByTag("PROFILE");

            if (searchFragment != null && !searchFragment.isHidden()) {
                activeFragment = searchFragment;
                updateSidebarUI(navSongs, tvSongs, false);
            } else if (playlistsFragment != null && !playlistsFragment.isHidden()) {
                activeFragment = playlistsFragment;
                updateSidebarUI(navPlaylist, tvPlaylist, false);
            } else if (settingsFragment != null && !settingsFragment.isHidden()) {
                activeFragment = settingsFragment;
                updateSidebarUI(btnBottomSettings, null, false);
            } else if (profileFragment != null && !profileFragment.isHidden()) {
                activeFragment = profileFragment;
                updateSidebarUI(btnSidebarProfile, null, false);
            } else if (playerFragment != null && !playerFragment.isHidden()) {
                isPlayerExpanded = true;
                findViewById(R.id.player_fragment_container).setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupViewModel() {
        musicViewModel = new ViewModelProvider(this).get(SharedMusicViewModel.class);

        musicViewModel.getAccentColor().observe(this, colorHex -> {
            if (colorHex != null) {
                try {
                    currentAccentColor = Color.parseColor(colorHex);
                    activeIndicator.setBackgroundColor(currentAccentColor);
                    tvBottomTitle.setTextColor(currentAccentColor);

                    // Directly paint the custom Squiggly SeekBar
                    bottomProgressBar.setAccentColor(currentAccentColor);

                    applyActiveColors();
                } catch (Exception e) {}
            }
        });

        musicViewModel.getCurrentSong().observe(this, song -> {
            if (song != null) {
                if (!isPlayerExpanded) bottomPlayerBar.setVisibility(View.VISIBLE);
                tvBottomTitle.setText(song.getTitle());
                tvBottomArtist.setText(song.getArtist());
                Glide.with(this).load(song.getThumbnailUrl()).into(ivBottomThumbnail);
            }
        });

        musicViewModel.getIsPlaying().observe(this, isPlaying -> {
            btnBottomPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        });

        musicViewModel.getJamRoomCode().observe(this, code -> {
            if (code != null) tvJamSidebar.setTextColor(currentAccentColor);
            else tvJamSidebar.setTextColor(Color.parseColor(COLOR_INACTIVE));
        });
    }

    private void setupSeekBar() {
        bottomProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    mediaController.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingSeekBar = false;

                // If HOST scrubs the bottom bar, Instantly beam the update to listeners
                if (Boolean.TRUE.equals(musicViewModel.getIsJamHost().getValue()) && mediaController != null) {
                    Song currentSong = musicViewModel.getCurrentSong().getValue();
                    if (currentSong != null) {
                        musicViewModel.sendHostUpdate(currentSong, mediaController.isPlaying(), mediaController.getCurrentPosition());
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        navSongs.setOnClickListener(v -> switchTab(searchFragment, navSongs, tvSongs));
        navPlaylist.setOnClickListener(v -> switchTab(playlistsFragment, navPlaylist, tvPlaylist));
        btnBottomSettings.setOnClickListener(v -> switchTab(settingsFragment, btnBottomSettings, null));
        btnSidebarProfile.setOnClickListener(v -> switchTab(profileFragment, btnSidebarProfile, null));
        navJam.setOnClickListener(v -> showJoinJamDialog());

        bottomPlayerBar.setOnClickListener(v -> openFullScreenPlayer());

        btnBottomPlayPause.setOnClickListener(v -> musicViewModel.togglePlayPause());

        // INTELLIGENCE INJECTED: Wire the mini-player's NEXT button to the Context Engine
        btnBottomNext.setOnClickListener(v -> musicViewModel.skipToNext());
    }

    private void showJoinJamDialog() {
        EditText input = new EditText(this);
        input.setTextColor(Color.WHITE);
        input.setHint("Enter 6-Digit Code");
        input.setHintTextColor(Color.parseColor("#5E6168"));
        input.setSingleLine(true);

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 20);
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Join a Jam Session")
                .setView(container)
                .setPositiveButton("Join", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.length() == 6) {
                        musicViewModel.joinJamRoom(code);
                        Toast.makeText(this, "Joined Jam: " + code, Toast.LENGTH_SHORT).show();
                        openFullScreenPlayer();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchTab(Fragment targetFragment, View targetAnchor, @Nullable TextView targetText) {
        if (isPlayerExpanded) closeFullScreenPlayer();
        if (activeFragment == targetFragment) return;

        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit();

        activeFragment = targetFragment;
        updateSidebarUI(targetAnchor, targetText, true);
    }

    // ========================================================
    // THE AESTHETIC ANIMATION MASTERPIECE
    // ========================================================

    public void openFullScreenPlayer() {
        if (isPlayerExpanded) return;
        isPlayerExpanded = true;

        View playerContainer = findViewById(R.id.player_fragment_container);
        View mainContent = findViewById(R.id.main_fragment_container);

        // Calculate screen height dynamically
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 1. Prepare the Fragment (but start it completely off-screen at the bottom)
        getSupportFragmentManager().beginTransaction().show(playerFragment).commitNow();
        playerContainer.setTranslationY(screenHeight);
        playerContainer.setVisibility(View.VISIBLE);

        // 2. The Smooth Slide-Up Animation (Fast Out, Slow In)
        playerContainer.animate()
                .translationY(0)
                .setDuration(450)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        // 3. Parallax Background effect (Pushes the list behind the player down and fades it slightly)
        mainContent.animate()
                .translationY(150)
                .alpha(0.3f)
                .setDuration(450)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        // Fade out the bottom bar so it seamlessly transitions into the full player
        bottomPlayerBar.animate().alpha(0f).setDuration(250).start();
    }

    public void closeFullScreenPlayer() {
        if (!isPlayerExpanded) return;
        isPlayerExpanded = false;

        View playerContainer = findViewById(R.id.player_fragment_container);
        View mainContent = findViewById(R.id.main_fragment_container);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 1. Slide the player back down
        playerContainer.animate()
                .translationY(screenHeight)
                .setDuration(400)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> {
                    playerContainer.setVisibility(View.GONE);
                    getSupportFragmentManager().beginTransaction().hide(playerFragment).commitAllowingStateLoss();
                })
                .start();

        // 2. Pull the background list back into focus
        mainContent.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        // 3. Reveal the bottom bar again
        bottomPlayerBar.animate().alpha(1f).setDuration(400).start();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isPlayerExpanded) closeFullScreenPlayer();
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
    }

    private void updateSidebarUI(View activeAnchor, @Nullable TextView activeText, boolean animate) {
        currentActiveAnchor = activeAnchor;
        currentActiveText = activeText;
        applyActiveColors();

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(sidebar);
        constraintSet.connect(R.id.active_indicator, ConstraintSet.TOP, activeAnchor.getId(), ConstraintSet.TOP);
        constraintSet.connect(R.id.active_indicator, ConstraintSet.BOTTOM, activeAnchor.getId(), ConstraintSet.BOTTOM);

        if (animate) TransitionManager.beginDelayedTransition(sidebar);
        constraintSet.applyTo(sidebar);
    }

    private void applyActiveColors() {
        tvSongs.setTextColor(Color.parseColor(COLOR_INACTIVE));
        tvPlaylist.setTextColor(Color.parseColor(COLOR_INACTIVE));
        btnBottomSettings.setColorFilter(Color.parseColor(COLOR_INACTIVE));
        btnSidebarProfile.setColorFilter(Color.parseColor(COLOR_INACTIVE));

        if (currentActiveText != null) currentActiveText.setTextColor(currentAccentColor);
        else if (currentActiveAnchor == btnBottomSettings) btnBottomSettings.setColorFilter(currentAccentColor);
        else if (currentActiveAnchor == btnSidebarProfile) btnSidebarProfile.setColorFilter(currentAccentColor);

        if (musicViewModel != null && musicViewModel.getJamRoomCode() != null && musicViewModel.getJamRoomCode().getValue() != null) {
            tvJamSidebar.setTextColor(currentAccentColor);
        } else {
            tvJamSidebar.setTextColor(Color.parseColor(COLOR_INACTIVE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the render loop to prevent memory leaks
        mainThreadHandler.removeCallbacks(syncRunnable);

        // Disconnect safely from the Media Session
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            mediaController = null;
        }
    }
}
