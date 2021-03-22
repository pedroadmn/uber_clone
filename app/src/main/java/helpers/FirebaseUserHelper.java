package helpers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import activities.PassengerActivity;
import activities.RequestsActivity;
import models.User;

import static helpers.FirebaseConfig.getAuthFirebase;

public class FirebaseUserHelper {

    public static String getUserId() {
        FirebaseAuth auth = getAuthFirebase();
        return auth.getCurrentUser().getUid();
    }

    public static FirebaseUser getCurrentUser() {
        FirebaseAuth auth = getAuthFirebase();
        return auth.getCurrentUser();
    }

    public static User getLoggedUserInfo() {
        FirebaseUser firebaseUser = getCurrentUser();
        User user = new User();
        user.setUserId(firebaseUser.getUid());
        user.setEmail(firebaseUser.getEmail());
        user.setName(firebaseUser.getDisplayName());
        return user;
    }

    public static boolean updateUsername(String name) {
        try {
            FirebaseUser user = getCurrentUser();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()) {
                        Log.d("Profile", "Error on update user name: ");
                    }
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void redirectLoggedUser(Activity activity) {
        FirebaseUser firebaseUser = getCurrentUser();

        if (firebaseUser != null) {
            DatabaseReference usersRef = FirebaseConfig.getFirebase().child("users").child(getLoggedUserId());

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);

                    String userType = user.getType();

                    if (userType.equals("D")) {
                        Intent intent = new Intent(activity, RequestsActivity.class);
                        activity.startActivity(intent);
                    } else {
                        Intent intent = new Intent(activity, PassengerActivity.class);
                        activity.startActivity(intent);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    public static String getLoggedUserId() {
        return getCurrentUser().getUid();
    }
}
