package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signUpButton;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up); // ודאי ששם הקובץ שלך הוא activity_sign_up.xml

        firebaseAuth = FirebaseAuth.getInstance();

        // קישור לרכיבים מה-XML
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);

        // הפעלת הכפתור רק כאשר כל השדות מולאו
        signUpButton.setEnabled(true); // אפשר להוסיף TextWatcher אם רוצים בדיקה תוך כדי הקלדה

        signUpButton.setOnClickListener(v -> attemptSignUp());
    }

    private void attemptSignUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (!isValidEmail(email)) {
            emailEditText.setError("כתובת אימייל לא חוקית");
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("הסיסמה חייבת להכיל לפחות 6 תווים");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("הסיסמאות אינן תואמות");
            return;
        }

        // יצירת משתמש
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            Intent intent = new Intent(SignUpActivity.this, AppraiserSetupActivity.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(SignUpActivity.this, HomePageActivity.class);
                            startActivity(intent);
                        }

                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "הרשמה נכשלה: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
