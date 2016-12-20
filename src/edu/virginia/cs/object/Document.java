/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.virginia.cs.object;

/**
 *
 * @author wua4nw
 */
public class Document {

    private int document_id;
    private String document_url;
    private String document_text;

    public int getDocument_id() {
        return document_id;
    }

    public void setDocument_id(int document_id) {
        this.document_id = document_id;
    }

    public String getDocument_url() {
        return document_url;
    }

    public void setDocument_url(String document_url) {
        this.document_url = document_url;
    }

    public String getDocument_text() {
        return document_text;
    }

    public void setDocument_text(String document_text) {
        this.document_text = document_text;
    }

}
