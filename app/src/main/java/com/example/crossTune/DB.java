package com.example.crossTune;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DB{

    // Call this from anywhere: DB.execute("YOUR SQL STRING");
    public static void execute(String query) {
        new Thread(() -> {
            // Put your Railway credentials right here in the URL, User, and Password fields
            String url = "jdbc:mysql://root:VBaeubHexkotCcePElBAqOqEPHzEBSOP@monorail.proxy.rlwy.net:17727/railway";

            try (Connection c = DriverManager.getConnection(url, "root", "VBaeubHexkotCcePElBAqOqEPHzEBSOP");
                 Statement s = c.createStatement()) {

                // Runs the query and immediately finishes
                s.execute(query);

            } catch (Exception ignored) {
                // You said no error checking! If it fails, it fails silently. 
            }
        }).start();
    }
}