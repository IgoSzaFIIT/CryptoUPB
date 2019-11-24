package auth;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.faces.bean.ViewScoped;
import javax.servlet.http.Part;

import main.KPGenerator;
import org.passay.CharacterCharacteristicsRule;
import org.passay.CharacterRule;
import org.passay.DictionarySubstringRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RepeatCharacterRegexRule;
import org.passay.Rule;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;
import org.passay.dictionary.ArrayWordList;
import org.passay.dictionary.WordListDictionary;
import org.passay.dictionary.WordLists;
import org.passay.dictionary.sort.ArraysSort;


@ViewScoped
@ManagedBean(name = "authManagerBean")
public class AuthManagerBean {

    private final String INDEX_PATH = "/index.xhtml";
    private final String LOGIN_PATH = "/auth/login.xhtml";
    private final String SAVE_FOLDER = "\\temp\\";
    private final String USERS_TABLE_NAME = "users";
    private final String FILES_TABLE_NAME = "files";
    private final String COMMENTS_TABLE_NAME = "comments";
    private final String ACCESS_TABLE_NAME = "access";

    //SQLite DB
    private Connection dbConn;

    //username
    private String usr;

    //password
    private String pwd;

    //password repeat (used in registation)
    private String pwdR;

    //slovnik na slovnikovy utok
    private WordListDictionary slovnik;

    //List podmienok na spravne heslo
    private List<Rule> rules = new ArrayList<>();

    //List errorov
    public List<String> messageFail = new ArrayList<>();

    private Part pubKey;

    private Part privKey;

    private String publicKey;

    private String privateKey;

    private String generate;

    private KPGenerator kpgen;

    public String getGenerate() {
        return generate;
    }

    public void setGenerate(String generate) {
        this.generate = generate;
    }

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

    public Part getPubKey() {
        return pubKey;
    }

    public void setPubKey(Part pubKey) {
        this.pubKey = pubKey;
    }

    public Part getPrivKey() {
        return privKey;
    }

    public void setPrivKey(Part privKey) {
        this.privKey = privKey;
    }


