package com.example.crossTune;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class PlaylistsBottomSheet extends BottomSheetDialogFragment {

    private SharedMusicViewModel musicViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = view.findViewById(R.id.rv_bottom_playlists);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);

        SimpleAdapter adapter = new SimpleAdapter(playlist -> {
            Song current = musicViewModel.getCurrentSong().getValue();
            if (current != null) {
                musicViewModel.addSongToPlaylist(playlist.getId(), current);
                Toast.makeText(getContext(), "Added to " + playlist.getName(), Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });

        rv.setAdapter(adapter);

        musicViewModel.getPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null) adapter.setList(playlists);
        });
    }

    // FIXED: Added 'static' keyword here
    private static class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.Holder> {
        private List<Playlist> list = new ArrayList<>();
        private final OnClick listener;

        interface OnClick { void click(Playlist p); }

        SimpleAdapter(OnClick l) { listener = l; }

        void setList(List<Playlist> l) { list = l; notifyDataSetChanged(); }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(18);
            tv.setPadding(0, 32, 0, 32);
            return new Holder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Playlist p = list.get(position);
            holder.tv.setText("🎵 " + p.getName());
            holder.tv.setOnClickListener(v -> listener.click(p));
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder { // Also added static here for good measure
            TextView tv;
            Holder(@NonNull View v) { super(v); tv = (TextView) v; }
        }
    }
}