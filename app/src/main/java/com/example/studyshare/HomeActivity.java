package com.example.studyshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * מסך הבית (HomeActivity) - זהו המסך הראשי של האפליקציה לאחר ההתחברות.
 * המסך מציג עדכוני מערכת או פיד של סיכומים אחרונים, ומאפשר ניווט לכל חלקי האפליקציה.
 */
public class HomeActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש
    private View btnGoToUpload, btnGoToSearch, btnGoToProfile, btnLogout, cvHomeProfile;
    private ImageButton btnLeaderboard;
    private ImageView btnNavAdmin, ivHomeProfileImage;
    private TextView tvHomeTitle, tvHomeSubtitle;
    private RecyclerView rvHomeContent;
    private MaterialButtonToggleGroup toggleHome;
    
    // מתאמים לרשימות
    private SummaryAdapter summaryAdapter;
    private AppUpdateAdapter updateAdapter;
    
    // רשימות נתונים
    private List<Summary> feedList = new ArrayList<>();
    private List<DataManager.AppUpdate> updatesList = new ArrayList<>();
    
    private boolean showingUpdates = true; // משתנה למעקב אחר התצוגה הנוכחית (עדכונים או סיכומים)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initUI(); // אתחול רכיבי התצוגה
        setupRecyclerView(); // הגדרת רשימת התוכן
        setupNavigation(); // הגדרת כפתורי הניווט
        setupToggle(); // הגדרת כפתור ההחלפה בין עדכונים לסיכומים
        
        // בחירת מצב התחלתי - הצגת עדכונים
        toggleHome.check(R.id.btnViewUpdates);
        showUpdatesView();
    }

    /**
     * מקשר בין המשתנים בקוד לרכיבים הגרפיים בקובץ ה-XML.
     */
    private void initUI() {
        btnGoToUpload = findViewById(R.id.btnGoToUpload);
        btnGoToSearch = findViewById(R.id.btnGoToSearch);
        btnGoToProfile = findViewById(R.id.btnGoToProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnNavAdmin = findViewById(R.id.btnNavAdmin);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        cvHomeProfile = findViewById(R.id.cvHomeProfile);
        ivHomeProfileImage = findViewById(R.id.ivHomeProfileImage);
        rvHomeContent = findViewById(R.id.rvHomeContent);
        toggleHome = findViewById(R.id.toggleHome);
        tvHomeTitle = findViewById(R.id.tvHomeTitle);
        tvHomeSubtitle = findViewById(R.id.tvHomeSubtitle);
    }

    /**
     * מגדיר את ה-RecyclerView ואת המתאם עבור הסיכומים.
     */
    private void setupRecyclerView() {
        rvHomeContent.setLayoutManager(new LinearLayoutManager(this));
        summaryAdapter = new SummaryAdapter(feedList);
    }

    /**
     * מגדיר מאזין לכפתור ההחלפה (Toggle) כדי לעבור בין תצוגת עדכונים לתצוגת סיכומים.
     */
    private void setupToggle() {
        toggleHome.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnViewUpdates) {
                    showUpdatesView();
                } else if (checkedId == R.id.btnViewSummaries) {
                    showSummariesView();
                }
            }
        });
    }

    /**
     * מעביר את התצוגה להצגת עדכוני אפליקציה.
     */
    private void showUpdatesView() {
        showingUpdates = true;
        tvHomeTitle.setText("Updates");
        tvHomeSubtitle.setText("What's new in StudyShare");
        
        DataManager.getInstance().getCurrentUser(user -> {
            // בדיקה אם המשתמש הוא מנהל או סופר אדמין לצורך הרשאות בתצוגת העדכונים
            boolean isAdmin = user != null && (user.isAdmin() || DataManager.getInstance().isSuperAdmin());
            updateAdapter = new AppUpdateAdapter(updatesList, isAdmin);
            rvHomeContent.setAdapter(updateAdapter);
            loadUpdates();
        });
    }

    /**
     * מעביר את התצוגה להצגת הסיכומים האחרונים שהועלו.
     */
    private void showSummariesView() {
        showingUpdates = false;
        tvHomeTitle.setText("Latest");
        tvHomeSubtitle.setText("Recent study materials");
        
        rvHomeContent.setAdapter(summaryAdapter);
        loadFeed();
    }

    /**
     * מגדיר את פעולות הלחיצה עבור כל כפתורי הניווט במסך.
     */
    private void setupNavigation() {
        btnGoToUpload.setOnClickListener(v -> startActivity(new Intent(this, UploadActivity.class)));
        btnGoToSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        btnGoToProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        cvHomeProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        btnLeaderboard.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));

        // התנתקות מהחשבון וחזרה למסך הלוגין
        btnLogout.setOnClickListener(v -> {
            DataManager.getInstance().logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /**
     * בודק אם למשתמש יש הרשאות מנהל ומציג את כפתור הניהול בהתאם.
     */
    private void checkAdminAccess() {
        DataManager.getInstance().getCurrentUser(user -> {
            if (user != null) {
                // בדיקת הרשאת אדמין או סופר אדמין
                if (user.isAdmin() || DataManager.getInstance().isSuperAdmin()) {
                    btnNavAdmin.setVisibility(View.VISIBLE);
                    btnNavAdmin.setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));
                } else {
                    btnNavAdmin.setVisibility(View.GONE);
                }
                updateProfileImageUI(user);
            }
        });
    }

    /**
     * בודק אם היום חל יום ההולדת של המשתמש ומציג הודעת ברכה.
     */
    private void checkBirthday() {
        DataManager.getInstance().getCurrentUser(user -> {
            if (user != null && user.getDob() != null) {
                try {
                    String[] parts = user.getDob().split("/");
                    if (parts.length >= 2) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        
                        Calendar cal = Calendar.getInstance();
                        int todayDay = cal.get(Calendar.DAY_OF_MONTH);
                        int todayMonth = cal.get(Calendar.MONTH) + 1; // החודשים מתחילים מ-0 בלוח שנה
                        
                        if (day == todayDay && month == todayMonth) {
                            Toast.makeText(this, "מזל טוב " + user.getName() + "! 🎂🎉", Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    // התעלמות משגיאות בפורמט התאריך
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // עדכון סטטוס מחובר
        DataManager.getInstance().setUserOnlineStatus(true);
        checkAdminAccess();
        checkBirthday();
        loadProfileImage();
        if (showingUpdates) loadUpdates();
        else loadFeed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // עדכון סטטוס לא מחובר ביציאה מהמסך
        DataManager.getInstance().setUserOnlineStatus(false);
    }

    /**
     * טוען את פיד הסיכומים האחרונים מבסיס הנתונים.
     */
    private void loadFeed() {
        DataManager.getInstance().getLatestSummaries(summaries -> {
            feedList.clear();
            if (summaries != null) {
                feedList.addAll(summaries);
            }
            summaryAdapter.notifyDataSetChanged();
        });
    }

    /**
     * טוען את עדכוני האפליקציה מבסיס הנתונים.
     */
    private void loadUpdates() {
        DataManager.getInstance().getAppUpdates(updates -> {
            updatesList.clear();
            if (updates != null) {
                updatesList.addAll(updates);
            }
            if (updateAdapter != null) updateAdapter.notifyDataSetChanged();
        });
    }

    /**
     * טוען את תמונת הפרופיל של המשתמש הנוכחי.
     */
    private void loadProfileImage() {
        DataManager.getInstance().getCurrentUser(user -> {
            if (user != null) updateProfileImageUI(user);
        });
    }

    /**
     * מעדכן את רכיב התמונה במסך בהתאם לנתוני המשתמש.
     */
    private void updateProfileImageUI(DataManager.User user) {
        ivHomeProfileImage.setColorFilter(null);
        ivHomeProfileImage.setPadding(0, 0, 0, 0);
        if (user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
            try {
                ivHomeProfileImage.setImageURI(Uri.parse(user.getProfileImageUri()));
            } catch (Exception e) {
                setDefaultIcon();
            }
        } else {
            setDefaultIcon();
        }
    }

    /**
     * מגדיר אייקון ברירת מחדל אם אין תמונת פרופיל.
     */
    private void setDefaultIcon() {
        ivHomeProfileImage.setImageResource(android.R.drawable.ic_menu_gallery);
        ivHomeProfileImage.setPadding(20, 20, 20, 20);
        ivHomeProfileImage.setColorFilter(android.graphics.Color.parseColor("#7C3AED"));
    }
}
