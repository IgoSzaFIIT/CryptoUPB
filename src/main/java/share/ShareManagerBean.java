package share;

import file.*;
import auth.DBUtils;
import auth.SessionUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

@ViewScoped
@ManagedBean(name="shareManagerBean")
public class ShareManagerBean {
    
    private final String SAVE_FOLDER = "\\temp\\";

    //SQLite DB
    private Connection dbConn;
    
    //user comment to process
    private String comment;
    
    private String fileIdString;
    
    private List<CommentResult> commentList;

    public String getFileIdString() {
        return fileIdString;
    }

    public void setFileIdString(String fileIdString) {
        this.fileIdString = fileIdString;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCommentList(List<CommentResult> commentList) {
        this.commentList = commentList;
    }

    public List<CommentResult> getCommentList() {
        return commentList;
    }
    
    
    
    public void loadCommentList() {
        commentList = new ArrayList<>();
        String fileName = null;
        String filePath = null;
        Integer fileId = Integer.parseInt(fileIdString);
        
        ResultSet res1 = DBUtils.selectFile(dbConn, "files", fileId);
        try {
            if(res1.isBeforeFirst()) {
                fileName = res1.getString("filename");
                filePath = res1.getString("path");
            }                
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        ResultSet res2 = DBUtils.getFileComments(dbConn, filePath, fileName);
        try {
            if(res2.isBeforeFirst()) {
                while (res2.next()) {
                    CommentResult c = new CommentResult();
                    c.setTime(res2.getString(1));
                    c.setSender(res2.getString(2));
                    c.setMessage(res2.getString(3));
                    commentList.add(c);
                }               
            }                
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
    
    
    public String handleAddComment() {
        if(comment.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your comment first."));
            return null;
        }
        Map<String,String> params = 
            FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        
        String userFileName = params.get("fileName");
        String userFilePath = null;
        String userName = SessionUtils.getUserName();
        String userFileIdString = params.get("fileId");
        Integer userFileId = Integer.parseInt(userFileIdString);
        
        ResultSet res = DBUtils.selectFile(dbConn, "files", userFileId);
        try {
            if(res.isBeforeFirst()) {
                userFilePath = res.getString("path");
            }                
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if((userFileName != null) && (userFilePath != null) && (userName != null))
            DBUtils.addComment(dbConn, "comments", "files", "users", comment, userFileName, userFilePath, userName);
        else
            System.out.println("An error occured while submitting user comment.");
        
        return "fileDownload?fileName=" + userFileName + "&amp;fileId=" + userFileIdString + "&amp;faces-redirect=true";
    }
    
    @PostConstruct
    public void init() {
        /* Initialize SQLite DB connection */
        String dbPath = FacesContext.getCurrentInstance().getExternalContext().getRealPath("") + SAVE_FOLDER;
        String dbFileName = "users.db";
        dbConn = DBUtils.initDB(dbPath, dbFileName);
    }

}
