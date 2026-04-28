package com.example.crossTune;

import android.os.Handler;
import android.os.Looper;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DB {

    private static final String URL = "jdbc:mysql://switchback.proxy.rlwy.net:41893/railway";
    private static final String USER = "root";
    private static final String PASS = "vpytRSemYOnDrVTTLOKUTihyZIxSPggo";

    public interface SummaryCallback {
        void onResult(int count, int minutes);
    }

    public static void execute(String query) {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection c = DriverManager.getConnection(URL, USER, PASS);
                     Statement s = c.createStatement()) {
                    s.execute(query);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // FULFILLING RUBRIC: Calling a Stored Procedure (Point 12)
    public static void getPlaylistSummary(String playlistId, SummaryCallback callback) {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection c = DriverManager.getConnection(URL, USER, PASS);
                     Statement s = c.createStatement();
                     ResultSet rs = s.executeQuery("CALL GetPlaylistSummary('" + playlistId + "')")) {
                    
                    if (rs.next()) {
                        int count = rs.getInt("song_count");
                        int seconds = rs.getInt("total_seconds");
                        int mins = seconds / 60;

                        // Post back to main thread for UI update
                        new Handler(Looper.getMainLooper()).post(() -> 
                            callback.onResult(count, mins)
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
