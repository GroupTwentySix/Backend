package cc.grouptwentysix.vitality.model;

import java.util.List;

public class User {
    private String id;
    private String username;
    private String email;
    private String password;
    private String role;
    private String ip;
    private List<String> basket;
    private boolean emailVerified;
    private String verificationToken;

    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public List<String> getBasket() {
        return basket;
    }

    public void setBasket(List<String> basket) {
        this.basket = basket;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }



    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' + 
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                ", ip='" + ip + '\'' +
                ", basket=" + basket +
                ", emailVerified=" + emailVerified +
                ", verificationToken='" + verificationToken + '\'' +
                '}';
    }
}