package models;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;

import helpers.FirebaseConfig;

public class User {

    private String userId;
    private String name;
    private String email;
    private String password;
    private String type;

    public User() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Exclude
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void save() {
        DatabaseReference firebaseRef = FirebaseConfig.getFirebase();
        DatabaseReference usersRef = firebaseRef.child("users").child(getUserId());
        usersRef.setValue(this);
    }
}
