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
import com.bumptech.glide.Glide;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.MyViewHolder> {
    Context context;
    List<PlaylistModel> list;

    public PlaylistAdapter(Context context, List<PlaylistModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.playlist_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        PlaylistModel model = list.get(position);
        holder.title.setText(model.getTitle());
        holder.artist.setText(model.getArtist());

        Glide.with(context)
                .load(model.getImage())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image);

        // --- UPDATED: Click Listener to open PlaylistActivity ---
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PlaylistActivity.class);
            intent.putExtra("playlist_title", model.getTitle());
            intent.putExtra("playlist_image", model.getImage());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, artist;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.playlistImage);
            title = itemView.findViewById(R.id.playlistTitle);
            artist = itemView.findViewById(R.id.playlistArtist);
        }
    }
}