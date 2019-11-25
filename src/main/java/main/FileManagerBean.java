package main;

import auth.DBUtils;
import auth.RSAParser;
import auth.SessionUtils;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.Part;

@RequestScoped
@ManagedBean(name="fileManagerBean")
public class FileManagerBean {

    private Connection dbConn;

    private final String SAVE_FOLDER = "\\temp\\";

    private Part part;

    //file to work with
    private File userFile;
    
    private String fileIdString;

    private String pwd;

    private String downloadType;

    public Part getPart() {
        return part;
    }

    public File getFile() {
        return userFile;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public void setFile(File file) {
        this.userFile = file;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void setFileIdString(String fileIdString) {
        this.fileIdString = fileIdString;
    }

    public String getFileIdString() {
        return fileIdString;
    }
    
    

    //handle file uploads
    public void handleUpload(){
        if(part == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please choose a file to upload."));
            return;
        }
       // if(aKey.length() < 1) {
        //   FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please specify an asymmetrical key."));
        //   return;
        //}

        FileHandler h = new FileHandler();
        String userName = SessionUtils.getUserName();
        String pubKey = DBUtils.getUserPublicKey(dbConn, userName);
        
        File f = h.handleUpload(part, pubKey);
        if(f != null) {
            String username = SessionUtils.getUserName();
            String fName = f.getName();
            String fPath = f.getPath();
            DBUtils.insertFile(dbConn, "files", "users", fName, fPath, username);
            DBUtils.grantAccess(dbConn, "access", "files", "users", fName, fPath, username);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File " + f.getName() + " uploaded successfully."));
        }
        else
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Failed to upload file "));
    }

    public void handleSharedFileUpload(byte[] filePart, String fileName, String pubKey, String usernameToShare){

        FileHandler h = new FileHandler();
        File f = h.handleSharedFileUpload(filePart,fileName,pubKey);
        if(f != null) {
            String username = usernameToShare;
            String fName = f.getName();
            String fPath = f.getPath();
            DBUtils.insertFile(dbConn, "files", "users", fName, fPath, username);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File " + f.getName() + " uploaded successfully."));
        }
        else
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Failed to upload file "));
    }

    //handle file downloads
    public void handleDownload() throws Exception {
       
        String fileName = "";
        String filePath = "";
        Integer fileId = 0;
        try {
            fileId = Integer.parseInt(fileIdString);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
        ResultSet res1 = DBUtils.selectFile(dbConn, "files", fileId);
        try {
            if(res1.isBeforeFirst()) {
                fileName = res1.getString("filename");
                filePath = res1.getString("path");
            }                
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        userFile = new File(filePath);

        System.out.println("File downloaded: " + userFile.getAbsolutePath());
        FileHandler h = new FileHandler();
        /*
            Decide if we want to download the file as encrypted or decrypted.
            toDownload contains the final file to be downloaded
        */
        File toDownload = null;
        String userName = SessionUtils.getUserName();
        switch(downloadType){
            case "e":
                /*  
                    Download 'file' as Encrypted, simply pass the file saved on server for download
                */
                toDownload = userFile;
                if(userFile == null) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please upload a file first."));
                    return;
                }
                break;

            case "d":
                if(userFile == null) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please upload a file first."));
                    return;
                }
                
                if(pwd.length() < 1) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your password to use this option."));
                    return;
                }

                String pKey = DBUtils.getPrivateKey(dbConn, "users", userName, pwd);
                byte[] fileContent = Files.readAllBytes(userFile.toPath());
                byte[] plainText = CryptoUPB.decrypt(fileContent, pKey);

//                File fileToCreate = null;
//                String savePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("");
//                System.out.println(savePath);
//                String fileName = "kokotko.txt";
//                fileToCreate = new File(savePath, fileName);
//
//                FileOutputStream outputStream = new FileOutputStream(fileToCreate);
//                outputStream.write(plainText);
//                outputStream.flush();
//                outputStream.close();
                h.handleDownload(plainText, userFile);
//                fileToCreate.delete();
                break;
//

            case "a":
                if(userFile == null) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please upload a file first."));
                    return;
                }
                File app =  new File(FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + "\\temp\\" + "cryptoApp.jar");
                toDownload = app;

                break;


            case "prk":
                if(pwd.length() < 1) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your password to use this option."));
                    return;
                }
                
                String prKey = DBUtils.getPrivateKey(dbConn, "users", userName, pwd);
                prKey = new RSAParser().setPrivateKey(prKey);
                toDownload = new File("privateKey.pem");
                h.handleDownload(prKey.getBytes(), toDownload);


                break;

            case "pubk":
                toDownload = new File("publicKey.pem");
                String pubKey = DBUtils.getUserPublicKey(dbConn, userName);
                pubKey = new RSAParser().setPublicKey(pubKey);
                h.handleDownload(pubKey.getBytes(), toDownload);
            break;

        }

        if(toDownload != null)
            h.handleDownload(toDownload);
    }


    public void attrListener(ActionEvent event){

	downloadType = (String)event.getComponent().getAttributes().get("dlType");
    }

    @PostConstruct
    public void init() {
        /* Initialize SQLite DB connection */
        String dbPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + SAVE_FOLDER;
        String dbFileName = "users.db";
        dbConn = DBUtils.initDB(dbPath, dbFileName);
    }

    @PreDestroy
    public void releaseConnection() {
        System.out.println("Closed DB connection.");
        try {
        dbConn.close();
        }catch(Exception ex){ex.printStackTrace();}
    }
}
