package com.example.smsserver;

public class Sms {
    private String parent;

    public Sms(String parent) {
        this.parent = parent;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}
