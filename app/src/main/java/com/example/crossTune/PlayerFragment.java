package com.example.crossTune;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlayerFragment extends Fragment {

    // UI Elements
    private ImageView ivHugeThumbnail, ivPlayPauseIcon, btnPrev, btnNext, btnLike, btnRepeat, btnCollapse, btnDownload, ivJamIcon;
    private MaterialCardView btnPlayPause;
    private TextView tvSongName, tvArtistName, tvTimeCurrent, tvTimeTotal, tvJamStatus;
    private TextView tvPlayingContext, tvContextLabel; // CONTEXT HEADER
    private SeekBar seekBarProgress;
    private LinearLayout bottomActionBar, btnJamHost, layoutPlayingContext;

    // Architecture & Remote Playback
    private SharedMusicViewModel musicViewModel;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    private long pendingJamSeekMs = -1;

    // State Tracking
    private Song currentActiveSong;
    private String resolvedStreamUrl;
    private String currentSongId = "";

    // Theme & Interaction States
    private boolean isTrackingSeekBar = false;
    private int currentAccentColor = Color.WHITE;

    // Flawless SSOT Engine
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private Future<?> activeNetworkFuture;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    // Progress Bar & Host Sync Broadcaster
    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                if (!isTrackingSeekBar) {
                    long current = mediaController.getCurrentPosition();
                    seekBarProgress.setProgress((int) current);
                    tvTimeCurrent.setText(formatTime(current));
                }
                // If HOST, continuously blast our exact timestamp to the server every 1 second
                if (Boolean.TRUE.equals(musicViewModel.getIsJamHost().getValue()) && currentActiveSong != null) {
                    musicViewModel.sendHostUpdate(currentActiveSong, true, mediaController.getCurrentPosition());
                }
            }
            mainThreadHandler.postDelayed(this, 8);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupMediaController();
        setupViewModel();
        setupClickListeners();
        setupSeekBar();
        mainThreadHandler.post(syncRunnable);
    }

    private void initViews(View view) {
        btnCollapse = view.findViewById(R.id.btn_collapse);
        btnDownload = view.findViewById(R.id.btn_download);
        btnJamHost = view.findViewById(R.id.btn_jam_host);
        ivJamIcon = view.findViewById(R.id.iv_jam_icon);
        tvJamStatus = view.findViewById(R.id.tv_jam_status);
        ivHugeThumbnail = view.findViewById(R.id.iv_huge_thumbnail);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        ivPlayPauseIcon = view.findViewById(R.id.iv_play_pause_icon);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);
        btnLike = view.findViewById(R.id.btn_like);
        btnRepeat = view.findViewById(R.id.btn_repeat);
        tvSongName = view.findViewById(R.id.tv_player_song_name);
        tvArtistName = view.findViewById(R.id.tv_player_artist_name);
        tvTimeCurrent = view.findViewById(R.id.tv_time_current);
        tvTimeTotal = view.findViewById(R.id.tv_time_total);
        seekBarProgress = view.findViewById(R.id.seek_bar_progress);
        bottomActionBar = view.findViewById(R.id.bottom_action_bar);

        layoutPlayingContext = view.findViewById(R.id.layout_playing_context);
        tvContextLabel = view.findViewById(R.id.tv_context_label);
        tvPlayingContext = view.findViewById(R.id.tv_playing_context);
    }

    private String formatTime(long millis) {
        if (millis < 0) return "00:00";
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void setupMediaController() {
        SessionToken sessionToken = new SessionToken(requireContext(), new ComponentName(requireContext(), MusicService.class));
        controllerFuture = new MediaController.Builder(requireContext(), sessionToken).buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY) {
                            btnPlayPause.setAlpha(1.0f);
                            seekBarProgress.setMax((int) mediaController.getDuration());
                            tvTimeTotal.setText(formatTime(mediaController.getDuration()));
                            updatePlayPauseUI(true);

                            // Update song metadata with duration for UI display
                            if (currentActiveSong != null && currentActiveSong.getDuration() == 0) {
                                currentActiveSong.setDuration(mediaController.getDuration() / 1000);
                            }
                        } else if (state == Player.STATE_ENDED) {
                            ivPlayPauseIcon.setImageResource(R.drawable.ic_play);
                            seekBarProgress.setProgress(0);
                            tvTimeCurrent.setText("00:00");

                            if (musicViewModel.getJamRoomCode().getValue() == null || Boolean.TRUE.equals(musicViewModel.getIsJamHost().getValue())) {
                                musicViewModel.skipToNext();
                            }
                        }
                    }
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        updatePlayPauseUI(isPlaying);
                        Boolean vmState = musicViewModel.getIsPlaying().getValue();
                        if (vmState != null && vmState != isPlaying) musicViewModel.togglePlayPause();

                        if (Boolean.TRUE.equals(musicViewModel.getIsJamHost().getValue()) && currentActiveSong != null) {
                            musicViewModel.sendHostUpdate(currentActiveSong, isPlaying, mediaController.getCurrentPosition());
                        }
                    }
                });
            } catch (Exception e) {}
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setupViewModel() {
        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);

        // UI THEME SYNC
        musicViewModel.getAccentColor().observe(getViewLifecycleOwner(), colorHex -> {
            if (colorHex != null) {
                try {
                    currentAccentColor = Color.parseColor(colorHex);
                    ((SquigglySeekBar) seekBarProgress).setAccentColor(currentAccentColor);
                    tvPlayingContext.setTextColor(currentAccentColor);

                    if (currentActiveSong != null) updateLikeButtonUI(musicViewModel.isSongLiked(currentActiveSong.getId()));
                    if (mediaController != null && mediaController.getRepeatMode() == Player.REPEAT_MODE_ONE) btnRepeat.setColorFilter(currentAccentColor);

                    if (musicViewModel.getJamRoomCode().getValue() != null) {
                        tvJamStatus.setTextColor(currentAccentColor);
                        ivJamIcon.setColorFilter(currentAccentColor);
                    }
                } catch (Exception e) {}
            }
        });

        // QUEUE CONTEXT SYNC
        musicViewModel.getPlayingContext().observe(getViewLifecycleOwner(), contextName -> {
            if (contextName != null && !contextName.isEmpty()) {
                layoutPlayingContext.setVisibility(View.VISIBLE);
                tvPlayingContext.setText(contextName);
            } else {
                layoutPlayingContext.setVisibility(View.INVISIBLE);
            }
        });

        // FLAWLESS SONG LOAD ENGINE
        musicViewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null && !song.getId().equals(currentSongId)) {
                currentSongId = song.getId();
                currentActiveSong = song;
                tvSongName.setText(song.getTitle());
                tvArtistName.setText(song.getArtist());
                seekBarProgress.setProgress(0);
                tvTimeCurrent.setText("00:00");

                // Show approximate time if we have it before loading finishes
                if (song.getDuration() > 0) tvTimeTotal.setText(formatTime(song.getDuration() * 1000));
                else tvTimeTotal.setText("00:00");

                updateLikeButtonUI(musicViewModel.isSongLiked(song.getId()));
                loadHighResThumbnail(song.getThumbnailUrl());

                if (song.getLocalPath() != null && new File(song.getLocalPath()).exists()) {
                    resolvedStreamUrl = song.getLocalPath();
                    passUrlToService(song.getLocalPath(), song);
                } else {
                    startJioSaavnSSOTProtocol(song);
                }
            }
        });

        musicViewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (mediaController != null) {
                if (isPlaying) { if (mediaController.getPlaybackState() == Player.STATE_READY) mediaController.play(); updatePlayPauseUI(true); }
                else { mediaController.pause(); updatePlayPauseUI(false); }
            }
        });

        // ==========================================
        // THE JAMMING SYNC ENGINE
        // ==========================================
        musicViewModel.getJamRoomCode().observe(getViewLifecycleOwner(), code -> {
            if (code == null) {
                tvJamStatus.setText("START JAM");
                tvJamStatus.setTextColor(Color.parseColor("#8A8D93"));
                ivJamIcon.setColorFilter(Color.parseColor("#8A8D93"));
                unlockControlsForListener(true);
            } else {
                tvJamStatus.setText("ROOM " + code);
                tvJamStatus.setTextColor(currentAccentColor);
                ivJamIcon.setColorFilter(currentAccentColor);

                if (Boolean.FALSE.equals(musicViewModel.getIsJamHost().getValue())) {
                    unlockControlsForListener(false);
                }
            }
        });

        musicViewModel.getJamSyncEvent().observe(getViewLifecycleOwner(), state -> {
            if (state == null || Boolean.TRUE.equals(musicViewModel.getIsJamHost().getValue())) return;

            long latency = System.currentTimeMillis() - state.hostTime;
            long targetSeek = state.seekPosition + (state.isPlaying ? latency : 0);

            if (!state.videoId.equals(currentSongId)) {
                Song incomingSong = new Song(state.videoId, state.title, state.artist, "Unknown Album", state.thumbnailUrl, 0);
                if (state.streamUrl != null) incomingSong.setStreamUrl(state.streamUrl); // host gives us the URL directly
                musicViewModel.playSongWithContext(incomingSong, null, "Jam Session");
            } else if (mediaController != null) {
                long diff = Math.abs(mediaController.getCurrentPosition() - targetSeek);
                if (diff > 2000) { mediaController.seekTo(targetSeek); }

                if (state.isPlaying && !mediaController.isPlaying()) { musicViewModel.togglePlayPause(); }
                else if (!state.isPlaying && mediaController.isPlaying()) { musicViewModel.togglePlayPause(); }
            }
        });
    }

    private void unlockControlsForListener(boolean unlock) {
        float alpha = unlock ? 1.0f : 0.3f;
        btnPlayPause.setEnabled(unlock); btnPlayPause.setAlpha(alpha);
        btnPrev.setEnabled(unlock); btnPrev.setAlpha(alpha);
        btnNext.setEnabled(unlock); btnNext.setAlpha(alpha);
        seekBarProgress.setEnabled(unlock);
    }

    private void setupClickListeners() {
        btnCollapse.setOnClickListener(v -> { if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).closeFullScreenPlayer(); });

        btnJamHost.setOnClickListener(v -> {
            if (musicViewModel.getJamRoomCode().getValue() != null) {
                new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                        .setTitle("Leave Jam?")
                        .setMessage("Are you sure you want to disconnect from Room " + musicViewModel.getJamRoomCode().getValue() + "?")
                        .setPositiveButton("Leave", (dialog, which) -> musicViewModel.leaveJamRoom())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                musicViewModel.createJamRoom();
                Toast.makeText(getContext(), "Jam Room Created! Tell friends to join.", Toast.LENGTH_LONG).show();
            }
        });

        btnDownload.setOnClickListener(v -> {
            if (currentActiveSong == null) return;
            if (currentActiveSong.getLocalPath() != null && new File(currentActiveSong.getLocalPath()).exists()) {
                Toast.makeText(getContext(), "Already downloaded!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (resolvedStreamUrl != null) musicViewModel.downloadSong(requireContext(), currentActiveSong, resolvedStreamUrl);
            else Toast.makeText(getContext(), "Resolving audio... Please wait a moment.", Toast.LENGTH_SHORT).show();
        });

        bottomActionBar.setOnClickListener(v -> {
            new PlaylistsBottomSheet().show(getParentFragmentManager(), "PlaylistsBottomSheet");
        });

        btnPlayPause.setOnClickListener(v -> {
            if (mediaController == null || mediaController.getPlaybackState() == Player.STATE_IDLE) return;
            musicViewModel.togglePlayPause();
        });

        btnPrev.setOnClickListener(v -> musicViewModel.skipToPrevious());
        btnNext.setOnClickListener(v -> musicViewModel.skipToNext());

        btnLike.setOnClickListener(v -> { if (currentActiveSong != null) musicViewModel.toggleLike(currentActiveSong); });

        btnRepeat.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.getRepeatMode() == Player.REPEAT_MODE_OFF) {
                    mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
                    btnRepeat.setColorFilter(currentAccentColor);
                    Toast.makeText(getContext(), "Repeat: On", Toast.LENGTH_SHORT).show();
                } else {
                    mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
                    btnRepeat.setColorFilter(Color.parseColor("#8A8D93"));
                    Toast.makeText(getContext(), "Repeat: Off", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSeekBar() {
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    mediaController.seekTo(progress);
                    tvTimeCurrent.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { isTrackingSeekBar = true; }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingSeekBar = false;
                if (Boolean.TRUE.equals(musicViewModel.getIsJamHost().getValue()) && currentActiveSong != null && mediaController != null) {
                    musicViewModel.sendHostUpdate(currentActiveSong, mediaController.isPlaying(), mediaController.getCurrentPosition());
                }
            }
        });
    }

    private void updatePlayPauseUI(boolean isPlaying) { ivPlayPauseIcon.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play); }
    private void updateLikeButtonUI(boolean isLiked) { btnLike.setColorFilter(isLiked ? currentAccentColor : Color.parseColor("#8A8D93")); }

    private void loadHighResThumbnail(String url) {
        if (url != null) {
            if (url.startsWith("//")) url = "https:" + url;
            url = url.replace("150x150", "500x500").replace("50x50", "500x500");
            url = url.replace("100x100bb", "1000x1000bb");
            url = url.replace("hqdefault.jpg", "maxresdefault.jpg");
        }
        ivHugeThumbnail.setImageTintList(null); ivHugeThumbnail.clearColorFilter();
        Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).transition(DrawableTransitionOptions.withCrossFade(400)).into(ivHugeThumbnail);
    }

    // ==========================================
    // THE FLAWLESS SSOT RESOLUTION ENGINE
    // ==========================================
    private void startJioSaavnSSOTProtocol(Song song) {
        resolvedStreamUrl = null;
        if (mediaController != null) { mediaController.stop(); mediaController.clearMediaItems(); }
        btnPlayPause.setAlpha(0.5f); updatePlayPauseUI(false);

        if (activeNetworkFuture != null && !activeNetworkFuture.isDone()) activeNetworkFuture.cancel(true);

        activeNetworkFuture = networkExecutor.submit(() -> {
            try {
                String streamUrl = null;

                // 1. Check if we already hold the master key (Instant Playback)
                if (song.getStreamUrl() != null && song.getStreamUrl().startsWith("http")) {
                    streamUrl = song.getStreamUrl();
                }
                // 2. Fetch purely by ID (100% Deterministic)
                else {
                    streamUrl = fetchStreamUrlById(song.getId());
                    if (streamUrl != null) song.setStreamUrl(streamUrl); // Cache it forever
                }

                if (streamUrl != null) {
                    // Strip the expiring token parameters if possible, keeping the core URL safe
                    String safeUrl = streamUrl.replaceAll("&range=[^&]+", "").replaceAll("&rn=[^&]+", "");
                    resolvedStreamUrl = safeUrl;
                    passUrlToService(safeUrl, song);
                }
            } catch (Exception e) {
                Log.e("PlayerSSOT", "Failed to resolve stream", e);
                mainThreadHandler.post(() -> Toast.makeText(getContext(), "Failed to resolve stream. Check internet.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // Direct ID mapping, eliminating YouTube/Apple discrepancies
    private String fetchStreamUrlById(String songId) throws Exception {
        String cleanId = songId.replace("saavn_", ""); // Fallback cleanup just in case

        // Primary Unofficial API Endpoint
        Request request = new Request.Builder()
                .url("https://saavn.dev/api/songs?ids=" + cleanId)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JSONObject json = new JSONObject(response.body().string());
                JSONArray data = json.optJSONArray("data");

                if (data != null && data.length() > 0) {
                    JSONArray downloadUrls = data.getJSONObject(0).optJSONArray("downloadUrl");
                    if (downloadUrls != null && downloadUrls.length() > 0) {
                        // Grab highest quality 320kbps URL (usually the last in the array)
                        return downloadUrls.getJSONObject(downloadUrls.length() - 1).getString("url");
                    }
                }
            }
        }

        // Backup Node Routing
        Request backupReq = new Request.Builder()
                .url("https://jiosaavn-api-privatecvc2.vercel.app/songs?id=" + cleanId)
                .get().build();

        try (Response backupRes = httpClient.newCall(backupReq).execute()) {
            if (backupRes.isSuccessful() && backupRes.body() != null) {
                JSONObject json = new JSONObject(backupRes.body().string());
                JSONArray data = json.optJSONArray("data");
                if (data != null && data.length() > 0) {
                    JSONArray downloadUrls = data.getJSONObject(0).optJSONArray("downloadUrl");
                    if (downloadUrls != null && downloadUrls.length() > 0) {
                        return downloadUrls.getJSONObject(downloadUrls.length() - 1).getString("link");
                    }
                }
            }
        }

        throw new Exception("SSOT Engine Failed to resolve ID: " + songId);
    }

    private void passUrlToService(String rawUrl, Song song) {
        mainThreadHandler.post(() -> {
            if (mediaController == null) return;
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(song.getTitle())
                    .setArtist(song.getArtist())
                    .setAlbumTitle(song.getAlbum())
                    .setArtworkUri(Uri.parse(song.getThumbnailUrl()))
                    .build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(rawUrl)
                    .setMediaId(song.getId())
                    .setMediaMetadata(metadata)
                    .build();

            mediaController.setMediaItem(mediaItem);
            mediaController.prepare();
            if (Boolean.TRUE.equals(musicViewModel.getIsPlaying().getValue())) mediaController.play();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainThreadHandler.removeCallbacks(syncRunnable);
        if (activeNetworkFuture != null) activeNetworkFuture.cancel(true);
        if (controllerFuture != null) { MediaController.releaseFuture(controllerFuture); mediaController = null; }
    }
}
