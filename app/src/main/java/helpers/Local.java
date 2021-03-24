package helpers;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

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

    public static String formattedDistance(float distance) {
        String formattedDistance;
        if (distance < 1) {
            distance = distance * 1000;
            formattedDistance = Math.round(distance) + "m";
        } else {
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            formattedDistance = decimalFormat.format(distance) + "km";
        }

        return formattedDistance;
    }
}
