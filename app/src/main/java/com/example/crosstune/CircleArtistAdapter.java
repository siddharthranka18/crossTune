package com.example.crosstune;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class CircleArtistAdapter extends RecyclerView.Adapter<CircleArtistAdapter.ViewHolder> {

    private List<LibraryModel> list;

    public CircleArtistAdapter(List<LibraryModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflates the item_circle.xml layout we made for the circular artists
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_circle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryModel item = list.get(position);
        holder.name.setText(item.getTitle());
        holder.image.setImageResource(item.getImageResource());
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView image;
        TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match your item_circle.xml
            image = itemView.findViewById(R.id.imgArtistCircle);
            name = itemView.findViewById(R.id.txtArtistName);
        }
    }
}