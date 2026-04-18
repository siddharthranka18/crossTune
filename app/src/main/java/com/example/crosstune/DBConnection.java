package com.example.crosstune;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String url = "jdbc:mysql://monorail.proxy.rlwy.net:17727/railway?useSSL=false&allowPublicKeyRetrieval=true";
            String user = "root";
            String pass = "VBaeubHexkotCcePElBAqOqEPHzEBSOP";

            return DriverManager.getConnection(url, user, pass);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}