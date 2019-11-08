package auth;

import main.*;
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
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

@ApplicationScoped
@ManagedBean(name="loginManagerBean")
public class LoginManagerBean {
    
    //username
    private String usr;
    
    //password
    private String pwd;

    public String getUsr() {
        return usr;
    }

    public String getPwd() {
        return pwd;
    }

    public void setUsr(String usr) {
        this.usr = usr;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
    
    
    
    //handle incoming login data
    public String handleLogin() {
        if(usr.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your username."));
            return null;
        }        
        if(pwd.length() < 1) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Please enter your password."));
            return null;
        }
        
        boolean valid = validateLogin(this.usr, this.pwd);
        if(!valid) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Username/password combination is incorrect."));
            return null;
        }
        else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Login successful."));
            HttpSession session = SessionUtils.getSession();
            session.setAttribute("username", usr);
            return "index";
        }
    }
    
    public String handleLogout() {
        HttpSession session = SessionUtils.getSession();
        session.invalidate();
        return "login";
    }
    
    private boolean validateLogin(String u, String p) {
        /* 
            TODO:
            Validate if user submitted valid credentials.
            Check user DB and if the credentials are valid, return TRUE, 
            else return FALSE
        */
        return true;
    }
    
}
