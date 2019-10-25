package main;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

public class FileHandler {
    
    //change if you need to
    private final String SAVE_FOLDER = "\\temp\\";
    
    //handle file uploads
    public File handleUpload(Part filePart, String aKey){
        if(filePart == null)
            return null;
        
        String savePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + SAVE_FOLDER;
        String fileName = filePart.getSubmittedFileName();
        File fileToCreate = null;
        File keyFileToCreate = null;
        try {
            byte[] fileContent = new byte[(int) filePart.getSize()];
            InputStream in = filePart.getInputStream();
            in.read(fileContent);
            //keyFileToCreate = new File(savePath, fileName + ".key");

            //tu prebieha sifrovanie, vratia sa zasifrovane byte-i
            // treba spravit aby aKey bol File
            //****fileContent = CryptoUPB.encrypt(fileContent, aKey);*******


            fileToCreate = new File(savePath, fileName);
            File folder = new File(savePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            FileOutputStream fileOutStream = new FileOutputStream(fileToCreate);
            fileOutStream.write(fileContent);
            fileOutStream.flush();
            fileOutStream.close();
        }
        catch (IOException e) {} catch (Exception e) {
            e.printStackTrace();
        }

        return fileToCreate;
        
    }
    
    //src: https://stackoverflow.com/questions/3428039/download-a-file-with-jsf
    public void handleDownload(File file) {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();  
        
        response.setHeader("Content-Disposition", "attachment;filename=" + file.getName());  
        response.setContentLength((int) file.length());  
        
        ServletOutputStream out = null;
        try {  
            FileInputStream input = new FileInputStream(file);  
            byte[] buffer = new byte[1024];  
            out = response.getOutputStream();  
            int i = 0;  
            while((i = input.read(buffer)) != -1) {  
                out.write(buffer);  
                out.flush();  
            }  
            FacesContext.getCurrentInstance().responseComplete();  
        } 
        catch(IOException err) {  
            err.printStackTrace();  
        } 
        finally {  
            try {  
                if(out != null) {  
                    out.close();  
                }  
            } 
            catch(IOException err) {  
                err.printStackTrace();  
            }  
    }  
    }
}
