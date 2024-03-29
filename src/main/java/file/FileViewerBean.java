package file;

import auth.DBUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ViewScoped
@ManagedBean(name="fileViewerBean")
public class FileViewerBean {
    
    private final String SAVE_FOLDER = "\\temp\\";

    //SQLite DB
    private Connection dbConn;
    
    public List<FileResult> getFileList() {
        List<FileResult> list = new ArrayList<>();

        ResultSet res = DBUtils.selectAllFiles(dbConn);
        try {
            if(res.isBeforeFirst()) {
                while (res.next()) {
                    FileResult fr = new FileResult();
                    fr.setFileName(res.getString(1));
                    fr.setOwner(res.getString(2));
                    fr.setId(res.getInt(4));
                    list.add(fr);
                }               
            }                
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return list;
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
