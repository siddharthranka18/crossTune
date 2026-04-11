package com.example.crosstune;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.LibraryViewHolder> {

    private List<LibraryModel> libraryList;

    public LibraryAdapter(List<LibraryModel> libraryList) {
        this.libraryList = libraryList;
    }

    @NonNull
    @Override
    public LibraryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_library_card, parent, false);
        return new LibraryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryViewHolder holder, int position) {
        LibraryModel item = libraryList.get(position);
        if (item != null) {
            holder.titleText.setText(item.getTitle());
            holder.artistText.setText(item.getArtist());
            holder.albumImage.setImageResource(item.getImageResource());
        }
    }

    @Override
    public int getItemCount() {
        return libraryList != null ? libraryList.size() : 0;
    }

    public static class LibraryViewHolder extends RecyclerView.ViewHolder {
        ImageView albumImage;
        TextView titleText, artistText;

        public LibraryViewHolder(@NonNull View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.imgLibraryAlbum);
            titleText = itemView.findViewById(R.id.txtLibraryTitle);
            artistText = itemView.findViewById(R.id.txtLibraryArtist);
        }
    }
}