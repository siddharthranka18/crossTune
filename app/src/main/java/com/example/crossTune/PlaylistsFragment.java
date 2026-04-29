package com.example.crossTune;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private View viewMaster, viewDetail;
    private RecyclerView rvPlaylists, rvPlaylistSongs;
    private TextView tvDetailTitle;

    private SharedMusicViewModel musicViewModel;
    private PlaylistAdapter playlistAdapter;
    private SongAdapter songAdapter;

    // Track the currently opened playlist for the Queue Context Engine
    private Playlist currentActivePlaylist;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewMaster = view.findViewById(R.id.view_master);
        viewDetail = view.findViewById(R.id.view_detail);
        rvPlaylists = view.findViewById(R.id.rv_playlists);
        rvPlaylistSongs = view.findViewById(R.id.rv_playlist_songs);
        tvDetailTitle = view.findViewById(R.id.tv_detail_title);

        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);

        // Grid for Master
        rvPlaylists.setLayoutManager(new GridLayoutManager(getContext(), 2));
        playlistAdapter = new PlaylistAdapter(playlist -> openPlaylist(playlist));
        rvPlaylists.setAdapter(playlistAdapter);

        // List for Songs inside a Playlist
        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(getContext()));

        // INTELLIGENCE INJECTED: Tapping a song now feeds the entire playlist to the Queue Engine
        songAdapter = new SongAdapter(song -> {
            if (currentActivePlaylist != null) {
                musicViewModel.playSongWithContext(song, currentActivePlaylist.getSongs(), currentActivePlaylist.getName());

                // Open the full screen player dynamically
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openFullScreenPlayer();
                }
            }
        });
        rvPlaylistSongs.setAdapter(songAdapter);

        // Observe Data
        musicViewModel.getPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null) {
                playlistAdapter.setPlaylists(playlists);

                // If a playlist is currently open, dynamically refresh its songs (e.g., if a user un-liked a song while here)
                if (currentActivePlaylist != null) {
                    for (Playlist p : playlists) {
                        if (p.getId().equals(currentActivePlaylist.getId())) {
                            currentActivePlaylist = p;
                            songAdapter.setSongs(p.getSongs());
                            break;
                        }
                    }
                }
            }
        });

        // Add Playlist Button
        view.findViewById(R.id.btn_add_playlist).setOnClickListener(v -> showCreateDialog());
        // Back Button
        view.findViewById(R.id.btn_back).setOnClickListener(v -> closePlaylist());
    }

    private void openPlaylist(Playlist playlist) {
        currentActivePlaylist = playlist;
        viewMaster.setVisibility(View.GONE);
        viewDetail.setVisibility(View.VISIBLE);
        tvDetailTitle.setText(playlist.getName());
        songAdapter.setSongs(playlist.getSongs());
    }

    private void closePlaylist() {
        currentActivePlaylist = null;
        viewDetail.setVisibility(View.GONE);
        viewMaster.setVisibility(View.VISIBLE);
    }

    private void showCreateDialog() {
        EditText input = new EditText(getContext());
        input.setTextColor(Color.WHITE);
        input.setHint("Playlist Name, Apple or YT Link"); // Updated hint
        input.setHintTextColor(Color.parseColor("#5E6168"));

        new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("New Playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();

                    // ==========================================
                    // 1. APPLE MUSIC DETECTED
                    // ==========================================
                    if (name.contains("apple")) {
                        Toast.makeText(getContext(), "Fetching Apple Playlist...", Toast.LENGTH_SHORT).show();
                        Apple.fetchApplePlaylist(name, new Apple.AppleCallback() {
                            @Override
                            public void onSuccess(String playlistName, String rawSongsText) {
                                PlaylistBulkImporter importer = new PlaylistBulkImporter(musicViewModel);
                                importer.importSongsToNewPlaylist(playlistName, rawSongsText, new PlaylistBulkImporter.ImportCallback() {
                                    @Override
                                    public void onProgress(int current, int total, String songName) {}
                                    @Override
                                    public void onComplete() {
                                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Imported: " + playlistName, Toast.LENGTH_LONG).show());
                                    }
                                    @Override
                                    public void onError(String error) {
                                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Import Error: " + error, Toast.LENGTH_LONG).show());
                                    }
                                });
                            }
                            @Override
                            public void onError(String errorMsg) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed: " + errorMsg, Toast.LENGTH_LONG).show());
                            }
                        });
                    }
                    // ==========================================
                    // 2. YOUTUBE MUSIC DETECTED
                    // ==========================================
                    else if (name.contains("youtube") || name.contains("youtu.be")) {
                        Toast.makeText(getContext(), "Fetching YouTube Playlist...", Toast.LENGTH_SHORT).show();
                        Youtube.fetchYoutubePlaylist(name, new Youtube.YoutubeCallback() {
                            @Override
                            public void onSuccess(String playlistName, String rawSongsText) {
                                PlaylistBulkImporter importer = new PlaylistBulkImporter(musicViewModel);
                                importer.importSongsToNewPlaylist(playlistName, rawSongsText, new PlaylistBulkImporter.ImportCallback() {
                                    @Override
                                    public void onProgress(int current, int total, String songName) {}
                                    @Override
                                    public void onComplete() {
                                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Imported: " + playlistName, Toast.LENGTH_LONG).show());
                                    }
                                    @Override
                                    public void onError(String error) {
                                        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Import Error: " + error, Toast.LENGTH_LONG).show());
                                    }
                                });
                            }
                            @Override
                            public void onError(String errorMsg) {
                                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed: " + errorMsg, Toast.LENGTH_LONG).show());
                            }
                        });
                    }
                    // ==========================================
                    // 3. STANDARD PLAYLIST CREATION
                    // ==========================================
                    else if (!name.isEmpty()) {
                        musicViewModel.createPlaylist(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==============================================================
    // THE MASTERPIECE: DYNAMIC TRI-STATE ADAPTER
    // ==============================================================
    private static class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.Holder> {
        private List<Playlist> lists = new ArrayList<>();
        private final OnClick listener;

        interface OnClick { void click(Playlist p); }

        PlaylistAdapter(OnClick l) { listener = l; }

        void setPlaylists(List<Playlist> newLists) {
            this.lists = newLists;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Playlist p = lists.get(position);
            List<Song> songs = p.getSongs();

            holder.name.setText(p.getName());

            // 1. INSTANT LOAD: Calculate statistics locally using existing session data
            int size = songs.size();
            long localSeconds = 0;
            for (Song s : songs) {
                localSeconds += s.getDuration();
            }
            long localMins = localSeconds / 60;

            // Show local data immediately so there is no "Loading..." flicker
            holder.count.setText(size + " / " + localMins + " mins");

            // 2. BACKGROUND SYNC: Call DB via Stored Procedure (Point 12) to verify data
            DB.getPlaylistSummary(p.getId(), (dbCount, dbMins) -> {
                // BUG FIX: Only update if the database actually returned data.
                // Sometimes the DB sync is slower than the local memory, which caused "0 / 0" flicker.
                if (dbCount > 0) {
                    holder.count.setText(dbCount + " / " + dbMins + " mins");
                }
            });

            // Clear Glide bindings so recycled views don't show wrong images briefly
            Context ctx = holder.itemView.getContext();
            Glide.with(ctx).clear(holder.ivSingleCover);
            Glide.with(ctx).clear(holder.ivCol1);
            Glide.with(ctx).clear(holder.ivCol2);
            Glide.with(ctx).clear(holder.ivCol3);
            Glide.with(ctx).clear(holder.ivCol4);

            // STATE LOGIC
            if (size == 0) {
                // State 1: Empty
                holder.ivEmptyState.setVisibility(View.VISIBLE);
                holder.ivSingleCover.setVisibility(View.GONE);
                holder.layoutCollage.setVisibility(View.GONE);

            } else if (size < 4) {
                // State 2: Single Cover (Uses the first song's art)
                holder.ivEmptyState.setVisibility(View.GONE);
                holder.ivSingleCover.setVisibility(View.VISIBLE);
                holder.layoutCollage.setVisibility(View.GONE);

                loadCover(ctx, songs.get(0).getThumbnailUrl(), holder.ivSingleCover);

            } else {
                // State 3: Beautiful 2x2 Collage
                holder.ivEmptyState.setVisibility(View.GONE);
                holder.ivSingleCover.setVisibility(View.GONE);
                holder.layoutCollage.setVisibility(View.VISIBLE);

                loadCover(ctx, songs.get(0).getThumbnailUrl(), holder.ivCol1);
                loadCover(ctx, songs.get(1).getThumbnailUrl(), holder.ivCol2);
                loadCover(ctx, songs.get(2).getThumbnailUrl(), holder.ivCol3);
                loadCover(ctx, songs.get(3).getThumbnailUrl(), holder.ivCol4);
            }

            holder.itemView.setOnClickListener(v -> listener.click(p));
        }

        @Override
        public int getItemCount() { return lists.size(); }

        // Helper to upscale the URLs and load them smoothly
        private void loadCover(Context ctx, String url, ImageView iv) {
            if (url != null) {
                if (url.startsWith("//")) url = "https:" + url;
                url = url.replace("150x150", "500x500").replace("50x50", "500x500").replace("100x100bb", "600x600bb");
            }
            Glide.with(ctx)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(iv);
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView name, count;
            ImageView ivEmptyState, ivSingleCover, ivCol1, ivCol2, ivCol3, ivCol4;
            View layoutCollage;

            Holder(@NonNull View v) {
                super(v);
                name = v.findViewById(R.id.tv_playlist_name);
                count = v.findViewById(R.id.tv_playlist_count);

                ivEmptyState = v.findViewById(R.id.iv_empty_state);
                ivSingleCover = v.findViewById(R.id.iv_single_cover);
                layoutCollage = v.findViewById(R.id.layout_collage);

                ivCol1 = v.findViewById(R.id.iv_collage_1);
                ivCol2 = v.findViewById(R.id.iv_collage_2);
                ivCol3 = v.findViewById(R.id.iv_collage_3);
                ivCol4 = v.findViewById(R.id.iv_collage_4);
            }
        }
    }
}
