package com.blogspot.vsvydenko.multithread_site_searcher.entity;

/**
 * Created by vsvydenko on 07.09.14.
 */
public class UrlItem {

    private String name;
    private String status;
    private int found = 0;

    public UrlItem(String name, String status, int found) {
        this.name = name;
        this.status = status;
        this.found = found;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFoundNumber() {
        return found;
    }

    public void setFound(int found) {
        this.found = found;
    }
}
