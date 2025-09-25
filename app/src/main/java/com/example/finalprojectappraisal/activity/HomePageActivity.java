package com.example.finalprojectappraisal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.myProjects.MyProjectsActivity;
import com.example.finalprojectappraisal.activity.newProject.client.ClientDetailsActivity;
// removed: UploadImagesActivity import
import com.example.finalprojectappraisal.activity.AllProjects.AllProjectsViewActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomePageActivity extends AppCompatActivity {

    // UI Components
    private LinearLayout newProjectButton, myProjectsButton, allProjectsViewOnlyButton, settingsButton;
    private CardView cardNewProject, cardMyProjects, cardAllProjectsViewOnly, cardSettings;
    private TextView userNameText, totalProjectsDisplay, greetingText;
    private ImageView notificationsButton, userAvatar;

    private Intent intent;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Initialize handler
        handler = new Handler(Looper.getMainLooper());

        // Hide status bar for immersive experience
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        initializeViews();
        setupClickListeners();
        setupAnimations();

        userNameText = findViewById(R.id.user_name);
        loadUserData();
    }

    private void initializeViews() {
        // Action buttons
        newProjectButton = findViewById(R.id.button_new_project);
        myProjectsButton = findViewById(R.id.button_my_projects);
        allProjectsViewOnlyButton = findViewById(R.id.button_all_projects_view_only); // NEW
        settingsButton = findViewById(R.id.button_settings);

        // Cards
        cardNewProject = findViewById(R.id.card_new_project);
        cardMyProjects = findViewById(R.id.card_my_projects);
        cardAllProjectsViewOnly = findViewById(R.id.card_all_projects_view_only); // NEW
        cardSettings = findViewById(R.id.card_settings);

        // Text views
        userNameText = findViewById(R.id.user_name);
        totalProjectsDisplay = findViewById(R.id.total_projects_display);
        greetingText = findViewById(R.id.greeting_text);

        // Other UI elements
        notificationsButton = findViewById(R.id.btn_notifications);
        userAvatar = findViewById(R.id.user_avatar);
    }

    private void setupClickListeners() {
        // New Project
        View.OnClickListener newProjectListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, ClientDetailsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        newProjectButton.setOnClickListener(newProjectListener);
        cardNewProject.setOnClickListener(newProjectListener);

        // My Projects
        View.OnClickListener myProjectsListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, MyProjectsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        myProjectsButton.setOnClickListener(myProjectsListener);
        cardMyProjects.setOnClickListener(myProjectsListener);

        // All Projects (View Only) - NEW
        View.OnClickListener viewAllListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, AllProjectsViewActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        allProjectsViewOnlyButton.setOnClickListener(viewAllListener);
        cardAllProjectsViewOnly.setOnClickListener(viewAllListener);

        // Settings
        View.OnClickListener settingsListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        settingsButton.setOnClickListener(settingsListener);
        cardSettings.setOnClickListener(settingsListener);

        // Notifications
        notificationsButton.setOnClickListener(v -> {
            addRippleEffect(v);
            // TODO: NotificationsActivity
        });

        // Profile
        userAvatar.setOnClickListener(v -> {
            addRippleEffect(v);
            // TODO: ProfileActivity
        });
    }

    private void setupAnimations() {
        // Staggered animation for cards (like banking apps)
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);

        // Animate main card first
        CardView mainCard = findViewById(R.id.main_card);
        Animation mainCardAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        mainCardAnimation.setStartOffset(100);
        mainCard.startAnimation(mainCardAnimation);

        // Then animate action cards with staggered delay
        Animation cardAnimation1 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation1.setStartOffset(200);
        cardNewProject.startAnimation(cardAnimation1);

        Animation cardAnimation2 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation2.setStartOffset(250);
        cardMyProjects.startAnimation(cardAnimation2);

        // Replaced: cardUploadImages -> cardAllProjectsViewOnly
        Animation cardAnimation3 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation3.setStartOffset(300);
        cardAllProjectsViewOnly.startAnimation(cardAnimation3);

        Animation cardAnimation4 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation4.setStartOffset(350);
        cardSettings.startAnimation(cardAnimation4);

        // Recent projects card
        CardView recentCard = findViewById(R.id.recent_projects_card);
        Animation recentAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        recentAnimation.setStartOffset(400);
        recentCard.startAnimation(recentAnimation);
    }

    private void setDynamicGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 6 && hourOfDay < 12) {
            greeting = "בוקר טוב";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "צהריים טובים";
        } else if (hourOfDay >= 18 && hourOfDay < 22) {
            greeting = "ערב טוב";
        } else {
            greeting = "לילה טוב";
        }
        greetingText.setText(greeting);
    }

    private void loadProjectStats() {
        // TODO: Replace with real DB count
        int totalProjects = getTotalProjectsCount();
        animateCounterBanking(totalProjectsDisplay, 0, totalProjects);
    }

    private void animateCounterBanking(TextView textView, int start, int end) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> textView.setText(String.valueOf(animation.getAnimatedValue())));
        animator.start();
    }

    private int getTotalProjectsCount() {
        // TODO: Real implementation
        return 12;
    }

    private void addRippleEffect(View view) {
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale);
        view.startAnimation(scaleAnimation);
        view.setAlpha(0.7f);
        handler.postDelayed(() -> view.setAlpha(1.0f), 150);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjectStats();
        setDynamicGreeting();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }

    // ---- Data loading helpers ----

    private void loadUserProfile() {
        // TODO
    }

    private void loadRecentProjects() {
        // TODO
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // TODO
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            userNameText.setText("משתמש לא מחובר");
            return;
        }

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("appraisers")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        userNameText.setText(fullName != null ? fullName : "שמאי");
                    } else {
                        userNameText.setText("שמאי לא ידוע");
                    }
                })
                .addOnFailureListener(e -> {
                    userNameText.setText("שגיאה בטעינת שם");
                    Log.e("HomePageActivity", "Error loading user data", e);
                });

        setDynamicGreeting();
        loadProjectStats();
    }
}
