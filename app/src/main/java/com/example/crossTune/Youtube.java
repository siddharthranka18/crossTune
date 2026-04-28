package com.example.crossTune;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class Youtube {

    // 1. Callback interface to pass data back to the UI
    public interface YoutubeCallback {
        void onSuccess(String playlistName, String rawSongsText);
        void onError(String errorMsg);
    }

    public static void fetchYoutubePlaylist(String urlParam, YoutubeCallback callback) {
        // Build the URL with the query parameter for a GET request
        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://yt-for-playlist-fetch.onrender.com/playlist").newBuilder();
        urlBuilder.addQueryParameter("url", urlParam);
        String url = urlBuilder.build().toString();

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get() // This is a GET request, unlike Apple's POST
                .build();

        // Async call (Background thread)
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                if (callback != null) callback.onError("Network Request Failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (callback != null) callback.onError("API Error: HTTP " + response.code());
                    return;
                }

                try {
                    String rawResponse = response.body().string();
                    System.out.println("YouTube Raw Response: " + rawResponse);

                    JSONObject data = new JSONObject(rawResponse);

                    // Extract the exact keys from your Python script
                    String playlistName = data.getString("playlist_name");
                    String rawSongsText = data.getString("string");

                    // 🔥 Strip the trailing "||" if it exists
                    if (rawSongsText.endsWith("||")) {
                        rawSongsText = rawSongsText.substring(0, rawSongsText.length() - 2);
                    }

                    // Send it back to the UI
                    if (callback != null) {
                        callback.onSuccess(playlistName, rawSongsText);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) callback.onError("Invalid JSON format from API");
                }
            }
        });
    }
}