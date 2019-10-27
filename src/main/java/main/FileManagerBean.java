package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public void handleDownload() throws IOException {
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

                File fileToCreate = null;
                //kluc musi byt v type File
                //byte[] plainText = CryptoUPB.decrypt(userFile, pKey);
                FileOutputStream outputStream = new FileOutputStream(fileToCreate);
                //outputStream.write(plainText);
                outputStream.flush();
                outputStream.close();
                h.handleDownload(fileToCreate);
                break;
                
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
        }
        
        if(toDownload != null)
            h.handleDownload(toDownload);
    }
    
    public void attrListener(ActionEvent event){
 
	downloadType = (String)event.getComponent().getAttributes().get("dlType");
 
  }
}
