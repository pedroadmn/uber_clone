package activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
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

import java.util.ArrayList;
import java.util.List;

import adapters.RequestAdapter;
import helpers.FirebaseConfig;
import helpers.FirebaseUserHelper;
import listeners.RecyclerItemClickListener;
import models.Request;
import models.User;
import pedroadmn.uberclone.com.R;

public class RequestsActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference firebaseRef;

    private RecyclerView rvRequests;
    private RequestAdapter requestAdapter;
    private TextView tvWaitingRequests;

    private List<Request> requestList = new ArrayList<>();

    private User driver;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private final int INTERVAL = 3000;
    private final int FASTEST_INTERVAL = 1000;
    private final int UPDATE_NUMBER = 1;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private SettingsClient mSettingsClient;
    public static final int WIFI_PERMISSION_RESULT = 888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        initializeComponents();

        getSupportActionBar().setTitle("Trip Requests");

        requestPermissionIfRequired();
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

    private void verifyRequestStatus() {
        User loggedUser = FirebaseUserHelper.getLoggedUserInfo();
        DatabaseReference requests = firebaseRef.child("requests");
        Query requestSearch = requests.orderByChild("driver/userId").equalTo(loggedUser.getUserId());

        requestSearch.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Request request = ds.getValue(Request.class);

                    if (request.getStatus().equals(Request.STATUS_ON_WAY) || request.getStatus().equals(Request.STATUS_TRIP)) {
                        openRaceScreen(request.getId(), driver, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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

                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());

                FirebaseUserHelper.updateLocationData(location.getLatitude(), location.getLongitude());

                if (!latitude.isEmpty() && !longitude.isEmpty()) {
                    driver.setLatitude(latitude);
                    driver.setLongitude(longitude);

                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);

                    requestAdapter.notifyDataSetChanged();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.driver_menu, menu);

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
        rvRequests = findViewById(R.id.rvRequests);
        tvWaitingRequests = findViewById(R.id.tvWaitingRequests);

        driver = FirebaseUserHelper.getLoggedUserInfo();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        requestAdapter = new RequestAdapter(requestList, this, driver);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rvRequests.setLayoutManager(layoutManager);
        rvRequests.setHasFixedSize(true);
        rvRequests.setAdapter(requestAdapter);

        rvRequests.addOnItemTouchListener(new RecyclerItemClickListener(
                getApplicationContext(),
                rvRequests,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Request request = requestList.get(position);
                        openRaceScreen(request.getId(), driver, false);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    }
                }
        ));

        firebaseAuth = FirebaseConfig.getAuthFirebase();
        firebaseRef = FirebaseConfig.getFirebase();
    }

    private void openRaceScreen(String requestId, User driver, boolean activeRequest) {
        Intent intent = new Intent(RequestsActivity.this, RaceActivity.class);
        intent.putExtra("requestId", requestId);
        intent.putExtra("driver", driver);
        intent.putExtra("activeRequest", activeRequest);
        startActivity(intent);
    }

    private void getRequests() {
        DatabaseReference requests = firebaseRef.child("requests");

        Query requestSearch = requests.orderByChild("status").equalTo(Request.STATUS_WAITING);

        requestSearch.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                if (snapshot.getChildrenCount() > 0) {
                    tvWaitingRequests.setVisibility(View.GONE);
                    rvRequests.setVisibility(View.VISIBLE);

                    requestList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Request request = ds.getValue(Request.class);
                        requestList.add(request);
                    }

                    requestAdapter.notifyDataSetChanged();
                } else {
                    tvWaitingRequests.setVisibility(View.VISIBLE);
                    rvRequests.setVisibility(View.GONE);
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        verifyRequestStatus();

        startLocationUpdates();

        getRequests();
    }
}