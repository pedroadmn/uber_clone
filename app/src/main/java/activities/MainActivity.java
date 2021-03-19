package activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import pedroadmn.uberclone.com.R;

public class MainActivity extends AppCompatActivity {

    private Button btEnter;
    private Button btRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeComponents();

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
}