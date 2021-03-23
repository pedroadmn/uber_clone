package activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import helpers.FirebaseConfig;
import helpers.FirebaseUserHelper;
import models.Destine;
import models.Request;
import models.User;
import pedroadmn.uberclone.com.R;

public class PassengerActivity extends AppCompatActivity implements OnMapReadyCallback {

    /**
     * Lat/Lon passenger: -23.562791, -46.654668
     * Lat/Lon destine: -23.556407, -46.662365
     * Lat/Lon driver:
     * -- Initial: -23.563196, -46.650607
     * -- Intermediate: -23.564801, -46.652196
     * -- Final: -23.563136, -46.654247
     */

    private EditText etDestineLocation;
    private Button btCallUber;
    private LinearLayout llLocations;

    private GoogleMap mMap;

    private FirebaseAuth firebaseAuth;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private final int INTERVAL = 3000;
    private final int FASTEST_INTERVAL = 1000;
    private final int UPDATE_NUMBER = 1;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private SettingsClient mSettingsClient;

    private LatLng myLocal;

    public static final int WIFI_PERMISSION_RESULT = 888;

    private boolean calledUber = false;

    private DatabaseReference firebaseRef;

    private Request request;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passenger);

        initializeComponents();

        getSupportActionBar().setTitle("Start a new trip");

        btCallUber.setOnClickListener(v -> callUber());
        
        requestPermissionIfRequired();
    }

    private void verifyRequestStatus() {
        User loggedUser = FirebaseUserHelper.getLoggedUserInfo();
        DatabaseReference requests = firebaseRef.child("requests");
        Query requestSearch = requests.orderByChild("passenger/userId")
                .equalTo(loggedUser.getUserId());

        requestSearch.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Request> requestList = new ArrayList<>();

                for (DataSnapshot ds: snapshot.getChildren()) {
                    requestList.add(ds.getValue(Request.class));
                }

                if (!requestList.isEmpty()) {
                    Collections.reverse(requestList);
                    request = requestList.get(0);


                    switch (request.getStatus()) {
                        case Request.STATUS_WAITING:
                            llLocations.setVisibility(View.GONE);
                            btCallUber.setText("Cancel");
                            calledUber = true;
                            break;
                        case Request.STATUS_FINISHED:
                            llLocations.setVisibility(View.VISIBLE);
                            btCallUber.setText("Call Uber");
                            calledUber = false;
                            break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void callUber() {
        if (!calledUber) {
            String destineAddress = etDestineLocation.getText().toString();

            if (destineAddress != null && !destineAddress.equals("")) {
                Address address = getAddress(destineAddress);

                if (address != null) {
                    Destine destine = new Destine();
                    destine.setCity(address.getAdminArea());
                    destine.setPostalCode(address.getPostalCode());
                    destine.setNeighborhood(address.getSubLocality());
                    destine.setStreet(address.getThoroughfare());
                    destine.setNumber(address.getFeatureName());
                    destine.setLatitude(String.valueOf(address.getLatitude()));
                    destine.setLongitude(String.valueOf(address.getLongitude()));

                    StringBuilder message = new StringBuilder();
                    message.append("City: " + destine.getCity());
                    message.append("\nStreet: " + destine.getStreet());
                    message.append("\nNeighborhood: " + destine.getNeighborhood());
                    message.append("\nNumber: " + destine.getNumber());
                    message.append("\nPostal code: " + destine.getPostalCode());

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this)
                            .setTitle("Confirm the address")
                            .setMessage(message)
                            .setPositiveButton("Confirm", (dialogInterface, i) -> {
                                saveRequest(destine);
                            })
                            .setNegativeButton("Cancel", (dialogInterface, i) -> {

                            });

                    AlertDialog dialog = alertDialogBuilder.create();
                    dialog.show();
                }
            } else {
                Toast.makeText(this, "Fill the destine address", Toast.LENGTH_SHORT).show();
            }
        } else {
            calledUber = false;

        }
    }

    private void saveRequest(Destine destine) {
        Request request = new Request();
        request.setDestine(destine);

        User passengerUser = FirebaseUserHelper.getLoggedUserInfo();
        passengerUser.setLongitude(String.valueOf(myLocal.longitude));
        passengerUser.setLatitude(String.valueOf(myLocal.latitude));

        request.setPassenger(passengerUser);
        request.setStatus(Request.STATUS_WAITING);

        request.save();

        llLocations.setVisibility(View.GONE);
        btCallUber.setText("Cancel");
        calledUber = true;
    }

    private Address getAddress(String destineAddress) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addressList = geocoder.getFromLocationName(destineAddress, 1);

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                return address;
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return null;
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

                FirebaseUserHelper.updateLocationData(latitude, longitude);

                mMap.clear();
                mMap.addMarker(
                        new MarkerOptions()
                                .position(myLocal)
                                .title("My Local")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.passenger_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logoutMenu:
                logout();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        try {
            firebaseAuth.signOut();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void initializeComponents() {
        etDestineLocation = findViewById(R.id.etDestineLocation);
        btCallUber = findViewById(R.id.buttonCallUber);
        llLocations = findViewById(R.id.llLocations);

        firebaseAuth = FirebaseConfig.getAuthFirebase();
        firebaseRef = FirebaseConfig.getFirebase();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        startLocationUpdates();

        verifyRequestStatus();
    }
}