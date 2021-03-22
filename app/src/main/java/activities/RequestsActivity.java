package activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.SupportMapFragment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        initializeComponents();

        getSupportActionBar().setTitle("Trip Requests");
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

        requestAdapter = new RequestAdapter(requestList, this, driver);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        rvRequests.setLayoutManager(layoutManager);
        rvRequests.setHasFixedSize(true);
        rvRequests.setAdapter(requestAdapter);

        firebaseAuth = FirebaseConfig.getAuthFirebase();
        firebaseRef = FirebaseConfig.getFirebase();
    }

    private void getRequests() {
        DatabaseReference requests = firebaseRef.child("requests");

        Query requestSearch = requests.orderByChild("status").equalTo(Request.STATUS_WAITING);

        requestSearch.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() > 0) {
                    tvWaitingRequests.setVisibility(View.GONE);
                    rvRequests.setVisibility(View.VISIBLE);

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

        getRequests();
    }
}