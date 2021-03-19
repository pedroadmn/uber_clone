package helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

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
}
