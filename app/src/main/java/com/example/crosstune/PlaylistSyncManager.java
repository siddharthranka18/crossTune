package com.example.crosstune;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.Executors;

public class PlaylistSyncManager {

    public static void syncOnFirstFetch(String email, List<Playlist> playlists) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // We use REPLACE INTO to handle updates/inserts in one go
            String sql = "REPLACE INTO playlists (spotify_id, user_email, playlist_name, image_url) VALUES (?, ?, ?, ?)";

            try (Connection conn = DBConnection.connect();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                for (Playlist p : playlists) {
                    pstmt.setString(1, p.id); // The unique Spotify ID
                    pstmt.setString(2, email);
                    pstmt.setString(3, p.name);
                    String imgUrl = (p.images != null && p.images.length > 0) ? p.images[0].url : "";
                    pstmt.setString(4, imgUrl);
                    pstmt.addBatch();
                }

                pstmt.executeBatch();
                System.out.println("DBMS Sync: Upsert complete using Spotify IDs.");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}