/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

import edu.virginia.cs.model.TopicTreeNode;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author wua4nw
 */
public class Query {

    private int query_id;
    private String query_text;
    private Session query_session;
    private Date query_time;
    private ArrayList<Document> clicked_documents;
    private TopicTreeNode query_topic;

    public Query(int query_id, String query_text) {
        this.query_id = query_id;
        this.query_text = query_text;
        this.clicked_documents = new ArrayList<>();
    }

    public int getQuery_id() {
        return query_id;
    }

    public void setQuery_id(int query_id) {
        this.query_id = query_id;
    }

    public String getQuery_text() {
        return query_text;
    }

    public void setQuery_text(String query_text) {
        this.query_text = query_text;
    }

    public Session getQuery_session() {
        return query_session;
    }

    public void setQuery_session(Session query_session) {
        this.query_session = query_session;
    }

    public Date getQuery_time() {
        return query_time;
    }

    public void setQuery_time(Date query_time) {
        this.query_time = query_time;
    }

    public ArrayList<Document> getClicked_documents() {
        return clicked_documents;
    }

    public void setClicked_documents(ArrayList<Document> clicked_documents) {
        this.clicked_documents = clicked_documents;
    }

    public void addClicked_documents(Document clicked_documents) {
        this.clicked_documents.add(clicked_documents);
    }

    public TopicTreeNode getQuery_topic() {
        return query_topic;
    }

    public void setQuery_topic(TopicTreeNode query_topic) {
        this.query_topic = query_topic;
    }

}
