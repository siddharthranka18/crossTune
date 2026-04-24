package com.example.crosstune;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

// Change 'class' to 'interface'
public interface SpotifyApiService {
    @GET("v1/me/playlists")
    Call<PlaylistResponse> getCurrentUserPlaylists(
            @Header("Authorization") String authHeader,
            @Query("limit") int limit,
            @Query("offset") int offset
    ); // Semicolon here is correct for an interface
}