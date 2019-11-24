package main;


import java.io.*;
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
        File keyFileToCreate = null;    // TODO : nahratie async key
        try {
            byte[] fileContent = new byte[(int) filePart.getSize()];
            InputStream in = filePart.getInputStream();
            in.read(fileContent);
            keyFileToCreate = new File(savePath, fileName + ".key");

            //tu prebieha sifrovanie, vratia sa zasifrovane byte-i
            // treba spravit aby aKey bol File
            fileContent = CryptoUPB.encrypt(fileContent, aKey);


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

    public File handleSharedFileUpload(byte[] filePart, String fileName, String pubKey){
        if(filePart == null)
            return null;

        String savePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + SAVE_FOLDER;
        File fileToCreate = null;
        try {
            filePart = CryptoUPB.encrypt(filePart, pubKey);


            fileToCreate = new File(savePath, fileName);
            File folder = new File(savePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            FileOutputStream fileOutStream = new FileOutputStream(fileToCreate);
            fileOutStream.write(filePart);
            fileOutStream.flush();
            fileOutStream.close();
        }
        catch (IOException e) {} catch (Exception e) {
            e.printStackTrace();
        }
        return fileToCreate;
    }

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

    public void handleDownload(byte[] file, File extensionFile) {
        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setHeader("Content-Disposition", "attachment;filename=" + extensionFile.getName());
        response.setContentLength((int) file.length);

        ServletOutputStream out = null;
        try {
            InputStream input = new ByteArrayInputStream(file);
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
