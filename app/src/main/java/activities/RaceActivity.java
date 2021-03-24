package activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

import helpers.FirebaseConfig;
import helpers.FirebaseUserHelper;
import helpers.Local;
import models.Destine;
import models.Request;
import models.User;
import pedroadmn.uberclone.com.R;

public class RaceActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Button btAcceptRace;
    private FloatingActionButton fabRote;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private final int INTERVAL = 3000;
    private final int FASTEST_INTERVAL = 1000;
    private final int UPDATE_NUMBER = 1;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private SettingsClient mSettingsClient;
    public static final int WIFI_PERMISSION_RESULT = 888;

    private GoogleMap mMap;

    private LatLng driverLocation;
    private LatLng passengerLocation;

    private User driver;
    private User passenger;

    private String requestId;

    private Request request;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseRef;

    private Marker driverMarker;
    private Marker passengerMarker;
    private Marker destineMarker;

    private String requestStatus;

    private boolean activeRequest;

    private Destine destine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race);

        getSupportActionBar().setTitle("Start trip");

        initializeComponent();

        btAcceptRace.setOnClickListener(v -> acceptRace());

        requestPermissionIfRequired();

        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.containsKey("requestId") && bundle.containsKey("driver")) {
            driver = (User) bundle.getSerializable("driver");

            if (driver.getLatitude() != null && driver.getLongitude() != null) {
                driverLocation = new LatLng(Double.parseDouble(driver.getLatitude()), Double.parseDouble(driver.getLongitude()));
            }

            requestId = bundle.getString("requestId");
            activeRequest = bundle.getBoolean("activeRequest");

            verifyStatusRequest();
        }
    }

    private void verifyStatusRequest() {
        DatabaseReference requests = firebaseRef.child("requests").child(requestId);

        requests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                request = snapshot.getValue(Request.class);

                if (request != null) {
                    passenger = request.getPassenger();

                    passengerLocation = new LatLng(Double.parseDouble(passenger.getLatitude()),
                            Double.parseDouble(passenger.getLongitude()));

                    requestStatus = request.getStatus();
                    destine = request.getDestine();

                    updateUIBasedOnRequestStatus(requestStatus);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateUIBasedOnRequestStatus(String status) {
        switch (status) {
            case Request.STATUS_WAITING:
                waitingRequest();
                break;
            case Request.STATUS_ON_WAY:
                onWayRequest();
                break;
            case Request.STATUS_TRIP:
                tripRequest();
                break;
            case Request.STATUS_FINISHED:
                finishedRequest();
                break;
        }
    }

    private void finishedRequest() {
        fabRote.setVisibility(View.GONE);

        activeRequest = false;

        if (driverMarker != null) {
            driverMarker.remove();
        }

        if (destineMarker != null) {
            destineMarker.remove();
        }

        LatLng destineLocation = new LatLng(
                Double.parseDouble(destine.getLatitude()),
                Double.parseDouble(destine.getLongitude())
        );

        addDestineMarker(destineLocation, "Destine");

        centerMarker(destineLocation);

        float distance = Local.calculateDistance(passengerLocation, destineLocation);
        float price = distance * 3;

        if (price < 5) {
            price = 6.60f;
        }
        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        String result = decimalFormat.format(price);

        btAcceptRace.setText("Finalized Trip: R$ " + result);
    }

    private void centerMarker(LatLng location) {
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(location, 20)
        );
    }

    private void tripRequest() {
        fabRote.setVisibility(View.VISIBLE);
        btAcceptRace.setText("On way to destine");

        addDriverMarker(driverLocation, driver.getName());

        LatLng destineLocation = new LatLng(Double.parseDouble(destine.getLatitude()), Double.parseDouble(destine.getLongitude()));
        addDestineMarker(destineLocation, "Destine");

        centerMarkers(driverMarker, destineMarker);

        initMonitoring(driver, destineLocation, Request.STATUS_FINISHED);

    }

    private void onWayRequest() {
        btAcceptRace.setText("Going to passenger location");
        fabRote.setVisibility(View.VISIBLE);
        addDriverMarker(driverLocation, driver.getName());
        addPassengerMarker(passengerLocation, passenger.getName());

        centerMarkers(driverMarker, passengerMarker);

        initMonitoring(driver, passengerLocation, Request.STATUS_TRIP);
    }

    private void initMonitoring(User userOrigin, LatLng destine, String status) {
        DatabaseReference userLocal = FirebaseConfig.getFirebase().child("local_user");
        GeoFire geoFire = new GeoFire(userLocal);

        Circle circle = mMap.addCircle(
                new CircleOptions()
                        .center(destine)
                        .radius(100)
                        .fillColor(Color.argb(90, 255, 153, 0))
                        .strokeColor(Color.argb(190, 255, 152, 0)));

        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(destine.latitude, destine.longitude),
                0.05
        );

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (key.equals(userOrigin.getUserId())) {
                    request.setStatus(status);
                    request.updateStatus();

                    geoQuery.removeAllListeners();
                    circle.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void centerMarkers(Marker marker1, Marker marker2) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(marker1.getPosition());
        builder.include(marker2.getPosition());

        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int internSpace = (int)(width * 0.2);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, width, height, internSpace));
    }

    private void waitingRequest() {
        btAcceptRace.setText("Accept race");

        addDriverMarker(driverLocation, driver.getName());

        centerMarker(driverLocation);
    }

    private void addDriverMarker(LatLng location, String title) {
        if (driverMarker != null) {
            driverMarker.remove();
        }

        driverMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(location)
                        .title(title)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );
    }

    private void addDestineMarker(LatLng location, String title) {
        if (passengerMarker != null) {
            passengerMarker.remove();
        }

        if (destineMarker != null) {
            destineMarker.remove();
        }

        destineMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(location)
                        .title(title)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );
    }

    private void addPassengerMarker(LatLng location, String title) {
        if (passengerMarker != null) {
            passengerMarker.remove();
        }

        passengerMarker = mMap.addMarker(
                new MarkerOptions()
                        .position(location)
                        .title(title)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );
    }

    private void requestPermissionIfRequired() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, WIFI_PERMISSION_RESULT);
            }
        }
    }

    private void requestLocation() {
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL)
                .setNumUpdates(UPDATE_NUMBER);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                FirebaseUserHelper.updateLocationData(latitude, longitude);

                driverLocation = new LatLng(latitude, longitude);

                updateUIBasedOnRequestStatus(requestStatus);
            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
    }

    private void startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, locationSettingsResponse -> {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        startLocationUpdates();
    }

    private void initializeComponent() {
        btAcceptRace = findViewById(R.id.btAcceptRace);
        fabRote = findViewById(R.id.fabRote);

        fabRote.setOnClickListener(view -> {
            String status = requestStatus;

            if (status != null && !status.isEmpty()) {
                String lat = "";
                String lon = "";

                switch (status) {
                    case Request.STATUS_ON_WAY:
                        lat = String.valueOf(passengerLocation.latitude);
                        lon = String.valueOf(passengerLocation.longitude);
                        break;
                    case Request.STATUS_TRIP:
                        lat = destine.getLatitude();
                        lon = destine.getLongitude();
                        break;
                }

                String latLong = lat + "," + lon;

                Uri uri = Uri.parse("google.navigation:q=" + latLong +"&mode=d");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        firebaseAuth = FirebaseConfig.getAuthFirebase();
        firebaseRef = FirebaseConfig.getFirebase();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void acceptRace() {
        request = new Request();
        request.setId(requestId);
        request.setDriver(driver);
        request.setStatus(Request.STATUS_ON_WAY);

        request.update();

        activeRequest = true;
    }

    @Override
    public void onMapReady(com.google.android.gms.maps.GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (activeRequest) {
            Toast.makeText(this, "It is necessary close the current request", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, RequestsActivity.class);
            startActivity(intent);
        }
        return false;
    }
}