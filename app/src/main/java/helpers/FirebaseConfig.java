package helpers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseConfig {

    private static DatabaseReference firebaseRef;
    private static FirebaseAuth authRef;
    private static StorageReference storageRef;

    public static DatabaseReference getFirebase() {
        if (firebaseRef == null) {
            firebaseRef = FirebaseDatabase.getInstance().getReference();
        }
        return firebaseRef;
    }

    public static FirebaseAuth getAuthFirebase() {
        if (authRef == null) {
            authRef = FirebaseAuth.getInstance();
        }
        return authRef;
    }

    public static StorageReference getFirebaseStorage() {
        if (storageRef == null) {
            storageRef = FirebaseStorage.getInstance().getReference();
        }
        return storageRef;
    }
}
