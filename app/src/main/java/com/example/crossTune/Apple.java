package com.example.crossTune;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;

public class Apple {

    // 1. Create an interface to pass the data back to the Fragment/Activity
    public interface AppleCallback {
        void onSuccess(String playlistName, String rawSongsText);
        void onError(String errorMsg);
    }

    public static void fetchApplePlaylist(String urlParam, AppleCallback callback) {
        String URL = "https://apple-w69y.onrender.com/playlist";
        OkHttpClient client = new OkHttpClient();

        // 🔥 FIX: Properly insert the string variable into the JSON payload
        String jsonPayload = "{ \"url\": \"" + urlParam + "\" }";

        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
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

                String rawResponse = response.body().string();
                System.out.println("Raw Response: " + rawResponse);

                try {
                    JSONObject data = new JSONObject(rawResponse);

                    // Extract the data
                    String playlistName = data.getString("playlist");
                    String rawSongsText = data.getString("text");

                    // Send it back to the UI!
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