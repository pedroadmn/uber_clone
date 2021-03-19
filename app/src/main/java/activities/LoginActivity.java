package activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import pedroadmn.uberclone.com.R;

public class LoginActivity extends AppCompatActivity {

    private Button btEnter;
    private EditText etEmail;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeComponents();

        getSupportActionBar().setTitle("Access my account");
    }

    private void initializeComponents() {
        btEnter = findViewById(R.id.btEnter);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
    }
}