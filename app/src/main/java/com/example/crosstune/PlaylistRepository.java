package com.example.crosstune;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlaylistRepository {
    private SpotifyApiService apiService;

    public PlaylistRepository() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/") // The base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(SpotifyApiService.class);
    }

    // This is Step 4 integrated into the class
    public void fetchPlaylists(String token, retrofit2.Callback<PlaylistResponse> callback) {
        String authHeader = "Bearer " + token;
        // We pass the callback so the Activity can hear the result
        apiService.getCurrentUserPlaylists(authHeader, 10, 0).enqueue(callback);
    }
}