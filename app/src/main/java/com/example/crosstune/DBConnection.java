package com.example.crosstune;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection connect() throws Exception {

        Class.forName("com.mysql.jdbc.Driver");

        String url = "jdbc:mysql://monorail.proxy.rlwy.net:17727/railway?useSSL=false";
        String user = "root";
        String pass = "VBaeubHexkotCcePElBAqOqEPHzEBSOP";

        // If it fails here, it will now throw the exact error to your screen
        return DriverManager.getConnection(url, user, pass);
    }
}