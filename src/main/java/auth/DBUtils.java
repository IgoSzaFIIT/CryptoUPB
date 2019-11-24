/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auth;

import main.CryptoUPB;
import main.FileHandler;
import main.FileManagerBean;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Base64;

/**
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
        } catch (ClassNotFoundException e) {
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
            String TableStruct = "("
                    + "   id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "   username VARCHAR(100),"
                    + "   pwdhash VARCHAR(100),"
                    + "   pwdsalt BLOB,"
                    + "   loginAttempts INTEGER,"
                    + "   lastAttempt DATETIME,"
                    + "   privateKey VARCHAR(100),"
                    + "   publicKey VARCHAR(100))";
            createTableIfNotExists(conn, tableName, TableStruct);

            tableName = "files";
            TableStruct = "("
                    + "  id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "  filename VARCHAR(100),"
                    + "  path VARCHAR(100),"
                    + "  owner INTEGER )";
            createTableIfNotExists(conn, tableName, TableStruct);

            tableName = "access";
            TableStruct = "("
                    + "ID_user INTEGER, "
                    + "ID_file INTEGER )";
            createTableIfNotExists(conn, tableName, TableStruct);

            tableName = "comments";
            TableStruct = "("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "message VARCHAR(500),"
                    + "file INTEGER,"
                    + "sender  INTEGER,"
                    + "time DATETIME default CURRENT_TIMESTAMP)";
            createTableIfNotExists(conn, tableName, TableStruct);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static void createTableIfNotExists(Connection conn, String tableName, String tableStructure) {
        String sqlCreate = "CREATE TABLE IF NOT EXISTS " + tableName + tableStructure;

        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sqlCreate);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void insertUser(Connection conn, String tableName, String username, String pwd, String pwdhash, byte[] salt, String PrivateKey, String PublicKey) {
        PrivateKey = encrypt_decrypt(PrivateKey, SymetricCipherKey(pwd), 0);
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("insert into " + tableName + "(username,pwdhash,pwdsalt,loginAttempts,privateKey, publicKey) values(?,?,?,?,?,?)");
            stmt.setString(1, username);
            stmt.setString(2, pwdhash);
            stmt.setBytes(3, salt);
            stmt.setInt(4, 0);
            stmt.setString(5, PrivateKey);
            stmt.setString(6, PublicKey);

            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static void updateLoginAttempts(Connection conn, String tableName, String username, int n) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("update " + tableName + " set loginAttempts = ?, lastAttempt = ? where username = ?");
            stmt.setInt(1, n);
            stmt.setTimestamp(2, java.sql.Timestamp.from(java.time.Instant.now()));
            stmt.setString(3, username);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public static ResultSet selectUser(Connection conn, String tableName, String username, String pwdhash) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select username,pwdhash from " + tableName + " where username = ? and pwdhash = ?");
            stmt.setString(1, username);
            stmt.setString(2, pwdhash);
            ResultSet res = stmt.executeQuery();
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static ResultSet selectUserName(Connection conn, String tableName, String username) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select username from " + tableName + " where username = ?");
            stmt.setString(1, username);
            ResultSet res = stmt.executeQuery();
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static ResultSet selectUserPwdSalt(Connection conn, String tableName, String username) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select username,pwdsalt from " + tableName + " where username = ?");
            stmt.setString(1, username);
            ResultSet res = stmt.executeQuery();
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static ResultSet selectLoginAttempts(Connection conn, String tableName, String username) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select loginAttempts,lastAttempt from " + tableName + " where username = ?");
            stmt.setString(1, username);
            ResultSet res = stmt.executeQuery();
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // file table related selects
    public static ResultSet selectAllFiles(Connection conn) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select files.filename,users.username,files.path,files.id from files join users on files.owner = users.id");
            ResultSet res = stmt.executeQuery();
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static ResultSet selectFile(Connection conn, String tableName, int fileId) {
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select filename,owner,path from " + tableName + " where id = ?");
            stmt.setInt(1, fileId);
            ResultSet res = stmt.executeQuery();
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    
    //pri uploade vlozi file do DB
    // TODO: pri uploade suboru treba pridavat aj do db
    public static void insertFile(Connection conn, String fileTable, String userTable, String filename, String path, String owner) {
        try {
            // zabranit duplicintnym suborom
            PreparedStatement check = conn.prepareStatement("SELECT * FROM " + fileTable + " WHERE filename=? AND path=?");
            check.setString(1, filename);
            check.setString(2, path);
            ResultSet res = check.executeQuery();
            if (res.isBeforeFirst()) {
                System.out.println("File Already exists!\n");
                return;
            }
            //ak subor este v db neni, pridaj ho
            PreparedStatement stmnt = conn.prepareStatement("INSERT INTO " + fileTable + " (filename, path, owner) VALUES (?,?,(SELECT id FROM " + userTable + " WHERE username=?))");
            stmnt.setString(1, filename);
            stmnt.setString(2, path);
            stmnt.setString(3, owner);
            stmnt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //access to files
    public static void grantAccess(Connection conn, String accessTable, String fileTable, String userTable, String fileName, String path, String userName) {
        try {
            PreparedStatement stmnt = conn.prepareStatement("INSERT INTO " + accessTable + " (ID_user, ID_file) VALUES ((SELECT id FROM " + userTable + " WHERE username=?),(SELECT files.id FROM " + fileTable + " WHERE filename=? AND path=?))");
            stmnt.setString(1, userName);
            stmnt.setString(2, fileName);
            stmnt.setString(3, path);
            stmnt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //add comment to file
    public static void addComment(Connection conn, String commentTable, String fileTable, String userTable, String message, String filename, String path, String sender) {
        try {
            PreparedStatement stmnt = conn.prepareStatement("INSERT INTO " + commentTable + " (message, file, sender) values (?,(SELECT id FROM " + fileTable + " WHERE filename=? AND path=?),(SELECT id FROM " + userTable + " WHERE username=?))");
            stmnt.setString(1, message);
            stmnt.setString(2, filename);
            stmnt.setString(3, path);
            stmnt.setString(4, sender);
            stmnt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    // return all timestamps, senders, messages for file
    public static ResultSet getFileComments(Connection conn, String path, String fileName) {
        try {
            PreparedStatement stmnt = conn.prepareStatement("select comments.time, users.username, comments.message from comments JOIN users on comments.sender = users.id where comments.file IN (SELECT id from files where path = ? and filename = ? )");
            stmnt.setString(1, path);
            stmnt.setString(2, fileName);
            ResultSet res = stmnt.executeQuery();
            return res;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // returns decrypted user's privateKey -
    public static String getPrivateKey(Connection conn, String userTable, String user, String pwd) {
        //over spravnost hesla
        ResultSet rs = selectUserPwdSalt(conn, userTable, user);
        byte[] salt = new byte[0];
        try {
            if (rs.isBeforeFirst()) {
                salt = rs.getBytes("pwdsalt");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String pwdhash = null;
        try {
            pwdhash = AuthManagerBean.Hash(pwd, salt);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        boolean valid = false;
        rs = selectUser(conn, userTable, user, pwdhash);
        try {
            if (!rs.isBeforeFirst()) {
                System.out.println("Wrong password!\n");
                return null;
            } else valid = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //ak je heslo spravne
        String privateKey = "";
        if (valid) {
            PreparedStatement stmnt = null;
            try {
                stmnt = conn.prepareStatement("SELECT users.privateKey FROM " + userTable + " WHERE users.username=?");
                stmnt.setString(1, user);
                rs = stmnt.executeQuery();
                privateKey = rs.getString("privateKey");
                return encrypt_decrypt(privateKey, SymetricCipherKey(pwd), 1);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    // returns specified user's publicKey
    public static String getUserPublicKey(Connection conn,String username){
        try {
            PreparedStatement stmnt = conn.prepareStatement("select users.publicKey from users where username=?");
            stmnt.setString(1,username);
            ResultSet res = stmnt.executeQuery();
            return res.getString("publicKey");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;

    }
    // check if user has access to file ( for download )
    public static boolean hasUserAccess(Connection conn, String filename, String path, String user){
        try {
            PreparedStatement stmnt = conn.prepareStatement("select * from access where ID_file IN (Select id From files where filename = ? and path = ?) and ID_user in (Select id from users where username = ?)");
            stmnt.setString(1,filename);
            stmnt.setString(2,path);
            stmnt.setString(3,user);
            ResultSet res = stmnt.executeQuery();
            if(res.isBeforeFirst()) return true;
            else return false;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean doesUserExists(Connection conn, String username){
        try {
            PreparedStatement stmt = conn.prepareStatement
                    ("select username from users where username = ?");
            stmt.setString(1, username);
            ResultSet res = stmt.executeQuery();
            if(res.isBeforeFirst()) return true;
            else return false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void ShareFile(Connection conn, String usernameOwner, String usernameToShare, String ownerPwd, String fileName, String filePath) throws Exception {
        String fullFilePath = filePath + fileName;
        String privateKeyOwner = getPrivateKey(conn,"users",usernameOwner, ownerPwd);
        if(privateKeyOwner != null){
            File userFile = new File(fullFilePath);
            byte[] fileContent = Files.readAllBytes(userFile.toPath());
            byte[] plainText = CryptoUPB.decrypt(fileContent, privateKeyOwner);

            String publicKeyUserToShare = getUserPublicKey(conn,usernameToShare);
            if(publicKeyUserToShare != null){
                handleSharedFileUpload(conn, plainText, fileName, publicKeyUserToShare, usernameToShare);
            }
        }
    }
    // -------- used for private key encryption --------

    private static String SymetricCipherKey(String pwd) {
        //hash
        try {
            KeySpec spec = new PBEKeySpec(pwd.toCharArray(), new byte[0], 50643, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            String key = Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
            return key;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 0 - encrypt, 1 - decrypt
    private static String encrypt_decrypt(String message, String key, int mode) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            byte[] decodeKey = Base64.getDecoder().decode(key);
            SecretKey sk = new SecretKeySpec(decodeKey, 0, decodeKey.length, "AES");
            if (mode == 0) cipher.init(Cipher.ENCRYPT_MODE, sk);
            else if (mode == 1) cipher.init(Cipher.DECRYPT_MODE, sk);
            else return null;
            final String finalString = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
            return finalString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void handleSharedFileUpload(Connection conn, byte[] filePart, String fileName, String pubKey, String usernameToShare){

        FileHandler h = new FileHandler();
        File f = h.handleSharedFileUpload(filePart,fileName,pubKey);
        if(f != null) {
            String username = usernameToShare;
            String fName = f.getName();
            String fPath = f.getPath();
            DBUtils.insertFile(conn, "files", "users", fName, fPath, username);
            DBUtils.grantAccess(conn, "access", "files", "users", fName, fPath, username);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File " + f.getName() + " uploaded successfully."));
        }
        else
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Failed to upload file "));
    }

}
