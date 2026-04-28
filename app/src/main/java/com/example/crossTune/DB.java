package com.example.crossTune;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DB {

    // Call this from anywhere: DB.execute("YOUR SQL STRING");
    public static void execute(String query) {
        new Thread(() -> {
            try {
                // 1. Notice the name change here! We removed ".cj."
                Class.forName("com.mysql.jdbc.Driver");

                // 2. The proper URL
                String url = "jdbc:mysql://switchback.proxy.rlwy.net:41893/railway?useSSL=false&allowPublicKeyRetrieval=true";

                // 3. Connect and execute
                Connection c = DriverManager.getConnection(url, "root", "vpytRSemYOnDrVTTLOKUTihyZIxSPggo");
                Statement s = c.createStatement();
                s.execute(query);

                // 4. Safely close
                s.close();
                c.close();

            } catch (Exception e) {
                System.out.println("DB_ERROR_NORMAL: " + e.getMessage());
                e.printStackTrace();
            } catch (Error err) {
                System.out.println("DB_FATAL_CRASH: " + err.getMessage());
                err.printStackTrace();
            }
        }).start();
    }
}