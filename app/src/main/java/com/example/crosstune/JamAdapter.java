package com.example.crosstune;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class JamAdapter extends RecyclerView.Adapter<JamAdapter.ViewHolder> {

    Context context;
    List<JamModel> list;

    public JamAdapter(Context context, List<JamModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        JamModel model = list.get(position);

        holder.image.setImageResource(model.image);
        holder.title.setText(model.title);
        holder.artist.setText(model.artist);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title, artist;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.playlistImage);
            title = itemView.findViewById(R.id.playlistTitle);
            artist = itemView.findViewById(R.id.playlistArtist);
        }
    }
}