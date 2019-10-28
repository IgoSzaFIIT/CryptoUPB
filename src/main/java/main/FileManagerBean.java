package main;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.Part;

@ApplicationScoped
@ManagedBean(name="fileManagerBean")
public class FileManagerBean {
    private Part part;
    
    //file to work with
    private File userFile;
    
    //file containing the symmetrical key to decrypt userFile ------> nepotrebujeme v tretom tyzdni dole to je napisane
    //private File keyFile;
    
    //asymmetrical public key to encrypt the symmetrical key
    private String aKey = null;
    
    //asymmetrical private key to decrypt the symmetrical key
    private String pKey = null;

    private String downloadType;

    public String getpKey() {
        return pKey;
    }

    public void setpKey(String pKey) {
        this.pKey = pKey;
    }
    
    public String getaKey() {
        return aKey;
    }

    public void setaKey(String aKey) {
        this.aKey = aKey;
    }

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
    
    //handle file uploads
    public void handleUpload(){
        if(part == null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please choose a file to upload."));
            return;
        }
        if(aKey.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please specify an asymmetrical key."));
            return;
        }
        
        FileHandler h = new FileHandler();
        File f = h.handleUpload(part, aKey);
        if(f != null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File " + f.getName() + " uploaded successfully."));
            userFile = f;
        }
        else
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Failed to upload file "));
    }
    
    //handle file downloads
    public void handleDownload() throws Exception {
        FileHandler h = new FileHandler();
        /*
            Decide if we want to download the file as encrypted or decrypted.
            toDownload contains the final file to be downloaded
        */
        File toDownload = null;
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

                if(pKey.length() < 1) {
                    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please specify a private key for decryption."));
                    return;
                }





                //kluc musi byt v type File
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
                /*  
                    -------------------------------------------------------------------------------------------
                        TODO HERE:
                        Create an application used to decrypt the 'userFile'.
                        Set variable toDownload as the application file!
                    -------------------------------------------------------------------------------------------
                */
                
                break;


            case "k":

                generatorHandler();


            break;

            case "p":

                toDownload = new File("privateKey");
               // h.handleDownload(toDownload);


                break;

            case "u":
                toDownload = new File("publicKey");
               // h.handleDownload(toDownload);
            break;

        }
        
        if(toDownload != null)
            h.handleDownload(toDownload);
    }


    public void generatorHandler() throws NoSuchAlgorithmException, IOException {
        KPGenerator kp = new KPGenerator();

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Private Key : "+kp.getEncodedPrivKey()));
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Public Key : "+kp.getEncodedPubKey()));
        kp.saveKeys("privateKey", "publicKey");



    }
    
    public void attrListener(ActionEvent event){
 
	downloadType = (String)event.getComponent().getAttributes().get("dlType");
 
  }
}
