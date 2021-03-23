package models;

import com.google.firebase.database.DatabaseReference;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import helpers.FirebaseConfig;

public class Request implements Serializable {

    private String id;
    private String status;
    private User passenger;
    private User driver;
    private Destine destine;

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_ON_WAY = "onway";
    public static final String STATUS_TRIP = "trip";
    public static final String STATUS_FINISHED = "finished";

    public Request() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getPassenger() {
        return passenger;
    }

    public void setPassenger(User passenger) {
        this.passenger = passenger;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public Destine getDestine() {
        return destine;
    }

    public void setDestine(Destine destine) {
        this.destine = destine;
    }

    public void save() {
        DatabaseReference requestRef = FirebaseConfig.getFirebase().child("requests");
        String requestId = requestRef.push().getKey();
        setId(requestId);

        requestRef.child(getId()).setValue(this);
    }

    public void update() {
        DatabaseReference requestRef = FirebaseConfig.getFirebase().child("requests");

        DatabaseReference request = requestRef.child(getId());

        Map object = new HashMap<>();
        object.put("driver", getDriver());
        object.put("status", getStatus());

        request.updateChildren(object);
    }
}
