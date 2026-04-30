package com.example.crossTune;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchFragment extends Fragment {

    // UI Elements
    private EditText etSearchInput;
    private ImageView btnExecuteSearch, btnClearSearch;
    private NestedScrollView layoutDiscoverContent;
    private RecyclerView rvTrending, rvTopPicks, rvCurated, rvSearchResults;
    private ProgressBar pbInfiniteLoading;
    private TextView tvHeaderSearch;

    // Adapters
    private SongAdapter searchAdapter;
    private DiscoverAdapter trendingAdapter, topPicksAdapter, curatedAdapter;

    // Architecture
    private SharedMusicViewModel musicViewModel;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // SSOT Network Engine
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(4);
    private Future<?> activeSearchFuture;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(18, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    // Infinite Scroll & Context State
    private String currentSearchQuery = "";
    private int currentSearchPage = 1;
    private boolean isFetchingMore = false;
    private boolean hasMoreResults = true;

    // UI Lists
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

        // Trigger the Discover Feed
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

        searchAdapter = new SongAdapter(song -> playSong(song, latestSearchResults, "Search Results"));
        trendingAdapter = new DiscoverAdapter(song -> playSong(song, latestTrending, "Trending Now"));
        topPicksAdapter = new DiscoverAdapter(song -> playSong(song, latestTopPicks, "On Repeat"));
        curatedAdapter = new DiscoverAdapter(song -> playSong(song, latestCurated, "Made For You"));

        rvSearchResults.setAdapter(searchAdapter);
        rvTrending.setAdapter(trendingAdapter);
        rvTopPicks.setAdapter(topPicksAdapter);
        rvCurated.setAdapter(curatedAdapter);
    }

    private void playSong(Song song, List<Song> contextList, String contextName) {
        musicViewModel.playSongWithContext(song, contextList, contextName);
    }

    private void setupListeners() {
        btnExecuteSearch.setOnClickListener(v -> initiateSearch());

        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                initiateSearch();
                return true;
            }
            return false;
        });

        etSearchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchInput.setText("");
            hideKeyboard();
            showDiscoverState();
        });

        rvSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                    if (!isFetchingMore && hasMoreResults) {
                        loadMoreResults();
                    }
                }
            }
        });
    }

    private void showSearchState() {
        tvHeaderSearch.setText("Results");
        if (layoutDiscoverContent.getVisibility() == View.VISIBLE) {
            layoutDiscoverContent.animate().alpha(0f).setDuration(200).withEndAction(() -> layoutDiscoverContent.setVisibility(View.GONE));
            rvSearchResults.setVisibility(View.VISIBLE);
            rvSearchResults.setAlpha(0f);
            rvSearchResults.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void showDiscoverState() {
        tvHeaderSearch.setText("Discover");
        if (rvSearchResults.getVisibility() == View.VISIBLE) {
            rvSearchResults.animate().alpha(0f).setDuration(200).withEndAction(() -> rvSearchResults.setVisibility(View.GONE));
            layoutDiscoverContent.setVisibility(View.VISIBLE);
            layoutDiscoverContent.setAlpha(0f);
            layoutDiscoverContent.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void hideKeyboard() {
        View view = requireActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        etSearchInput.clearFocus();
    }

    private void loadIntelligentDiscoverFeed() {
        networkExecutor.submit(() -> {
            try {
                // Now fetching categories directly instead of relying on telemetry history
                List<Song> topPicks = fetchFromJioSaavnOfficial("latest top hits", 1);
                List<Song> curated = fetchFromJioSaavnOfficial("lofi chill study", 1);
                List<Song> trending = fetchFromJioSaavnOfficial("trending charts", 1);

                mainThreadHandler.post(() -> {
                    if (isAdded()) {
                        latestTopPicks = topPicks;
                        latestCurated = curated;
                        latestTrending = trending;

                        topPicksAdapter.setSongs(latestTopPicks);
                        curatedAdapter.setSongs(latestCurated);
                        trendingAdapter.setSongs(latestTrending);
                    }
                });
            } catch (Exception e) { Log.e("DiscoverEngine", "Failed to load discover feed", e); }
        });
    }

    private void initiateSearch() {
        String query = etSearchInput.getText().toString().trim();
        if (query.isEmpty()) return;

        currentSearchQuery = query;
        currentSearchPage = 1;
        hasMoreResults = true;

        hideKeyboard();
        showSearchState();
        latestSearchResults.clear();
        searchAdapter.setSongs(new ArrayList<>());
        etSearchInput.setHint("Searching...");

        executeSearchTask(currentSearchQuery, currentSearchPage, false);
    }

    private void loadMoreResults() {
        isFetchingMore = true;
        currentSearchPage++;
        pbInfiniteLoading.setVisibility(View.VISIBLE);
        executeSearchTask(currentSearchQuery, currentSearchPage, true);
    }

    private void executeSearchTask(String query, int page, boolean isAppending) {
        if (activeSearchFuture != null && !activeSearchFuture.isDone() && !isAppending) activeSearchFuture.cancel(true);

        activeSearchFuture = networkExecutor.submit(() -> {
            try {
                List<Song> results = fetchFromJioSaavnOfficial(query, page);

                mainThreadHandler.post(() -> {
                    if (isAdded()) {
                        etSearchInput.setHint("Search songs, artists...");
                        pbInfiniteLoading.setVisibility(View.GONE);
                        isFetchingMore = false;

                        if (isAppending) {
                            latestSearchResults.addAll(results);
                            searchAdapter.addSongs(results);
                        } else {
                            latestSearchResults = new ArrayList<>(results);
                            searchAdapter.setSongs(latestSearchResults);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("SSOTSearch", "Error", e);
                mainThreadHandler.post(() -> {
                    if (isAdded()) {
                        pbInfiniteLoading.setVisibility(View.GONE);
                        isFetchingMore = false;
                        if (!isAppending) {
                            etSearchInput.setHint("Search failed.");
                            Toast.makeText(getContext(), "Check your network.", Toast.LENGTH_LONG).show();
                        } else {
                            hasMoreResults = false;
                        }
                    }
                });
            }
        });
    }

    private List<Song> fetchFromJioSaavnOfficial(String query, int page) throws Exception {
        String safeQuery = URLEncoder.encode(query, "UTF-8");
        try {
            return parseUnofficialAPI("https://saavn.dev/api/search/songs?query=" + safeQuery + "&page=" + page + "&limit=25");
        } catch (Exception e1) {
            try {
                return parseUnofficialAPI("https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query=" + safeQuery + "&page=" + page + "&limit=25");
            } catch (Exception e2) {
                return parseOfficialAPI(query, page);
            }
        }
    }

    private List<Song> parseUnofficialAPI(String url) throws Exception {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());
            JSONObject json = new JSONObject(response.body().string());

            JSONArray results = null;
            if (json.has("data")) {
                Object dataObj = json.get("data");
                if (dataObj instanceof JSONObject && ((JSONObject)dataObj).has("results")) {
                    results = ((JSONObject)dataObj).getJSONArray("results");
                } else if (dataObj instanceof JSONArray) {
                    results = (JSONArray) dataObj;
                }
            } else if (json.has("results")) {
                results = json.getJSONArray("results");
            }

            if (results == null) throw new Exception("Unrecognized JSON structure");

            List<Song> songs = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                String id = item.optString("id", "");
                String title = item.optString("name", item.optString("title", "")).replace("&quot;", "\"").replace("&amp;", "&").trim();
                if (id.isEmpty() || title.isEmpty()) continue;

                String artist = "Unknown Artist";
                if (item.has("primaryArtists")) artist = item.optString("primaryArtists").trim();
                else if (item.has("subtitle")) artist = item.optString("subtitle").trim();

                String album = "Unknown Album";
                if (item.has("album")) {
                    Object albumObj = item.get("album");
                    if (albumObj instanceof JSONObject) album = ((JSONObject)albumObj).optString("name", "Unknown Album");
                    else if (albumObj instanceof String) album = (String) albumObj;
                }

                long duration = item.optLong("duration", 0);

                String thumbnail = "";
                if (item.has("image")) {
                    Object imgObj = item.get("image");
                    if (imgObj instanceof JSONArray) {
                        JSONArray imageArr = (JSONArray) imgObj;
                        if (imageArr.length() > 0) thumbnail = imageArr.getJSONObject(imageArr.length() - 1).optString("link", imageArr.getJSONObject(imageArr.length() - 1).optString("url", ""));
                    } else if (imgObj instanceof String) {
                        thumbnail = (String) imgObj;
                    }
                }
                thumbnail = thumbnail.replace("150x150", "500x500");

                Song song = new Song(id, title, artist, album, thumbnail, duration);

                if (item.has("downloadUrl")) {
                    Object dlObj = item.get("downloadUrl");
                    if (dlObj instanceof JSONArray) {
                        JSONArray downloadArr = (JSONArray) dlObj;
                        if (downloadArr.length() > 0) {
                            String streamUrl = downloadArr.getJSONObject(downloadArr.length() - 1).optString("link", downloadArr.getJSONObject(downloadArr.length() - 1).optString("url", ""));
                            if (!streamUrl.isEmpty()) song.setStreamUrl(streamUrl);
                        }
                    }
                }
                songs.add(song);
            }
            return songs;
        }
    }

    private List<Song> parseOfficialAPI(String query, int page) throws Exception {
        String safeQuery = URLEncoder.encode(query, "UTF-8");
        Request request = new Request.Builder()
                .url("https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=25&p=" + page + "&q=" + safeQuery)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("Official API HTTP " + response.code());
            JSONObject json = new JSONObject(response.body().string());
            if (!json.has("results")) throw new Exception("No results found");
            JSONArray data = json.getJSONArray("results");

            List<Song> songs = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.getJSONObject(i);
                String title = item.optString("title", item.optString("song", "")).replace("&quot;", "\"").replace("&amp;", "&").trim();
                if (title.isEmpty()) continue;

                String artist = item.optString("subtitle", "").replace("&quot;", "\"").replace("&amp;", "&").trim();
                String album = "Unknown Album";

                if (item.has("more_info")) {
                    JSONObject moreInfo = item.getJSONObject("more_info");
                    if (artist.isEmpty()) artist = moreInfo.optString("primary_artists", "").replace("&quot;", "\"").replace("&amp;", "&").trim();
                    album = moreInfo.optString("album", "Unknown Album").replace("&quot;", "\"").replace("&amp;", "&").trim();
                }
                if (artist.isEmpty()) artist = "Unknown Artist";

                String id = item.optString("id");
                String thumbnail = item.optString("image", "").replace("150x150", "500x500");

                Song song = new Song(id, title, artist, album, thumbnail, 0);
                songs.add(song);
            }
            return songs;
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
            Song song = list.get(position);
            holder.title.setText(song.getTitle());
            holder.artist.setText(song.getArtist());

            Glide.with(holder.itemView.getContext())
                    .load(song.getThumbnailUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .into(holder.thumbnail);

            holder.itemView.setOnClickListener(v -> listener.onSongClick(song));
        }

        @Override public int getItemCount() { return list.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            ImageView thumbnail; TextView title, artist;
            Holder(@NonNull View v) {
                super(v);
                thumbnail = v.findViewById(R.id.iv_discover_thumbnail);
                title = v.findViewById(R.id.tv_discover_title);
                artist = v.findViewById(R.id.tv_discover_artist);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeSearchFuture != null) activeSearchFuture.cancel(true);
    }
}
