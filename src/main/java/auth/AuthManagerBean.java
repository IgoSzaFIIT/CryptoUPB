package auth;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;


@ApplicationScoped
@ManagedBean(name="authManagerBean")
public class AuthManagerBean {

    private final String INDEX_PATH = "/index.xhtml";
    private final String LOGIN_PATH = "/auth/login.xhtml";
    private final String SAVE_FOLDER = "\\temp\\";
    private final String USERS_TABLE_NAME = "users";

    //SQLite DB
    private Connection dbConn;

    //username
    private String usr;

    //password
    private String pwd;

    //password repeat (used in registation)
    private String pwdR;

    public String getUsr() {
        return usr;
    }

    public String getPwd() {
        return pwd;
    }

    public String getPwdR() {
        return pwdR;
    }

    public void setUsr(String usr) {
        this.usr = usr;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void setPwdR(String pwdR) {
        this.pwdR = pwdR;
    }


    //handle user login
    public String handleLogin() {
        if(usr.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your username."));
            return null;
        }
        if(pwd.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your password."));
            return null;
        }
        /*
            TODO:
            potrebujeme z DB vytiahnut salt pre daneho uzivatela
        */
        String pwdhash = pwd;
        int nAttempts = 0;
        Timestamp lastAttempt = null;
        ResultSet res = DBUtils.selectLoginAttempts(dbConn, USERS_TABLE_NAME, usr);
        try {
            if(res.isBeforeFirst()) {
                nAttempts = res.getInt("loginAttempts");
                lastAttempt = res.getTimestamp("lastAttempt");

                /* Lockout for 5 minutes after 3 unsuccessful attempts */
                if(nAttempts > 2){
                    if((java.sql.Timestamp.from(java.time.Instant.now()).getTime() - lastAttempt.getTime()) > 300000)
                        DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, 0);
                    else {
                        nAttempts++;
                        DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, nAttempts);
                        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Too many failed attempts. Try again later."));
                        return null;
                    }
                }

            }

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        boolean valid = validateLogin(this.usr, pwdhash);
        nAttempts++;
        if(!valid) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Username/password combination is incorrect."));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, nAttempts);
            return null;
        }
        else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Login successful."));
            DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, 0);
            HttpSession session = SessionUtils.getSession();
            session.setAttribute("username", usr);
            return INDEX_PATH + "?faces-redirect=true";
        }
    }

    //handle user logout
    public String handleLogout() {
        HttpSession session = SessionUtils.getSession();
        session.invalidate();
        return LOGIN_PATH + "?faces-redirect=true";
    }

    //handle user registration
    public String handleRegistration() {
        if(usr.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter a username."));
            return null;
        }
        if(pwd.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter a password."));
            return null;
        }
        if(pwdR.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please repeat the password."));
            return null;
        }
        if(!pwd.equals(pwdR)) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Passwords do not match!"));
            return null;
        }
        if(pwd.length() < 6) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Password too short."));
            return null;
        }
        Pattern regex = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");
        if(!regex.matcher(pwd).matches()){
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Password must contain at least one uppercase letter, one lowercase letter and one digit."));
            return null;
        }
        //TODO:
        // overenie ci je heslo slovnikove
        byte[] salt = salt(8);
        String pwdhash = null;
        try {
            pwdhash = Hash(pwd,salt,126,65536);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        //TODO: treba ukladat aj salt ( meno, hash, salt )
        DBUtils.insertUser(dbConn, USERS_TABLE_NAME, usr, pwdhash);

        HttpSession session = SessionUtils.getSession();
        session.setAttribute("username", usr);
        return handleLogin();
    }

    public String Hash(String pwd, byte[] salt, int lengthBits, int iterCount) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //hash
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, iterCount, lengthBits);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        String pwdhash = factory.generateSecret(spec).getEncoded().toString();
        return pwdhash;
    }

    private byte[] salt(int sizeInBytes){
        SecureRandom rnd = new SecureRandom();
        byte[] salt = new byte[sizeInBytes];
        rnd.nextBytes(salt);
        return salt;
    }

    private boolean validateLogin(String u, String p) {
        /*
            Check if user is already logged in
        */
        HttpSession session = SessionUtils.getSession();
        if(session != null && session.getAttribute("username") != null) {
            String usrS = session.getAttribute("username").toString();
            if(usrS.equals(usr))
                return true;
        }

        ResultSet res = DBUtils.selectUser(dbConn, USERS_TABLE_NAME, u, p);
        try {
            //wrong login
            if(!res.isBeforeFirst())
                return false;
            //correct login
            else
                return true;

        }
        catch(Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    @PostConstruct
    public void init() {
        /* Initialize SQLite DB connection */
        String dbPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + SAVE_FOLDER;
        String dbFileName = "users.db";
        dbConn = DBUtils.initDB(dbPath, dbFileName);
    }



}
