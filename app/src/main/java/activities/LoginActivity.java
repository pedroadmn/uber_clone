package activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import helpers.FirebaseConfig;
import helpers.FirebaseUserHelper;
import models.User;
import pedroadmn.uberclone.com.R;

public class LoginActivity extends AppCompatActivity {

    private Button btEnter;
    private EditText etEmail;
    private EditText etPassword;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeComponents();

        auth = FirebaseConfig.getAuthFirebase();

        btEnter.setOnClickListener(v -> login());

        getSupportActionBar().setTitle("Access my account");
    }

    private void initializeComponents() {
        btEnter = findViewById(R.id.btEnter);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
    }

    private void login() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

            if (!email.isEmpty()) {
                if (!password.isEmpty()) {
                    auth.signInWithEmailAndPassword(
                            email, password
                    ).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                    "Successfully logged",
                                    Toast.LENGTH_SHORT).show();

                            FirebaseUserHelper.redirectLoggedUser(LoginActivity.this);

                        } else {
                            String exceptionMessage = "";
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException exception) {
                                exceptionMessage = "User is not registered";
                            } catch (FirebaseAuthInvalidCredentialsException exception) {
                                exceptionMessage = "Email or password is invalid";
                            } catch (Exception exception) {
                                exceptionMessage = "Error on login user";
                                exception.printStackTrace();
                            }

                            Toast.makeText(this, exceptionMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Fill the password!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(LoginActivity.this,
                        "Fill the E-mail!",
                        Toast.LENGTH_SHORT).show();
            }
    }

    private void goToPassengerScreen() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToDriverScreen() {
        Intent intent = new Intent(this, RequestsActivity.class);
        startActivity(intent);
        finish();
    }
}