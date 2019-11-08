/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auth;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 *
 * @author Igor
 */
public class DBUtils {
    public static Connection initDB(String dbPath, String dbFileName) {
        
        String url = "jdbc:sqlite:" + dbPath + dbFileName;
        
        
        File folder = new File(dbPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
            String tableName = "users";
            String userTableStruct = "("
                + "   id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "   username VARCHAR(100),"
                + "   pwdhash VARCHAR(100),"
                + "   loginAttempts INTEGER,"
                + "   lastAttempt DATETIME)";
            createTableIfNotExists(conn, tableName, userTableStruct);
 
        } 
        catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
    
    public static void createTableIfNotExists(Connection conn, String tableName, String tableStructure) {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS " + tableName + tableStructure;               

        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sqlCreate);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void insertUser(Connection conn, String tableName, String username, String pwdhash) {
        try {
            PreparedStatement stmt = conn.prepareStatement
            ("insert into " + tableName + "(username,pwdhash,loginAttempts) values(?,?,?)");
            stmt.setString(1,username);
            stmt.setString(2,pwdhash);
            stmt.setInt(3, 0);
            stmt.executeUpdate();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void updateLoginAttempts(Connection conn, String tableName, String username, int n) {
        try {
            PreparedStatement stmt = conn.prepareStatement
            ("update " + tableName + " set loginAttempts = ?, lastAttempt = ? where username = ?");
            stmt.setInt(1, n);
            stmt.setTimestamp(2, java.sql.Timestamp.from(java.time.Instant.now()));
            stmt.setString(3,username);
            stmt.executeUpdate();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static ResultSet selectUser(Connection conn, String tableName, String username, String pwdhash) {
        try {
            PreparedStatement stmt = conn.prepareStatement
            ("select username,pwdhash from " + tableName + " where username = ? and pwdhash = ?");
            stmt.setString(1,username);
            stmt.setString(2,pwdhash);
            ResultSet res = stmt.executeQuery();
            return res;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static ResultSet selectLoginAttempts(Connection conn, String tableName, String username) {
        try {
            PreparedStatement stmt = conn.prepareStatement
            ("select loginAttempts,lastAttempt from " + tableName + " where username = ?");
            stmt.setString(1,username);
            ResultSet res = stmt.executeQuery();
            return res;
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
}
