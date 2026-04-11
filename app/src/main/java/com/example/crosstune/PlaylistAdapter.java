package com.example.crosstune;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {
    private final List<PlaylistModel> list;
    private final Context context;

    public PlaylistAdapter(Context context, List<PlaylistModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaylistModel model = list.get(position);

        holder.title.setText(model.title);
        holder.artist.setText(model.artist);
        holder.image.setImageResource(model.image);

        // Ensure the overlay is visible so text is readable
        if (holder.overlay != null) {
            holder.overlay.setVisibility(View.VISIBLE);
        }

        if (holder.playButton != null) {
            holder.playButton.setVisibility(View.VISIBLE);
            holder.playButton.bringToFront();

            holder.playButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, NowPlayingActivity.class);
                intent.putExtra(NowPlayingActivity.EXTRA_SONG_TITLE, model.title);
                intent.putExtra(NowPlayingActivity.EXTRA_ARTIST_NAME, model.artist);
                intent.putExtra(NowPlayingActivity.EXTRA_ALBUM_IMAGE, model.image);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image, playButton;
        TextView title, artist;
        View overlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.playlistImage);
            title = itemView.findViewById(R.id.playlistTitle);
            artist = itemView.findViewById(R.id.playlistArtist);
            playButton = itemView.findViewById(R.id.playButton);

            // FIXED: ID must match your XML (gradientOverlay)
            overlay = itemView.findViewById(R.id.gradientOverlay);
        }
    }
}