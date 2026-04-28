package com.example.crossTune;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlaylistBulkImporter {

    private final SharedMusicViewModel musicViewModel;
    private final ExecutorService networkExecutor;
    private final Handler mainThreadHandler;
    private final OkHttpClient httpClient;

    public interface ImportCallback {
        void onProgress(int current, int total, String songName);

        void onComplete();

        void onError(String error);
    }

    public PlaylistBulkImporter(SharedMusicViewModel musicViewModel) {
        this.musicViewModel = musicViewModel;
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
        this.networkExecutor = Executors.newSingleThreadExecutor();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Creates a new playlist and populates it with the first search result of each song.
     *
     * @param playlistName Name of the new playlist
     * @param rawSongList  String containing songs separated by "||"
     * @param callback     Callback to track UI progress
     */
    public void importSongsToNewPlaylist(String playlistName, String rawSongList, ImportCallback callback) {
        // 1. Create the Playlist on the Main Thread
        mainThreadHandler.post(() -> {
            musicViewModel.createPlaylist(playlistName);
            List<Playlist> currentPlaylists = musicViewModel.getPlaylists().getValue();

            if (currentPlaylists == null || currentPlaylists.isEmpty()) {
                if (callback != null)
                    callback.onError("Critical Error: Could not create playlist.");
                return;
            }

            // The ViewModel adds the new playlist to the end of the list
            String newPlaylistId = currentPlaylists.get(currentPlaylists.size() - 1).getId();

            // 2. Switch to Background Thread for Network Searches
            networkExecutor.submit(() -> processSongsInBackground(newPlaylistId, rawSongList, callback));
        });
    }

    private void processSongsInBackground(String playlistId, String rawSongList, ImportCallback callback) {
        // Split the string using the literal "||" (needs escaping in regex)
        String[] queries = rawSongList.split("\\|\\|");
        int total = queries.length;
        int current = 0;

        for (String query : queries) {
            String trimmedQuery = query.trim();
            trimmedQuery = cleanSongQuery(trimmedQuery);
            if (trimmedQuery.isEmpty()) continue;

            current++;
            try {
                // Fetch the exact 1st result
                Song foundSong = fetchFirstResult(trimmedQuery);

                if (foundSong != null) {
                    final int finalCurrent = current;
                    // Switch to Main Thread to update the ViewModel securely
                    mainThreadHandler.post(() -> {
                        musicViewModel.addSongToPlaylist(playlistId, foundSong);
                        if (callback != null)
                            callback.onProgress(finalCurrent, total, foundSong.getTitle());
                    });
                }
            } catch (Exception e) {
                Log.e("PlaylistBulkImporter", "Failed to find song: " + trimmedQuery, e);
            }
        }

        if (callback != null) {
            mainThreadHandler.post(callback::onComplete);
        }
    }

    // =========================================================
    // SEARCH EXTRACTION ENGINE (Mirrors your SearchFragment SSOT)
    // =========================================================
    // =========================================================
    // TRIPLE-REDUNDANT EXTRACTION ENGINE (Mirrors SearchFragment)
    // =========================================================
    private Song fetchFirstResult(String query) throws Exception {
        String safeQuery = URLEncoder.encode(query, "UTF-8");

        // 1. Try Primary Unofficial API (Pre-caches 320kbps URLs)
        try {
            return parseUnofficialAPI("https://saavn.dev/api/search/songs?query=" + safeQuery + "&page=1&limit=1");
        } catch (Exception e1) {
            Log.e("PlaylistBulkImporter", "Primary API failed", e1);

            // 2. Try Backup Unofficial API
            try {
                return parseUnofficialAPI("https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query=" + safeQuery + "&page=1&limit=1");
            } catch (Exception e2) {
                Log.e("PlaylistBulkImporter", "Backup API failed", e2);

                // 3. Absolute Fallback: Official JioSaavn API
                return parseOfficialAPI(safeQuery);
            }
        }
    }

    private Song parseUnofficialAPI(String url) throws Exception {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new Exception("HTTP " + response.code());
            JSONObject json = new JSONObject(response.body().string());

            // Handle dynamically changing JSON structures
            JSONArray results = null;
            if (json.has("data")) {
                Object dataObj = json.get("data");
                if (dataObj instanceof JSONObject && ((JSONObject) dataObj).has("results")) {
                    results = ((JSONObject) dataObj).getJSONArray("results");
                } else if (dataObj instanceof JSONArray) {
                    results = (JSONArray) dataObj;
                }
            } else if (json.has("results")) {
                results = json.getJSONArray("results");
            }

            if (results == null || results.length() == 0) throw new Exception("No results parsed");

            JSONObject item = results.getJSONObject(0);
            String id = item.optString("id", "");
            String title = item.optString("name", item.optString("title", "")).replace("&quot;", "\"").replace("&amp;", "&").trim();
            if (id.isEmpty() || title.isEmpty()) throw new Exception("Invalid song data");

            String artist = "Unknown Artist";
            if (item.has("primaryArtists")) artist = item.optString("primaryArtists").trim();
            else if (item.has("subtitle")) artist = item.optString("subtitle").trim();

            String album = "Unknown Album";
            if (item.has("album")) {
                Object albumObj = item.get("album");
                if (albumObj instanceof JSONObject)
                    album = ((JSONObject) albumObj).optString("name", "Unknown Album");
                else if (albumObj instanceof String) album = (String) albumObj;
            }

            long duration = item.optLong("duration", 0);

            String thumbnail = "";
            if (item.has("image")) {
                Object imgObj = item.get("image");
                if (imgObj instanceof JSONArray) {
                    JSONArray imageArr = (JSONArray) imgObj;
                    if (imageArr.length() > 0)
                        thumbnail = imageArr.getJSONObject(imageArr.length() - 1).optString("link", imageArr.getJSONObject(imageArr.length() - 1).optString("url", ""));
                } else if (imgObj instanceof String) {
                    thumbnail = (String) imgObj;
                }
            }
            thumbnail = thumbnail.replace("150x150", "500x500");

            Song song = new Song(id, title, artist, album, thumbnail, duration);

            // ========================================================
            // CRITICAL FIX: Properly extracting both "url" and "link"
            // ========================================================
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
            return song;
        }
    }

    private Song parseOfficialAPI(String safeQuery) throws Exception {
        Request request = new Request.Builder()
                .url("https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=1&p=1&q=" + safeQuery)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful())
                throw new Exception("Official API HTTP " + response.code());
            JSONObject json = new JSONObject(response.body().string());
            if (!json.has("results")) throw new Exception("No results found in Official API");
            JSONArray data = json.getJSONArray("results");
            if (data.length() == 0) throw new Exception("Empty results");

            JSONObject item = data.getJSONObject(0);
            String title = item.optString("title", item.optString("song", "")).replace("&quot;", "\"").replace("&amp;", "&").trim();

            String artist = item.optString("subtitle", "").replace("&quot;", "\"").replace("&amp;", "&").trim();
            String album = "Unknown Album";
            if (item.has("more_info")) {
                JSONObject moreInfo = item.getJSONObject("more_info");
                if (artist.isEmpty())
                    artist = moreInfo.optString("primary_artists", "").replace("&quot;", "\"").replace("&amp;", "&").trim();
                album = moreInfo.optString("album", "Unknown Album").replace("&quot;", "\"").replace("&amp;", "&").trim();
            }
            if (artist.isEmpty()) artist = "Unknown Artist";

            String id = item.optString("id");
            String thumbnail = item.optString("image", "").replace("150x150", "500x500");

            return new Song(id, title, artist, album, thumbnail, 0);
        }
    }
    // =========================================================
    // YOUTUBE SANITIZER ENGINE
    // =========================================================
    private String cleanSongQuery(String query) {
        if (query == null) return "";

        // 1. Remove anything inside (parentheses) or [brackets]
        String cleaned = query.replaceAll("\\(.*?\\)", "").replaceAll("\\[.*?\\]", "");

        // 2. Strip out common YouTube keywords (case-insensitive)
        cleaned = cleaned.replaceAll("(?i)official video|music video|lyric video|audio|remastered|hd", "");

        // 3. Replace hyphens with spaces (e.g., "Artist - Title" becomes "Artist Title")
        cleaned = cleaned.replace("-", " ");

        // 4. Remove any double spaces left behind and trim the edges
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }
}