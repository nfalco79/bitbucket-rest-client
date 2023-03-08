package com.github.nfalco79.bitbucket.client.model;

import java.io.Serializable;

public class ReportData implements Serializable {
    private static final long serialVersionUID = -6436611882901128821L;

    private String title;
    private String type;
    private Object value;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
