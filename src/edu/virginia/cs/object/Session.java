/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import java.util.Date;

/**
 *
 * @author wua4nw
 */
public class Session {

    private int session_id;
    private Date start_time;
    private Date end_time;

    public Session(int id) {
        this.session_id = id;
    }

    public int getSession_id() {
        return session_id;
    }

    public void setSession_id(int session_id) {
        this.session_id = session_id;
    }

    public void setStartTime(Date startTime) {
        this.start_time = startTime;
    }

    public Date getStartTime() {
        return this.start_time;
    }

    public void setEndTime(Date endTime) {
        this.end_time = endTime;
    }

    public Date getEndTime() {
        return this.end_time;
    }

}
