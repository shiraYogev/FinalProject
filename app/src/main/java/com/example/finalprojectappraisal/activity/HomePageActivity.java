package com.example.finalprojectappraisal.activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectappraisal.R;
import com.example.finalprojectappraisal.activity.AllProjects.AllProjectsViewActivity;
import com.example.finalprojectappraisal.activity.myProjects.MyProjectsActivity;
import com.example.finalprojectappraisal.activity.newProject.client.ClientDetailsActivity;
import com.example.finalprojectappraisal.model.Project;
import com.example.finalprojectappraisal.utils.FilterPrefs;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomePageActivity extends AppCompatActivity {

    // ---- Tags for Logcat ----
    private static final String TAG_HOME   = "HomePage";
    private static final String TAG_NAV    = "Home→MyProjects";
    private static final String TAG_STATUS = "HomeStatus";
    private static final String TAG_RECENT = "HomeRecent";

    // ---- Views ----
    private MaterialCardView cardNewProject, cardMyProjects, cardAllProjectsViewOnly, cardSettings;
    private MaterialCardView cardStatTotal, cardStatActive, cardStatCompleted;
    private TextView userNameText, greetingText;
    private TextView statTotalValue, statActiveValue, statCompletedValue;
    private ImageView userAvatar;

    // ---- By-Status section views ----
    private MaterialCardView statusProjectsCard;
    private LinearLayout statusProjectsContainer, statusLoadingState, statusProjectsList;
    private TextView statusTitle;

    // ---- Recent projects (dynamic) ----
    private LinearLayout recentActivityContainer;
    private TextView recentActivityEmpty;

    // ---- Infra ----
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
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        initializeViews();
        setupClickListeners();
        setupAnimations();
        loadUserData();
        loadProjectsByStatus();
        loadRecentProjectsForHome(); // טעינת 4 הפרויקטים האחרונים
    }

    private void initializeViews() {
        // Main cards
        cardNewProject = findViewById(R.id.card_new_project);
        cardMyProjects = findViewById(R.id.card_my_projects);
        cardAllProjectsViewOnly = findViewById(R.id.card_all_projects_view_only);
        cardSettings = findViewById(R.id.card_settings);

        // Stat cards
        cardStatTotal = findViewById(R.id.card_stat_total);
        cardStatActive = findViewById(R.id.card_stat_active);
        cardStatCompleted = findViewById(R.id.card_stat_completed);

        // Texts
        userNameText = findViewById(R.id.user_name);
        greetingText = findViewById(R.id.greeting_text);

        // Stat texts
        statTotalValue = findViewById(R.id.stat_total_value);
        statActiveValue = findViewById(R.id.stat_active_value);
        statCompletedValue = findViewById(R.id.stat_completed_value);

        // Header icons
        userAvatar = findViewById(R.id.user_avatar);

        // By-Status section
        statusProjectsCard = findViewById(R.id.status_projects_card);
        statusProjectsContainer = findViewById(R.id.status_projects_container);
        statusLoadingState = findViewById(R.id.status_loading_state);
        statusProjectsList = findViewById(R.id.status_projects_list);
        statusTitle = findViewById(R.id.status_title);

        // Recent projects section
        recentActivityContainer = findViewById(R.id.recent_activity_container);
        recentActivityEmpty = findViewById(R.id.recent_activity_empty);
    }

    private void setupClickListeners() {
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

        // Stat cards click listeners - navigate to MyProjects with filter
        cardStatTotal.setOnClickListener(v -> {
            addRippleEffect(v);
            intent = new Intent(HomePageActivity.this, MyProjectsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        cardStatActive.setOnClickListener(v -> {
            addRippleEffect(v);
            FilterPrefs.saveSingleStatus(this, "בביצוע");
            intent = new Intent(HomePageActivity.this, MyProjectsActivity.class);
            intent.putExtra(MyProjectsActivity.EXTRA_PREFILTER_STATUS, "בביצוע");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        cardStatCompleted.setOnClickListener(v -> {
            addRippleEffect(v);
            FilterPrefs.saveSingleStatus(this, "הושלם");
            intent = new Intent(HomePageActivity.this, MyProjectsActivity.class);
            intent.putExtra(MyProjectsActivity.EXTRA_PREFILTER_STATUS, "הושלם");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        // אין notificationsButton יותר

        userAvatar.setOnClickListener(v -> {
            addRippleEffect(v);
            // אפשר בעתיד לפתוח פה מסך פרופיל/הגדרות משתמש
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
    }

    private void setupAnimations() {
        // Stat cards animation
        Animation statAnim1 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        statAnim1.setStartOffset(100);
        cardStatTotal.startAnimation(statAnim1);

        Animation statAnim2 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        statAnim2.setStartOffset(150);
        cardStatActive.startAnimation(statAnim2);

        Animation statAnim3 = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
        statAnim3.setStartOffset(200);
        cardStatCompleted.startAnimation(statAnim3);

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

        if (statusProjectsCard != null) {
            Animation statusAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_banking);
            statusAnim.setStartOffset(400);
            statusProjectsCard.startAnimation(statusAnim);
        }
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

    /**
     * טוען סטטיסטיקות ל-3 הכרטיסים:
     *  - סה"כ פרויקטים
     *  - בביצוע (לא הושלם)
     *  - הושלמו
     */
    private void loadProjectStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            setStatValues(0, 0, 0);
            updateWeeklyText(0);
            Log.d(TAG_HOME, "No user → stats=0");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("appraisers").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.w(TAG_HOME, "Appraiser doc not found for uid=" + userId);
                        setStatValues(0, 0, 0);
                        updateWeeklyText(0);
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<String> activeProjects =
                            (List<String>) doc.get("activeProjects");

                    int total = (activeProjects != null) ? activeProjects.size() : 0;
                    Log.d(TAG_HOME, "Total activeProjects for appraiser=" + total);

                    // נטען את הסטטוסים של הפרויקטים לחלוקה לכרטיסים
                    loadProjectStatusBreakdown(activeProjects, total);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG_HOME, "Error loading appraiser for stats", e);
                    setStatValues(0, 0, 0);
                    updateWeeklyText(-1);
                });
    }

    /** מעדכן את הכרטיסים עם הנתונים */
    private void setStatValues(int total, int active, int completed) {
        if (statTotalValue != null) statTotalValue.setText(String.valueOf(total));
        if (statActiveValue != null) statActiveValue.setText(String.valueOf(active));
        if (statCompletedValue != null) statCompletedValue.setText(String.valueOf(completed));
    }

    /** טוען את הסטטוסים ומחלק ל-3 קטגוריות */
    private void loadProjectStatusBreakdown(List<String> activeProjects, int total) {
        if (activeProjects == null || activeProjects.isEmpty()) {
            setStatValues(0, 0, 0);
            updateWeeklyText(0);
            return;
        }

        final int totalToCheck = activeProjects.size();
        final int[] processed = {0};
        final int[] completedCount = {0};
        final int[] activeCount = {0}; // לא הושלם

        for (String projectId : activeProjects) {
            if (projectId == null || projectId.trim().isEmpty()) {
                processed[0]++;
                if (processed[0] == totalToCheck) {
                    animateCounterBanking(statTotalValue, 0, total);
                    animateCounterBanking(statActiveValue, 0, activeCount[0]);
                    animateCounterBanking(statCompletedValue, 0, completedCount[0]);
                    updateWeeklyText(completedCount[0]);
                }
                continue;
            }

            db.collection("projects").document(projectId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        processed[0]++;

                        if (doc.exists()) {
                            String status = doc.getString("projectStatus");
                            if ("הושלם".equals(status)) {
                                completedCount[0]++;
                            } else {
                                activeCount[0]++;
                            }
                        }

                        if (processed[0] == totalToCheck) {
                            animateCounterBanking(statTotalValue, 0, total);
                            animateCounterBanking(statActiveValue, 0, activeCount[0]);
                            animateCounterBanking(statCompletedValue, 0, completedCount[0]);
                            updateWeeklyText(completedCount[0]);
                        }
                    })
                    .addOnFailureListener(e -> {
                        processed[0]++;
                        Log.e(TAG_HOME, "Error loading project status, id=" + projectId, e);
                        if (processed[0] == totalToCheck) {
                            animateCounterBanking(statTotalValue, 0, total);
                            animateCounterBanking(statActiveValue, 0, activeCount[0]);
                            animateCounterBanking(statCompletedValue, 0, completedCount[0]);
                            updateWeeklyText(completedCount[0]);
                        }
                    });
        }
    }

    /** לוג בלבד - הכרטיס השלישי מציג כבר את הספירה */
    private void updateWeeklyText(int completedCount) {
        if (completedCount < 0) {
            Log.w(TAG_HOME, "Weekly stats error");
        } else {
            Log.d(TAG_HOME, "Weekly completed projects: " + completedCount);
        }
    }

    private void animateCounterBanking(TextView textView, int start, int end) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(a -> textView.setText(String.valueOf((int) a.getAnimatedValue())));
        animator.start();
    }

    private void addRippleEffect(View view) {
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.button_scale);
        view.startAnimation(scaleAnimation);
        view.setAlpha(0.7f);
        if (handler != null) {
            handler.postDelayed(() -> view.setAlpha(1.0f), 150);
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            userNameText.setText("משתמש לא מחובר");
            Log.d(TAG_HOME, "No user → userNameText=משתמש לא מחובר");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("appraisers").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        userNameText.setText(fullName != null ? fullName : "שמאי");
                        Log.d(TAG_HOME, "Loaded appraiser name=" + fullName);
                    } else {
                        userNameText.setText("שמאי לא ידוע");
                        Log.d(TAG_HOME, "Appraiser doc not exists → default name");
                    }
                    setDynamicGreeting();
                    loadProjectStats();       // משתמש במסמך השמאי כדי להגיע לפרויקטים
                    loadRecentProjectsForHome();
                })
                .addOnFailureListener(e -> {
                    userNameText.setText("שגיאה בטעינת שם");
                    Log.e(TAG_HOME, "Error loading user data", e);
                    setDynamicGreeting();
                    loadProjectStats();
                    loadRecentProjectsForHome();
                });
    }

    // ========================= PROJECTS BY STATUS =========================

    /** Loads all projects for the current appraiser and shows one clickable row per status with a count badge. */
    private void loadProjectsByStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showStatusNoData("משתמש לא מחובר");
            return;
        }

        statusLoadingState.setVisibility(View.VISIBLE);
        statusProjectsList.setVisibility(View.GONE);

        db.collection("projects")
                .whereEqualTo("appraiserId", currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    statusLoadingState.setVisibility(View.GONE);
                    statusProjectsList.setVisibility(View.VISIBLE);
                    statusProjectsList.removeAllViews();

                    Log.d(TAG_STATUS, "Query returned " + query.size() + " projects for status bucketing");

                    if (query.isEmpty()) {
                        showStatusNoData("אין פרויקטים להצגה");
                        return;
                    }

                    // Order is defined in arrays.xml
                    String[] orderArr = getResources().getStringArray(R.array.project_statuses);
                    List<String> order = java.util.Arrays.asList(orderArr);

                    java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
                    for (String s : order) counts.put(s, 0);

                    for (DocumentSnapshot doc : query) {
                        String heb = safe(doc.getString("projectStatus")); // already Hebrew
                        if (counts.containsKey(heb)) {
                            counts.put(heb, counts.get(heb) + 1);
                        }
                    }

                    Log.d(TAG_STATUS, "Status counts: " + new Gson().toJson(counts));

                    int shown = 0;
                    for (String hebStatus : order) {
                        int c = counts.get(hebStatus);
                        if (c == 0) continue; // skip empty statuses
                        addStatusRow(hebStatus, c);
                        shown++;
                    }

                    if (shown == 0) showStatusNoData("אין פרויקטים להצגה");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG_STATUS, "Error loading projects by status", e);
                    statusLoadingState.setVisibility(View.GONE);
                    showStatusNoData("שגיאה בטעינת סטטוסים");
                });
    }

    /** Creates a large, tappable card for a status with a numeric badge. */
    private void addStatusRow(String hebrewStatus, int count) {
        MaterialCardView card = new MaterialCardView(this);
        card.setClickable(true);
        card.setFocusable(true);
        card.setUseCompatPadding(true);
        card.setCardElevation(dp(2));
        card.setStrokeColor(android.graphics.Color.TRANSPARENT);
        card.setOnClickListener(v -> openMyProjectsWithStatus(hebrewStatus));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(8);
        cardLp.bottomMargin = dp(8);
        card.setLayoutParams(cardLp);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(20), dp(18), dp(20), dp(18)); // generous touch target
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.addView(row);

        TextView title = new TextView(this);
        title.setText(hebrewStatus);
        title.setTextSize(16f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.text_primary_dark));
        row.addView(title);

        View spacer = new View(this);
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, 0, 1f);
        spacer.setLayoutParams(spLp);
        row.addView(spacer);

        TextView badge = new TextView(this);
        badge.setText(String.valueOf(count));
        badge.setTextSize(14f);
        badge.setTextColor(getColor(R.color.text_primary_dark));
        badge.setPadding(dp(12), dp(6), dp(12), dp(6));
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(getColor(R.color.glass_surface));
        g.setCornerRadius(dp(20));
        badge.setBackground(g);
        row.addView(badge);

        Log.d(TAG_STATUS, "Add status row: " + hebrewStatus + " (" + count + ")");
        statusProjectsList.addView(card);
    }

    private void openMyProjectsWithStatus(String hebrewStatus) {
        Log.d(TAG_NAV, "User tapped status: " + hebrewStatus);
        try {
            FilterPrefs.saveSingleStatus(this, hebrewStatus);
            com.example.finalprojectappraisal.activity.myProjects.filter.ProjectFilter dump =
                    FilterPrefs.load(this);
            Log.d(TAG_NAV, "Saved FilterPrefs: " + new Gson().toJson(dump));
        } catch (Throwable t) {
            Log.e(TAG_NAV, "Failed saving FilterPrefs", t);
        }

        Intent i = new Intent(this, MyProjectsActivity.class);
        i.putExtra(MyProjectsActivity.EXTRA_PREFILTER_STATUS, hebrewStatus);
        Log.d(TAG_NAV, "Starting MyProjectsActivity with prefilter_status=" + hebrewStatus);
        startActivity(i);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    private void showStatusNoData(String msg) {
        if (statusLoadingState != null) statusLoadingState.setVisibility(View.GONE);

        if (statusProjectsList == null) return;
        statusProjectsList.setVisibility(View.VISIBLE);
        statusProjectsList.removeAllViews();

        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(getColor(R.color.text_secondary_dark));
        tv.setTextSize(14);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, dp(32), 0, dp(32));

        statusProjectsList.addView(tv);
    }

    // ========================= RECENT PROJECTS (LAST 4) =========================

    private void loadRecentProjectsForHome() {
        if (recentActivityContainer == null || recentActivityEmpty == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showRecentActivityEmpty("משתמש לא מחובר");
            Log.w(TAG_RECENT, "No user → cannot load recent projects");
            return;
        }

        String userId = currentUser.getUid();
        recentActivityEmpty.setText("טוען פרויקטים אחרונים...");
        recentActivityEmpty.setVisibility(View.VISIBLE);
        recentActivityContainer.removeAllViews();

        db.collection("appraisers").document(userId)
                .get()
                .addOnSuccessListener(appraiserDoc -> {
                    if (!appraiserDoc.exists()) {
                        Log.w(TAG_RECENT, "Appraiser doc not found for uid=" + userId);
                        showRecentActivityEmpty("לא נמצאו פרויקטים לשמאי");
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<String> activeProjects = (List<String>) appraiserDoc.get("activeProjects");
                    Log.d(TAG_RECENT, "activeProjects=" + activeProjects);

                    if (activeProjects == null || activeProjects.isEmpty()) {
                        showRecentActivityEmpty("אין פרויקטים פעילים להצגה");
                        return;
                    }

                    List<String> reversed = new ArrayList<>(activeProjects);
                    Collections.reverse(reversed); // נניח שהאחרונים בסוף הרשימה
                    List<String> limitedIds = reversed.subList(0, Math.min(4, reversed.size()));
                    final List<String> orderedIds = new ArrayList<>(limitedIds);

                    Log.d(TAG_RECENT, "Will load last ids=" + orderedIds);

                    db.collection("projects")
                            .whereIn("projectId", orderedIds)
                            .get()
                            .addOnSuccessListener(query -> {
                                if (query.isEmpty()) {
                                    Log.w(TAG_RECENT, "Projects query by projectId returned empty");
                                    showRecentActivityEmpty("לא נמצאו פרויקטים תואמים");
                                    return;
                                }

                                List<Project> projects = new ArrayList<>();
                                for (DocumentSnapshot doc : query) {
                                    Project p = doc.toObject(Project.class);
                                    if (p != null) projects.add(p);
                                }

                                // סדר לפי סדר ה־ID ברשימת orderedIds
                                Collections.sort(projects, (p1, p2) -> {
                                    String id1 = safe(p1.getProjectId());
                                    String id2 = safe(p2.getProjectId());
                                    int idx1 = orderedIds.indexOf(id1);
                                    int idx2 = orderedIds.indexOf(id2);
                                    if (idx1 == -1) idx1 = orderedIds.size();
                                    if (idx2 == -1) idx2 = orderedIds.size();
                                    return Integer.compare(idx1, idx2);
                                });

                                recentActivityContainer.removeAllViews();

                                for (Project p : projects) {
                                    String projectId = p.getProjectId();
                                    String status = p.getProjectStatus();
                                    String address = p.getFullAddress();
                                    if (address == null || address.trim().isEmpty()) {
                                        address = "פרויקט ללא כתובת";
                                    }
                                    addRecentProjectRow(projectId, address, status);
                                }

                                if (recentActivityContainer.getChildCount() == 0) {
                                    showRecentActivityEmpty("אין פרויקטים להצגה");
                                } else {
                                    recentActivityEmpty.setVisibility(View.GONE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG_RECENT, "Error loading recent projects (projects query)", e);
                                showRecentActivityEmpty("שגיאה בטעינת פרויקטים");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG_RECENT, "Error loading appraiser doc for recent projects", e);
                    showRecentActivityEmpty("שגיאה בטעינת פרויקטים");
                });
    }

    private void addRecentProjectRow(String projectId, String address, String status) {
        if (recentActivityContainer == null) return;

        // Divider בין רשומות
        if (recentActivityContainer.getChildCount() > 0) {
            View divider = new View(this);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            divider.setLayoutParams(dLp);
            divider.setBackgroundColor(getColor(R.color.fp_outline));
            recentActivityContainer.addView(divider);
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setPadding(dp(8), dp(12), dp(8), dp(12));

        TextView title = new TextView(this);
        title.setText(address);
        title.setTextSize(15f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextColor(getColor(R.color.text_primary_dark));
        row.addView(title);

        TextView subtitle = new TextView(this);
        String statusLabel = (status == null || status.trim().isEmpty())
                ? "סטטוס לא ידוע"
                : "סטטוס: " + status;
        subtitle.setText(statusLabel);
        subtitle.setTextSize(13f);
        subtitle.setTextColor(getColor(R.color.text_secondary_dark));
        subtitle.setPadding(0, dp(4), 0, 0);
        row.addView(subtitle);

        final String query = address; // search query for MyProjects
        row.setOnClickListener(v -> {
            Log.d(TAG_NAV, "User tapped recent project from home; pid=" + projectId
                    + ", query=" + query);
            Intent i = new Intent(this, MyProjectsActivity.class);
            i.putExtra(MyProjectsActivity.EXTRA_PREFILTER_QUERY, query);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        recentActivityContainer.addView(row);
    }

    private void showRecentActivityEmpty(String msg) {
        if (recentActivityEmpty == null) return;
        recentActivityEmpty.setText(msg);
        recentActivityEmpty.setVisibility(View.VISIBLE);
    }

    // ---- Lifecycle ----
    @Override
    protected void onResume() {
        super.onResume();
        loadProjectStats();
        setDynamicGreeting();
        loadProjectsByStatus(); // keep status card fresh
        loadRecentProjectsForHome(); // לרענן גם את 4 הפרויקטים האחרונים
        Log.d(TAG_HOME, "onResume → refreshed stats, status, recent");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacksAndMessages(null);
    }

    // ---- Utils ----
    private String safe(String s) { return s == null ? "" : s; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    // מצפן – כמו שהיה אצלך
    private void openCompassAppOrStore() {
        String specificCompassPackage = "app.melon.icompass";
        Intent intent = getPackageManager().getLaunchIntentForPackage(specificCompassPackage);

        if (intent != null) {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
            return;
        }

        android.widget.Toast.makeText(this, "האפליקציה הספציפית לא נמצאה, מפנה לחנות.", android.widget.Toast.LENGTH_LONG).show();

        Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=" + specificCompassPackage));

        if (playStoreIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(playStoreIntent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=" + specificCompassPackage));
            startActivity(browserIntent);
        }
    }
}
