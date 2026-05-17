package com.example.crossTune;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchFragment extends Fragment {

    private EditText etSearchInput;
    private ImageView btnExecuteSearch, btnClearSearch;
    private NestedScrollView layoutDiscoverContent;
    private RecyclerView rvTrending, rvTopPicks, rvCurated, rvSearchResults;
    private ProgressBar pbInfiniteLoading;
    private TextView tvHeaderSearch;

    private SongAdapter searchAdapter;
    private DiscoverAdapter trendingAdapter, topPicksAdapter, curatedAdapter;

    private SharedMusicViewModel musicViewModel;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(4);
    private Future<?> activeSearchFuture;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private List<Song> latestTrending = new ArrayList<>();
    private List<Song> latestTopPicks = new ArrayList<>();
    private List<Song> latestCurated = new ArrayList<>();
    private List<Song> latestSearchResults = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupAdapters();
        setupListeners();
        loadIntelligentDiscoverFeed();
    }

    private void initViews(View view) {
        tvHeaderSearch = view.findViewById(R.id.tv_header_search);
        etSearchInput = view.findViewById(R.id.et_search_input);
        btnExecuteSearch = view.findViewById(R.id.btn_execute_search);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        layoutDiscoverContent = view.findViewById(R.id.layout_discover_content);
        rvTrending = view.findViewById(R.id.rv_trending);
        rvTopPicks = view.findViewById(R.id.rv_top_picks);
        rvCurated = view.findViewById(R.id.rv_curated_playlists);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        pbInfiniteLoading = view.findViewById(R.id.pb_infinite_loading);
    }

    private void setupAdapters() {
        musicViewModel = new ViewModelProvider(requireActivity()).get(SharedMusicViewModel.class);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTrending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTopPicks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCurated.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        searchAdapter = new SongAdapter(song -> musicViewModel.playSongWithContext(song, latestSearchResults, "Search Results"));
        trendingAdapter = new DiscoverAdapter(song -> musicViewModel.playSongWithContext(song, latestTrending, "Trending Now"));
        topPicksAdapter = new DiscoverAdapter(song -> musicViewModel.playSongWithContext(song, latestTopPicks, "On Repeat"));
        curatedAdapter = new DiscoverAdapter(song -> musicViewModel.playSongWithContext(song, latestCurated, "Made For You"));

        rvSearchResults.setAdapter(searchAdapter);
        rvTrending.setAdapter(trendingAdapter);
        rvTopPicks.setAdapter(topPicksAdapter);
        rvCurated.setAdapter(curatedAdapter);
    }

    private void setupListeners() {
        btnExecuteSearch.setOnClickListener(v -> initiateSearch());
        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                initiateSearch();
                return true;
            }
            return false;
        });
        btnClearSearch.setOnClickListener(v -> {
            etSearchInput.setText("");
            showDiscoverState();
        });
    }

    private void initiateSearch() {
        String query = etSearchInput.getText().toString().trim();
        if (query.isEmpty()) return;
        hideKeyboard();
        showSearchState();
        executeSearchTask(query);
    }

    private void executeSearchTask(String query) {
        if (activeSearchFuture != null) activeSearchFuture.cancel(true);
        pbInfiniteLoading.setVisibility(View.VISIBLE);
        searchAdapter.setSongs(new ArrayList<>());
        
        activeSearchFuture = networkExecutor.submit(() -> {
            try {
                List<Song> results = fetchSongs(query);
                mainThreadHandler.post(() -> {
                    latestSearchResults = results;
                    searchAdapter.setSongs(results);
                    pbInfiniteLoading.setVisibility(View.GONE);
                    if (results.isEmpty()) {
                        Toast.makeText(getContext(), "Server returned no results.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("SearchFragment", "Search process crashed", e);
                mainThreadHandler.post(() -> {
                    pbInfiniteLoading.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadIntelligentDiscoverFeed() {
        networkExecutor.submit(() -> {
            try {
                latestTrending = fetchSongs("trending hindi");
                latestTopPicks = fetchSongs("new releases");
                latestCurated = fetchSongs("hits 2024");
                mainThreadHandler.post(() -> {
                    trendingAdapter.setSongs(latestTrending);
                    topPicksAdapter.setSongs(latestTopPicks);
                    curatedAdapter.setSongs(latestCurated);
                });
            } catch (Exception e) { Log.e("SearchFragment", "Feed failed", e); }
        });
    }

    private List<Song> fetchSongs(String query) throws Exception {
        String[] apis = {
            "https://saavn.dev/api/search/songs?query=",
            "https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query="
        };

        for (String baseUrl : apis) {
            try {
                String url = baseUrl + URLEncoder.encode(query, "UTF-8");
                Request request = new Request.Builder().url(url).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) continue;
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    
                    JSONArray results = findResultsArray(json);
                    if (results != null && results.length() > 0) {
                        return parseResults(results);
                    }
                }
            } catch (Exception e) { Log.e("SearchFragment", "API failed: " + baseUrl); }
        }
        return new ArrayList<>();
    }

    private JSONArray findResultsArray(JSONObject json) {
        // Deep search for results array across different API versions
        if (json.has("data")) {
            Object data = json.opt("data");
            if (data instanceof JSONObject) {
                JSONObject dataObj = (JSONObject) data;
                if (dataObj.has("results")) return dataObj.optJSONArray("results");
                if (dataObj.has("songs")) return dataObj.optJSONArray("songs");
            } else if (data instanceof JSONArray) {
                return (JSONArray) data;
            }
        }
        if (json.has("results")) return json.optJSONArray("results");
        return null;
    }

    private List<Song> parseResults(JSONArray results) {
        List<Song> songs = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            try {
                JSONObject item = results.getJSONObject(i);
                String id = item.optString("id", "");
                if (id.isEmpty()) continue;

                String title = item.optString("name", item.optString("title", "Unknown Track"));
                
                String artist = "Various Artists";
                if (item.has("artists")) {
                    Object a = item.opt("artists");
                    if (a instanceof JSONObject) {
                        JSONArray primary = ((JSONObject) a).optJSONArray("primary");
                        if (primary != null && primary.length() > 0) artist = primary.getJSONObject(0).optString("name", artist);
                    } else if (a instanceof JSONArray) {
                        JSONArray arr = (JSONArray) a;
                        if (arr.length() > 0) artist = arr.getJSONObject(0).optString("name", artist);
                    }
                } else if (item.has("primaryArtists")) {
                    artist = item.optString("primaryArtists", artist);
                }

                String album = "Unknown Album";
                if (item.has("album")) {
                    Object alb = item.opt("album");
                    if (alb instanceof JSONObject) album = ((JSONObject) alb).optString("name", album);
                    else album = String.valueOf(alb);
                }

                String thumb = "";
                if (item.has("image")) {
                    JSONArray images = item.optJSONArray("image");
                    if (images != null && images.length() > 0) {
                        thumb = images.getJSONObject(images.length() - 1).optString("url", images.getJSONObject(images.length() - 1).optString("link", ""));
                    }
                }

                Song song = new Song(id, title, artist, album, thumb, item.optLong("duration", 0));
                songs.add(song);
            } catch (Exception e) { Log.e("SearchFragment", "Parse item failed"); }
        }
        return songs;
    }

    private void showSearchState() { layoutDiscoverContent.setVisibility(View.GONE); rvSearchResults.setVisibility(View.VISIBLE); tvHeaderSearch.setText("Results"); }
    private void showDiscoverState() { rvSearchResults.setVisibility(View.GONE); layoutDiscoverContent.setVisibility(View.VISIBLE); tvHeaderSearch.setText("Discover"); }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static class DiscoverAdapter extends RecyclerView.Adapter<DiscoverAdapter.Holder> {
        private List<Song> list = new ArrayList<>();
        private final SongAdapter.OnSongClickListener listener;
        DiscoverAdapter(SongAdapter.OnSongClickListener listener) { this.listener = listener; }
        void setSongs(List<Song> songs) { this.list = songs; notifyDataSetChanged(); }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discover_card, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            Song s = list.get(position);
            holder.title.setText(s.getTitle());
            holder.artist.setText(s.getArtist());
            Glide.with(holder.itemView).load(s.getThumbnailUrl()).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.thumbnail);
            holder.itemView.setOnClickListener(v -> listener.onSongClick(s));
        }
        @Override public int getItemCount() { return list.size(); }
        static class Holder extends RecyclerView.ViewHolder {
            ImageView thumbnail; TextView title, artist;
            Holder(View v) { super(v); thumbnail = v.findViewById(R.id.iv_discover_thumbnail); title = v.findViewById(R.id.tv_discover_title); artist = v.findViewById(R.id.tv_discover_artist); }
        }
    }
}
