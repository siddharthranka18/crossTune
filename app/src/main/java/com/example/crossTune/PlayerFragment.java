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

    private ImageView ivHugeThumbnail, ivPlayPauseIcon, btnPrev, btnNext, btnLike, btnRepeat, btnCollapse, btnDownload, ivJamIcon;
    private MaterialCardView btnPlayPause;
    private TextView tvSongName, tvArtistName, tvTimeCurrent, tvTimeTotal, tvJamStatus;
    private TextView tvPlayingContext, tvContextLabel; 
    private SeekBar seekBarProgress;
    private LinearLayout bottomActionBar, btnJamHost, layoutPlayingContext;

    private SharedMusicViewModel musicViewModel;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    private Song currentActiveSong;
    private String resolvedStreamUrl;
    private String currentSongId = "";

    private boolean isTrackingSeekBar = false;
    private int currentAccentColor = Color.WHITE;

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private Future<?> activeNetworkFuture;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();

    private final Runnable syncRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                if (!isTrackingSeekBar) {
                    long duration = mediaController.getDuration();
                    long current = mediaController.getCurrentPosition();
                    if (duration > 0) {
                        if (seekBarProgress.getMax() != (int) duration) {
                            seekBarProgress.setMax((int) duration);
                        }
                        seekBarProgress.setProgress((int) current);
                        tvTimeCurrent.setText(formatTime(current));
                        tvTimeTotal.setText(formatTime(duration));
                    }
                }
            }
            mainThreadHandler.postDelayed(this, 16);
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
        if (millis <= 0) return "00:00";
        long seconds = millis / 1000;
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
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
                            updatePlayPauseUI(mediaController.isPlaying());
                        } else if (state == Player.STATE_BUFFERING) {
                            btnPlayPause.setAlpha(0.5f);
                        }
                    }
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        updatePlayPauseUI(isPlaying);
                        if (seekBarProgress instanceof SquigglySeekBar) {
                            ((SquigglySeekBar) seekBarProgress).setWavy(isPlaying);
                        }
                    }
                });
            } catch (Exception e) { Log.e("PlayerFragment", "Controller failed", e); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setupViewModel() {
        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);
        musicViewModel.getCurrentSong().observe(getViewLifecycleOwner(), song -> {
            if (song != null && !song.getId().equals(currentSongId)) {
                currentSongId = song.getId();
                currentActiveSong = song;
                tvSongName.setText(song.getTitle());
                tvArtistName.setText(song.getArtist());
                seekBarProgress.setProgress(0);
                tvTimeCurrent.setText("00:00");
                loadHighResThumbnail(song.getThumbnailUrl());
                startStreamResolution(song);
            }
        });
        musicViewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (mediaController != null) {
                if (isPlaying) mediaController.play();
                else mediaController.pause();
            }
        });
    }

    private void setupClickListeners() {
        btnCollapse.setOnClickListener(v -> { if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).closeFullScreenPlayer(); });
        btnPlayPause.setOnClickListener(v -> musicViewModel.togglePlayPause());
        btnPrev.setOnClickListener(v -> musicViewModel.skipToPrevious());
        btnNext.setOnClickListener(v -> musicViewModel.skipToNext());
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
            public void onStopTrackingTouch(SeekBar seekBar) { isTrackingSeekBar = false; }
        });
    }

    private void updatePlayPauseUI(boolean isPlaying) {
        ivPlayPauseIcon.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        if (seekBarProgress instanceof SquigglySeekBar) {
            ((SquigglySeekBar) seekBarProgress).setWavy(isPlaying);
        }
    }

    private void loadHighResThumbnail(String url) {
        if (url != null) url = url.replace("150x150", "500x500").replace("50x50", "500x500");
        Glide.with(this).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).into(ivHugeThumbnail);
    }

    private void startStreamResolution(Song song) {
        resolvedStreamUrl = null;
        if (mediaController != null) { mediaController.stop(); mediaController.clearMediaItems(); }
        btnPlayPause.setAlpha(0.3f);
        if (activeNetworkFuture != null) activeNetworkFuture.cancel(true);

        activeNetworkFuture = networkExecutor.submit(() -> {
            try {
                // FORCE FRESH RESOLUTION - Old links in search are likely dead (404)
                String streamUrl = resolveWithRetry(song.getId());
                if (streamUrl != null) {
                    resolvedStreamUrl = streamUrl;
                    mainThreadHandler.post(() -> passUrlToService(streamUrl, song));
                } else {
                    mainThreadHandler.post(() -> Toast.makeText(getContext(), "Song unavailable on server.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("PlayerFragment", "Resolution failed", e);
            }
        });
    }

    private String resolveWithRetry(String songId) {
        String cleanId = songId.replace("saavn_", "");
        String[] apis = {
            "https://saavn.dev/api/songs?ids=",
            "https://jiosaavn-api-privatecvc2.vercel.app/songs?id="
        };
        for (String baseUrl : apis) {
            try {
                Request request = new Request.Builder().url(baseUrl + cleanId).get().build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray data = json.optJSONArray("data");
                        if (data == null) data = json.optJSONArray("results");
                        if (data != null && data.length() > 0) {
                            JSONArray urls = data.getJSONObject(0).optJSONArray("downloadUrl");
                            if (urls != null && urls.length() > 0) {
                                JSONObject best = urls.getJSONObject(urls.length() - 1);
                                return best.optString("link", best.optString("url", ""));
                            }
                        }
                    }
                }
            } catch (Exception e) { Log.e("PlayerFragment", "API failed: " + baseUrl); }
        }
        return null;
    }

    private void passUrlToService(String url, Song song) {
        if (mediaController == null) return;
        MediaItem item = new MediaItem.Builder()
                .setUri(url)
                .setMediaId(song.getId())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(song.getTitle())
                        .setArtist(song.getArtist())
                        .setArtworkUri(Uri.parse(song.getThumbnailUrl()))
                        .build())
                .build();
        mediaController.setMediaItem(item);
        mediaController.prepare();
        mediaController.play();
        btnPlayPause.setAlpha(1.0f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainThreadHandler.removeCallbacks(syncRunnable);
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
    }
}
