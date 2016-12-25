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
public class ResultDoc {

    private final int _id;
    private String _title = "[no title]";
    private String _content = "[no content]";
    private String _docUrl = "[no url]";
    private boolean _isClicked = false;

    public ResultDoc() {
        _id = -1;
    }

    public ResultDoc(int id) {
        _id = id;
    }

    public int getId() {
        return _id;
    }

    public String getTitle() {
        return _title;
    }

    public ResultDoc setTitle(String nTitle) {
        _title = nTitle;
        return this;
    }

    public String getContent() {
        return _content;
    }

    public String getUrl() {
        return _docUrl;
    }

    public ResultDoc setContent(String nContent) {
        _content = nContent;
        return this;
    }

    public ResultDoc setUrl(String nContent) {
        _docUrl = nContent;
        return this;
    }

    public ResultDoc setClicked() {
        _isClicked = true;
        return this;
    }

    public boolean isClicked() {
        return _isClicked;
    }
}
