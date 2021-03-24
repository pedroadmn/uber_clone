package helpers;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class Local {

    public static float calculateDistance(LatLng latLonInitial, LatLng latLonDestine) {
        Location initialLocation = new Location("Initial Location");
        initialLocation.setLatitude(latLonInitial.latitude);
        initialLocation.setLongitude(latLonInitial.longitude);

        Location finalLocation = new Location("Final Location");
        finalLocation.setLatitude(latLonDestine.latitude);
        finalLocation.setLongitude(latLonDestine.longitude);

        float distance = initialLocation.distanceTo(finalLocation) / 1000;
        return distance;
    }
}
