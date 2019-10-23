package main;

import java.io.File;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.Part;

@ViewScoped
@ManagedBean(name="fileManagerBean")
public class FileManagerBean {
    private Part part;
    private File file;

    public Part getPart() {
        return part;
    }

    public File getFile() {
        return file;
    }

    public void setPart(Part part) {
        this.part = part;
    }

    public void setFile(File file) {
        this.file = file;
    }
    
    public void handleUpload(){
        FileHandler h = new FileHandler();
        File f = h.handleUpload(part);
        if(f != null) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("File " + f.getName() + " uploaded successfully."));
            file = f;
        }
        else
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please choose a file to upload."));
    }
    
    public void handleDownload(){
        FileHandler h = new FileHandler();
        if(file != null)
            h.handleDownload(file);
        else
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please upload a file first."));
    }
}
