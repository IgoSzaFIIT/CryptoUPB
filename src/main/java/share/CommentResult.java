/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package share;

import java.sql.Timestamp;

public class CommentResult {
   
    private String message;
    private String sender;
    private String time;

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    
}
