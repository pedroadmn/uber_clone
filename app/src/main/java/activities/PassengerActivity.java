package activities;

import android.Manifest;
import android.content.Intent;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import helpers.FirebaseConfig;
import helpers.FirebaseUserHelper;
import helpers.Local;
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

    private boolean cancelUber = false;

    private DatabaseReference firebaseRef;

    private Request request;

    private User passenger;
    private User driver;

    private String requestStatus;
    private Destine destine;
    private LatLng passengerLocation;
    private LatLng driverLocation;

    private Marker driverMarker;
    private Marker passengerMarker;
    private Marker destineMarker;

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

                for (DataSnapshot ds : snapshot.getChildren()) {
                    requestList.add(ds.getValue(Request.class));
                }

                if (!requestList.isEmpty()) {
                    Collections.reverse(requestList);
                    request = requestList.get(0);

                    if (request != null) {
                        if (!request.getStatus().equals(Request.STATUS_CLOSED)) {
                            passenger = request.getPassenger();

                            passengerLocation = new LatLng(Double.parseDouble(passenger.getLatitude()),
                                    Double.parseDouble(passenger.getLongitude()));

                            requestStatus = request.getStatus();
                            destine = request.getDestine();

                            if (request.getDriver() != null) {
                                driver = request.getDriver();
                                driverLocation = new LatLng(Double.parseDouble(driver.getLatitude()), Double.parseDouble(driver.getLongitude()));
                            }

                            updateUIBasedOnRequestStatus(requestStatus);
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateUIBasedOnRequestStatus(String status) {
        if (status != null && !status.isEmpty()) {
            cancelUber = false;
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
                case Request.STATUS_CANCELED:
                    canceledRequest();
                    break;
            }
        } else {
            addPassengerMarker(passengerLocation, "Your location");
        }
    }

    private void canceledRequest() {
        llLocations.setVisibility(View.VISIBLE);
        btCallUber.setText("Call Uber");
        cancelUber = false;
    }

    private void finishedRequest() {
        llLocations.setVisibility(View.GONE);

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

        btCallUber.setText("Finalized Trip: R$ " + result);
        btCallUber.setEnabled(false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Price")
                .setMessage("Finalized Trip: R$ " + result)
                .setCancelable(false)
                .setNegativeButton("Close Trip", (dialogInterface, i) -> {
                    request.setStatus(Request.STATUS_CLOSED);
                    request.updateStatus();

                    finish();
                    startActivity(new Intent(getIntent()));
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void tripRequest() {
        llLocations.setVisibility(View.GONE);
        btCallUber.setText("On Destine way");
        btCallUber.setEnabled(false);

        addDriverMarker(driverLocation, driver.getName());

        LatLng destineLocation = new LatLng(
                Double.parseDouble(destine.getLatitude()),
                Double.parseDouble(destine.getLongitude())
        );

        addDestineMarker(destineLocation, "Destine");

        centerMarkers(driverMarker, destineMarker);
    }

    private void onWayRequest() {
        llLocations.setVisibility(View.GONE);
        btCallUber.setText("Driver on way");
        btCallUber.setEnabled(false);

        addPassengerMarker(passengerLocation, passenger.getName());
        addDriverMarker(driverLocation, driver.getName());

        centerMarkers(passengerMarker, driverMarker);
    }

    private void waitingRequest() {
        llLocations.setVisibility(View.GONE);
        btCallUber.setText("Cancel");
        cancelUber = true;

        addPassengerMarker(passengerLocation, passenger.getName());

        centerMarker(passengerLocation);
    }

    private void centerMarkers(Marker marker1, Marker marker2) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(marker1.getPosition());
        builder.include(marker2.getPosition());

        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int internSpace = (int) (width * 0.2);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, width, height, internSpace));
    }

    private void centerMarker(LatLng location) {
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(location, 20)
        );
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

    private void callUber() {
        if (cancelUber) {
            request.setStatus(Request.STATUS_CANCELED);
            request.updateStatus();
        } else {
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

                updateUIBasedOnRequestStatus(requestStatus);

                if (requestStatus != null && !requestStatus.isEmpty()) {
                    if (requestStatus.equals(Request.STATUS_TRIP) || requestStatus.equals(Request.STATUS_FINISHED)) {
                        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                    } else {
                        if (ActivityCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(PassengerActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(PassengerActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                        } else {
                            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                    mLocationCallback, Looper.myLooper());
                        }
                    }
                }
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