package com.example.smsserver;

public class LoginRequest {
    private String username;
    private String password;
    private String phonenumber;

    private String version;

    public LoginRequest(String username , String password, String phonenumber, String version) {
        this.username = username;
        this.password = password;
        this.phonenumber = phonenumber;
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }
}
