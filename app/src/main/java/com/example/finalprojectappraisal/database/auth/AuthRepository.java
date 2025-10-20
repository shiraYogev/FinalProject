package com.example.finalprojectappraisal.database.auth;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Facade לשכבת האימות (Firebase Auth) – כדי שה-UI לא ייגע ב-SDK ישירות.
 * מספק:
 *  - getCurrentUserId()
 *  - isLoggedIn()
 *  - signOut()
 *  - LiveData ל-UID ולמצב התחברות (מתעדכנים בזמן אמת)
 */
public class AuthRepository {

    private static AuthRepository instance;
    public static synchronized AuthRepository getInstance() {
        if (instance == null) instance = new AuthRepository();
        return instance;
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Boolean> isLoggedInLive = new MutableLiveData<>();
    private final MutableLiveData<String> userIdLive = new MutableLiveData<>();

    private final FirebaseAuth.AuthStateListener stateListener = firebaseAuth -> {
        FirebaseUser u = firebaseAuth.getCurrentUser();
        isLoggedInLive.setValue(u != null);
        userIdLive.setValue(u != null ? u.getUid() : null);
    };

    private AuthRepository() {
        auth.addAuthStateListener(stateListener);
        FirebaseUser u = auth.getCurrentUser();
        isLoggedInLive.setValue(u != null);
        userIdLive.setValue(u != null ? u.getUid() : null);
    }

    /** UID נוכחי או null אם אין משתמש מחובר */
    @Nullable
    public String getCurrentUserId() {
        FirebaseUser u = auth.getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    /** האם יש משתמש מחובר כרגע */
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    /** יציאה מהחשבון */
    public void signOut() {
        auth.signOut();
        // ה-AuthStateListener יעדכן את ה-LiveData אוטומטית
    }

    /** LiveData ל-UID הנוכחי (מתעדכן בהתחברות/יציאה) */
    public LiveData<String> getUserIdLive() {
        return userIdLive;
    }

    /** LiveData למצב התחברות */
    public LiveData<Boolean> getIsLoggedInLive() {
        return isLoggedInLive;
    }
}
