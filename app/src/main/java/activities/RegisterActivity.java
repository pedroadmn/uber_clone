package activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import helpers.FirebaseConfig;
import helpers.FirebaseUserHelper;
import models.User;
import pedroadmn.uberclone.com.R;

public class RegisterActivity extends AppCompatActivity {

    private Button btRegister;
    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private Switch switchAccess;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseConfig.getAuthFirebase();

        initializeComponents();

        btRegister.setOnClickListener(v -> register());

        getSupportActionBar().setTitle("Register an account");
    }

    private void initializeComponents() {
        btRegister = findViewById(R.id.btRegister);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        switchAccess = findViewById(R.id.switchAccess);
    }

    private void register() {
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();

        if (!name.isEmpty()) {
            if (!email.isEmpty()) {
                if (!password.isEmpty()) {

                    User user = new User();
                    user.setName(name);
                    user.setEmail(email);
                    user.setPassword(password);
                    user.setType(getUserType());

                    auth.createUserWithEmailAndPassword(
                            user.getEmail(), user.getPassword()
                    ).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            try {
                                String userId = task.getResult().getUser().getUid();
                                user.setUserId(userId);

                                user.save();

                                FirebaseUserHelper.updateUsername(user.getName());

                                if (getUserType().equals("D")) {
                                    Toast.makeText(RegisterActivity.this,
                                            "Driver successfully registered!",
                                            Toast.LENGTH_SHORT).show();
                                    goToDriverScreen();
                                } else {
                                    Toast.makeText(RegisterActivity.this,
                                            "Driver successfully registered!",
                                            Toast.LENGTH_SHORT).show();
                                    goToPassengerScreen();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            String erroExcecao = "";
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                erroExcecao = "Type a stronger password";
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                erroExcecao = "Please, type a valid e-mail";
                            } catch (FirebaseAuthUserCollisionException e) {
                                erroExcecao = "This account is already registered";
                            } catch (Exception e) {
                                erroExcecao = "Error on register an account: " + e.getMessage();
                                e.printStackTrace();
                            }

                            Toast.makeText(RegisterActivity.this, erroExcecao,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "Fill the password!",
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(RegisterActivity.this,
                        "Fill the E-mail!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(RegisterActivity.this,
                    "Fill the name!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void goToPassengerScreen() {
        Intent intent = new Intent(this, PassengerActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToDriverScreen() {
        Intent intent = new Intent(this, RequestsActivity.class);
        startActivity(intent);
        finish();
    }

    private String getUserType() {
        return switchAccess.isChecked() ? "D" : "P";
    }
}