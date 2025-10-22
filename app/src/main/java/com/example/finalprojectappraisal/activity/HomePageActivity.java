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
import android.widget.ProgressBar;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
// ❌ הסירי את הייבוא הישן של CardView
// import androidx.cardview.widget.CardView;
// ✅ ייבוא חדש:
import com.google.android.material.card.MaterialCardView;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.myProjects.MyProjectsActivity;
import com.example.finalprojectappraisal.activity.newProject.client.ClientDetailsActivity;
import com.example.finalprojectappraisal.activity.AllProjects.AllProjectsViewActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomePageActivity extends AppCompatActivity {

    // ❌ לא צריך יותר כפתורי inner:
    // private LinearLayout newProjectButton, myProjectsButton, allProjectsViewOnlyButton, settingsButton;

    private LinearLayout recentProjectsContainer, loadingState, recentProjectsList;

    // ✅ כל הכרטיסים הם MaterialCardView עכשיו, כולל הראשי וה-"Recent"
    private MaterialCardView cardNewProject, cardMyProjects, cardAllProjectsViewOnly, cardSettings, mainCard, recentProjectsCard;

    private TextView userNameText, totalProjectsDisplay, greetingText, recentTitle, completedThisWeek;
    private ImageView notificationsButton, userAvatar;
    private ProgressBar progressBar;

    private Intent intent;
    private Handler handler;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        db = FirebaseFirestore.getInstance();
        handler = new Handler(Looper.getMainLooper());

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        initializeViews();
        setupClickListeners();
        setupAnimations();
        loadUserData();
        loadRecentProjects();
    }

    private void initializeViews() {
        // ❌ אין יותר button_* ב-XML
        // newProjectButton = findViewById(R.id.button_new_project); ...

        // ✅ תפסי רק את הכרטיסים עצמם (MaterialCardView)
        cardNewProject = findViewById(R.id.card_new_project);
        cardMyProjects = findViewById(R.id.card_my_projects);
        cardAllProjectsViewOnly = findViewById(R.id.card_all_projects_view_only);
        cardSettings = findViewById(R.id.card_settings);
        mainCard = findViewById(R.id.main_card);
        recentProjectsCard = findViewById(R.id.recent_projects_card);

        userNameText = findViewById(R.id.user_name);
        totalProjectsDisplay = findViewById(R.id.total_projects_display);
        greetingText = findViewById(R.id.greeting_text);
        recentTitle = findViewById(R.id.recent_title);
        completedThisWeek = findViewById(R.id.completed_this_week);

        recentProjectsContainer = findViewById(R.id.recent_projects_container);
        loadingState = findViewById(R.id.loading_state);
        recentProjectsList = findViewById(R.id.recent_projects_list);

        notificationsButton = findViewById(R.id.btn_notifications);
        userAvatar = findViewById(R.id.user_avatar);
    }

    private void setupClickListeners() {
        // ✅ מאזינים ישירות לכרטיסים
        View.OnClickListener newProjectListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, ClientDetailsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        cardNewProject.setOnClickListener(newProjectListener);

        View.OnClickListener myProjectsListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, MyProjectsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        cardMyProjects.setOnClickListener(myProjectsListener);

        View.OnClickListener viewAllListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, AllProjectsViewActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        cardAllProjectsViewOnly.setOnClickListener(viewAllListener);

        View.OnClickListener settingsListener = v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        };
        cardSettings.setOnClickListener(settingsListener);

        notificationsButton.setOnClickListener(v -> {
            addRippleEffect(v);
            // TODO: NotificationsActivity
        });

        userAvatar.setOnClickListener(v -> {
            addRippleEffect(v);
            // מומלץ להשתמש באנימציה עקבית של Slide-in:
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
    }

    private void setupAnimations() {
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);

        Animation mainCardAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        mainCardAnimation.setStartOffset(100);
        mainCard.startAnimation(mainCardAnimation);

        Animation cardAnimation1 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation1.setStartOffset(200);
        cardNewProject.startAnimation(cardAnimation1);

        Animation cardAnimation2 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation2.setStartOffset(250);
        cardMyProjects.startAnimation(cardAnimation2);

        Animation cardAnimation3 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation3.setStartOffset(300);
        cardAllProjectsViewOnly.startAnimation(cardAnimation3);

        Animation cardAnimation4 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        cardAnimation4.setStartOffset(350);
        cardSettings.startAnimation(cardAnimation4);

        Animation recentAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        recentAnimation.setStartOffset(400);
        recentProjectsCard.startAnimation(recentAnimation);
    }

    private void setDynamicGreeting() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 6 && hourOfDay < 12) greeting = "בוקר טוב";
        else if (hourOfDay >= 12 && hourOfDay < 18) greeting = "צהריים טובים";
        else if (hourOfDay >= 18 && hourOfDay < 22) greeting = "ערב טוב";
        else greeting = "לילה טוב";

        greetingText.setText(greeting);
    }

    private void loadProjectStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            totalProjectsDisplay.setText("0");
            completedThisWeek.setText("אין נתונים");
            return;
        }

        db.collection("projects")
                .whereEqualTo("appraiserId", currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    int totalProjects = query.size();
                    animateCounterBanking(totalProjectsDisplay, 0, totalProjects);
                })
                .addOnFailureListener(e -> {
                    Log.e("HomePageActivity", "Error loading project count", e);
                    totalProjectsDisplay.setText("0");
                });

        loadWeeklyStats();
    }

    private void loadWeeklyStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        java.util.Date weekStart = calendar.getTime();

        db.collection("projects")
                .whereEqualTo("appraiserId", currentUser.getUid())
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("completedDate", weekStart)
                .get()
                .addOnSuccessListener(query -> {
                    int completedCount = query.size();
                    completedThisWeek.setText(completedCount + " הושלמו השבוע");
                })
                .addOnFailureListener(e -> {
                    Log.e("HomePageActivity", "Error loading weekly stats", e);
                    completedThisWeek.setText("נתונים לא זמינים");
                });
    }

    private void animateCounterBanking(TextView textView, int start, int end) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(a -> textView.setText(String.valueOf((int) a.getAnimatedValue())));
        animator.start();
    }

    private void addRippleEffect(View view) {
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale);
        view.startAnimation(scaleAnimation);
        view.setAlpha(0.7f);
        handler.postDelayed(() -> view.setAlpha(1.0f), 150);
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            userNameText.setText("משתמש לא מחובר");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("appraisers").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        userNameText.setText(fullName != null ? fullName : "שמאי");
                    } else {
                        userNameText.setText("שמאי לא ידוע");
                    }
                    setDynamicGreeting();
                    loadProjectStats();
                })
                .addOnFailureListener(e -> {
                    userNameText.setText("שגיאה בטעינת שם");
                    Log.e("HomePageActivity", "Error loading user data", e);
                    setDynamicGreeting();
                    loadProjectStats();
                });
    }

    private void loadRecentProjects() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showNoProjectsState();
            return;
        }

        loadingState.setVisibility(View.VISIBLE);
        recentProjectsList.setVisibility(View.GONE);

        db.collection("projects")
                .whereEqualTo("appraiserId", currentUser.getUid())
                .orderBy("createdDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(query -> {
                    loadingState.setVisibility(View.GONE);

                    if (query.isEmpty()) {
                        showNoProjectsState();
                        return;
                    }

                    recentProjectsList.setVisibility(View.VISIBLE);
                    recentProjectsList.removeAllViews();

                    query.forEach(this::addRecentProjectItem);
                })
                .addOnFailureListener(e -> {
                    Log.e("HomePageActivity", "Error loading recent projects", e);
                    loadingState.setVisibility(View.GONE);
                    showErrorState();
                });
    }

    private void addRecentProjectItem(com.google.firebase.firestore.QueryDocumentSnapshot document) {
        String projectName = document.getString("full_address");
        String status = document.getString("status");
        String clientName = document.getString("fullName");

        if (projectName == null) projectName = "כתובת לא זמינה";
        if (status == null) status = "לא ידוע";
        if (clientName == null) clientName = "לקוח לא ידוע";

        View projectItem = getLayoutInflater().inflate(R.layout.item_recent_project, recentProjectsList, false);

        TextView projectNameText = projectItem.findViewById(R.id.project_name);
        TextView projectClientText = projectItem.findViewById(R.id.project_client);
        TextView projectStatusText = projectItem.findViewById(R.id.project_status);

        projectNameText.setText(projectName);
        projectClientText.setText("לקוח: " + clientName);
        projectStatusText.setText(getStatusText(status));

        projectItem.setOnClickListener(v -> {
            Intent projectIntent = new Intent(this, MyProjectsActivity.class);
            projectIntent.putExtra("projectId", document.getId());
            startActivity(projectIntent);
        });

        recentProjectsList.addView(projectItem);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "draft": return "טיוטה";
            case "in_progress": return "בעבודה";
            case "completed": return "הושלם";
            case "submitted": return "נשלח";
            default: return "לא ידוע";
        }
    }

    private void showNoProjectsState() {
        loadingState.setVisibility(View.GONE);
        recentProjectsList.setVisibility(View.VISIBLE);
        recentProjectsList.removeAllViews();

        TextView noProjectsText = new TextView(this);
        noProjectsText.setText("אין פרויקטים אחרונים");
        noProjectsText.setTextColor(getColor(R.color.text_secondary_dark));
        noProjectsText.setTextSize(14);
        noProjectsText.setGravity(android.view.Gravity.CENTER);
        noProjectsText.setPadding(0, 32, 0, 32);

        recentProjectsList.addView(noProjectsText);
    }

    private void showErrorState() {
        loadingState.setVisibility(View.GONE);
        recentProjectsList.setVisibility(View.VISIBLE);
        recentProjectsList.removeAllViews();

        TextView errorText = new TextView(this);
        errorText.setText("שגיאה בטעינת הפרויקטים");
        errorText.setTextColor(getColor(R.color.error_color));
        errorText.setTextSize(14);
        errorText.setGravity(android.view.Gravity.CENTER);
        errorText.setPadding(0, 32, 0, 32);

        recentProjectsList.addView(errorText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjectStats();
        setDynamicGreeting();
        loadRecentProjects();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }
}