    //handle user login
    public String handleLogin() {
        if (usr.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your username."));
            return null;
        }
        if (pwd.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your password."));
            return null;
        }

        int nAttempts = 0;
        Timestamp lastAttempt = null;
        ResultSet res = DBUtils.selectLoginAttempts(dbConn, USERS_TABLE_NAME, usr);
        try {
            if (res.isBeforeFirst()) {
                nAttempts = res.getInt("loginAttempts");
                lastAttempt = res.getTimestamp("lastAttempt");

                /* Lockout for 5 minutes after 3 unsuccessful attempts */
                if (nAttempts > 2) {
                    if ((java.sql.Timestamp.from(java.time.Instant.now()).getTime() - lastAttempt.getTime()) > 300000)
                        DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, 0);
                    else {
                        nAttempts++;
                        DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, nAttempts);
                        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Too many failed attempts. Try again later."));
                        return null;
                    }
                }

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ResultSet res2 = DBUtils.selectUserPwdSalt(dbConn, USERS_TABLE_NAME, usr);
        byte[] salt = new byte[0];
        try {
            if (res2.isBeforeFirst()) {
                salt = res2.getBytes("pwdsalt");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String pwdhash = null;
        try {
            pwdhash = Hash(pwd, salt);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        boolean valid = validateLogin(this.usr, pwdhash);
        nAttempts++;
        if (!valid) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Username/password combination is incorrect."));
            DBUtils.updateLoginAttempts(dbConn, USERS_TABLE_NAME, usr, nAttempts);
            return null;
        } else {
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
    public String handleRegistration() throws Exception {
        //int hodnota = Integer.getInteger(generate);
        if (usr.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter a username."));
            return null;
        }
        if (pwd.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter a password."));
            return null;
        }
        if (pwdR.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please repeat the password."));
            return null;
        }
        if (!pwd.equals(pwdR)) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Passwords do not match!"));
            return null;
        }
        if(generate.equals("1")){
            kpgen = new KPGenerator();
            publicKey = kpgen.getEncodedPubKey();
            privateKey = kpgen.getEncodedPrivKey();
        }
        else {
            if (pubKey == null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please add public Key"));
                return null;
            }

            if (privKey == null) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please add private Key"));
                return null;
            }

            RSAParser parser = new RSAParser();
            publicKey = parser.getPublicKey(pubKey);
            privateKey = parser.getPrivateKey(privKey);
        }

        //check if username exists
        ResultSet res = DBUtils.selectUserName(dbConn, USERS_TABLE_NAME, usr);
        try {
            //wrong login
            if (res.isBeforeFirst()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Username already taken!"));
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }        

        if (isValid()) {

            //vytvorenie directory pre usera
            userDirectory();

            byte[] salt = salt(8);
            String pwdhash = null;
            try {
                pwdhash = Hash(pwd, salt);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
            // TODO: vytiahnut kluce zo suborov, ulozit do DB

            DBUtils.insertUser(dbConn, USERS_TABLE_NAME, usr, pwd, pwdhash, salt, privateKey, publicKey);
            HttpSession session = SessionUtils.getSession();
            session.setAttribute("username", usr);
            return handleLogin();
        } else {
            for (String msg : messageFail) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(msg));
            }
            return null;
        }
    }

    public static String Hash(String pwd, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //hash
        KeySpec spec = new PBEKeySpec(pwd.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        String pwdhash = Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        return pwdhash;
    }

    private byte[] salt(int sizeInBytes) {
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
        if (session != null && session.getAttribute("username") != null) {
            String usrS = session.getAttribute("username").toString();
            if (usrS.equals(usr))
                return true;
        }

        ResultSet res = DBUtils.selectUser(dbConn, USERS_TABLE_NAME, u, p);
        try {
            //wrong login
            if (!res.isBeforeFirst())
                return false;
                //correct login
            else
                return true;

        } catch (Exception ex) {
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

    public void createDictionary(final String nazovSuboru) throws Exception {
        final ArrayWordList awl = WordLists.createFromReader(new FileReader[]{new FileReader(nazovSuboru)}, false, new ArraysSort());
        slovnik = new WordListDictionary(awl);
    }

    public boolean isValid() throws Exception {
        createDictionary("F:\\Downloads\\10minpasswds.txt");
        final CharacterCharacteristicsRule pravidla = new CharacterCharacteristicsRule(3,
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1));

        final WhitespaceRule whitespaceRule = new WhitespaceRule();

        final LengthRule lengthRule = new LengthRule(8, 16);

        final DictionarySubstringRule dictRule = new DictionarySubstringRule(slovnik);
        dictRule.setMatchBackwards(true);

        final IllegalSequenceRule illegalSequenceRule = new IllegalSequenceRule(EnglishSequenceData.USQwerty);

        final IllegalSequenceRule alphaRule = new IllegalSequenceRule(EnglishSequenceData.Alphabetical);

        final IllegalSequenceRule numRule = new IllegalSequenceRule(EnglishSequenceData.Numerical);

        final RepeatCharacterRegexRule dupRule = new RepeatCharacterRegexRule();

        rules.add(pravidla);
        rules.add(whitespaceRule);
        rules.add(lengthRule);
        rules.add(dictRule);
        rules.add(illegalSequenceRule);
        rules.add(alphaRule);
        rules.add(numRule);
        rules.add(dupRule);

        final PasswordValidator validator = new PasswordValidator(rules);
        final RuleResult result = validator.validate(new PasswordData(pwd));
        if (result.isValid()) {
            return true;
        } else {
            for (String msg : validator.getMessages(result)) {
                System.out.println("chyba: " + msg);
            }
            this.messageFail = validator.getMessages(result);
            return false;
        }

    }
    @PreDestroy
    public void releaseConnection() {
        System.out.println("Closed DB connection.");
        try {
        dbConn.close();
        }catch(Exception ex){ex.printStackTrace();}
    }

    public void userDirectory(){
        String savePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + SAVE_FOLDER + usr+"\\";
        File file = new File(savePath);
        if (!file.exists()) {
            if (file.mkdir()) {
                System.out.println("Directory is created!");
            } else {
                System.out.println("Failed to create directory!");
            }
        }
    }
}




