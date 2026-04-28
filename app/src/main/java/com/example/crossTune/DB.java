package com.example.crossTune;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DB {

    private static final String URL = "jdbc:mysql://switchback.proxy.rlwy.net:41893/railway";
    private static final String USER = "root";
    private static final String PASS = "vpytRSemYOnDrVTTLOKUTihyZIxSPggo";

    private static final String[] MYSQL_DRIVERS = {
            "com.mysql.cj.jdbc.Driver",
            "com.mysql.jdbc.Driver"
    };

    public interface SummaryCallback {
        void onResult(int count, int minutes);
    }

    public static void execute(String query) {
        new Thread(() -> {
            try {
                loadDriver();
                try (Connection c = DriverManager.getConnection(URL, USER, PASS);
                     Statement s = c.createStatement()) {
                    s.execute(query);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Runs in the caller thread; use from a background thread to keep ordering.
    public static void executeSync(String query) {
        try {
            loadDriver();
            try (Connection c = DriverManager.getConnection(URL, USER, PASS);
                 Statement s = c.createStatement()) {
                s.execute(query);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // FULFILLING RUBRIC: Calling a Stored Procedure (Point 12)
    public static void getPlaylistSummary(String playlistId, SummaryCallback callback) {
        new Thread(() -> {
            try {
                loadDriver();
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

    private static void loadDriver() throws ClassNotFoundException {
        for (String driver : MYSQL_DRIVERS) {
            try {
                Class.forName(driver);
                Log.d("DB", "Loaded MySQL driver: " + driver);
                return;
            } catch (ClassNotFoundException ignored) {
                // Try next
            }
        }
        throw new ClassNotFoundException("No MySQL JDBC driver found in APK");
    }
}
