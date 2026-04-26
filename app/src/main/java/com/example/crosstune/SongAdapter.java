package com.example.crosstune;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList = new ArrayList<>();
    private final OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SongAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> newSongs) {
        this.songList = newSongs;
        notifyDataSetChanged();
    }

    public void addSongs(List<Song> moreSongs) {
        int startPosition = this.songList.size();
        this.songList.addAll(moreSongs);
        notifyItemRangeInserted(startPosition, moreSongs.size());
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.bind(song, listener);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvArtist;
        private final ImageView ivThumbnail;
        private final View rootView;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView;
            tvTitle = itemView.findViewById(R.id.tv_song_name);
            tvArtist = itemView.findViewById(R.id.tv_artist_name);
            ivThumbnail = itemView.findViewById(R.id.iv_song_thumbnail);
        }

        public void bind(Song song, OnSongClickListener listener) {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());

            // THE FIX: Violently remove the XML app:tint="#303340" so the image is visible!
            ivThumbnail.setImageTintList(null);
            ivThumbnail.clearColorFilter();

            String cleanUrl = song.getThumbnailUrl();
            if (cleanUrl != null) {
                if (cleanUrl.startsWith("//")) cleanUrl = "https:" + cleanUrl;
                // Force Ultra HD without breaking older formats
                cleanUrl = cleanUrl.replace("150x150", "500x500").replace("50x50", "500x500");
                cleanUrl = cleanUrl.replace("100x100bb", "600x600bb");
            }

            Glide.with(itemView.getContext())
                    .load(cleanUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache forever
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .error(R.drawable.ic_logo)
                    .into(ivThumbnail);

            rootView.setOnClickListener(v -> listener.onSongClick(song));
        }


    }
}