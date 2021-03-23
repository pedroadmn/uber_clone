package activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import helpers.FirebaseConfig;
import models.Request;
import models.User;
import pedroadmn.uberclone.com.R;

public class RaceActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Button btAcceptRace;

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

    private LatLng myLocal;

    private User driver;
    private String requestId;

    private Request request;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseRef;

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
            requestId = bundle.getString("requestId");

            verifyStatusRequest();
        }
    }

    private void verifyStatusRequest() {
        DatabaseReference requests = firebaseRef.child("requests").child(requestId);

        requests.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                request = snapshot.getValue(Request.class);

                switch (request.getStatus()) {
                    case Request.STATUS_WAITING:
                        waitingRequest();
                        break;
                    case Request.STATUS_ON_WAY:
                        onWayRequest();
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void onWayRequest() {
        btAcceptRace.setText("Going to passenger location");
    }

    private void waitingRequest() {
        btAcceptRace.setText("Accept race");
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
                myLocal = new LatLng(latitude, longitude);

                mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position(myLocal)
                                .title("My Local")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
                );

                mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(myLocal, 18)
                );
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
    }

    @Override
    public void onMapReady(com.google.android.gms.maps.GoogleMap googleMap) {
        mMap = googleMap;
    }
}