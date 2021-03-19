package activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import helpers.FirebaseUserHelper;
import helpers.Permissions;
import pedroadmn.uberclone.com.R;

public class MainActivity extends AppCompatActivity {

    private Button btEnter;
    private Button btRegister;

    private String[] permissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();

        Permissions.validatePermissions(permissions, this, 1);

        btEnter.setOnClickListener(v -> goToLoginScreen());
        btRegister.setOnClickListener(v -> goToRegisterScreen());

        getSupportActionBar().hide();
    }

    private void goToRegisterScreen() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void goToLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void initializeComponents() {
        btEnter = findViewById(R.id.btEnter);
        btRegister = findViewById(R.id.btRegister);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUserHelper.redirectLoggedUser(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int resultPermission : grantResults) {
                if (resultPermission == PackageManager.PERMISSION_DENIED) {
                    validatePermissionAlert();
                }
        }
    }

    private void validatePermissionAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Denied Permissions");
        builder.setMessage("To use the app is necessary accept the permissions");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}