package com.example.studyshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
// שורה 81,32
/**
 * מסך ניהול (AdminActivity) - מסך זה מיועד למנהלי המערכת בלבד.
 * הוא מאפשר לנהל את המשתמשים באפליקציה, לצפות בסיכומים, לפרסם עדכונים ולטפל בדיווחים על תוכן לא הולם.
 */
public class AdminActivity extends AppCompatActivity {

    private RecyclerView rvAdminList; // רשימה להצגת הפריטים (משתמשים, סיכומים וכו')
    private TabLayout tabLayout; // לשוניות למעבר בין קטגוריות הניהול
    private FloatingActionButton fabAddUpdate; // כפתור להוספת עדכון חדש
    private final List<Object> currentList = new ArrayList<>(); // הרשימה הנוכחית שמוצגת
    private ListenerRegistration usersListener; // מאזין לשינויים במשתמשים בזמן אמת

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        initUI(); // אתחול הממשק הגרפי
        startUsersRealtimeListener(); // התחלת מעקב אחרי רשימת המשתמשים
    }

    /**
     * מקשר בין הקוד לרכיבי התצוגה ומגדיר את פעולות הכפתורים.
     */
    private void initUI() {
        rvAdminList = findViewById(R.id.rvAdminList);
        tabLayout = findViewById(R.id.tabLayout);
        fabAddUpdate = findViewById(R.id.fabAddUpdate);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // חזרה למסך הבית בצורה נקייה
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        
        rvAdminList.setLayoutManager(new LinearLayoutManager(this));
        setupTabListener(); // הגדרת המעבר בין הלשוניות השונות
        fabAddUpdate.setOnClickListener(v -> showAddUpdateDialog()); // הצגת דיאלוג להוספת עדכון
    }

    /**
     * מגדיר מה קורה כשבוחרים לשונית אחרת (למשל: עוברים מניהול משתמשים לניהול דיווחים).
     */
    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                fabAddUpdate.setVisibility(View.GONE); // הסתרת כפתור העדכון כברירת מחדל
                int pos = tab.getPosition();
                stopUsersListener(); // הפסקת המאזין הקודם כדי לחסוך במשאבי מערכת
                
                if (pos == 0) startUsersRealtimeListener(); // לשונית משתמשים
                else if (pos == 1) showSummaries(); // לשונית סיכומים
                else if (pos == 2) {
                    showUpdates(); // לשונית עדכונים
                    // רק סופר אדמין רשאי לפרסם עדכוני מערכת חדשים
                    if (DataManager.getInstance().isSuperAdmin()) fabAddUpdate.setVisibility(View.VISIBLE);
                } else if (pos == 3) showReports(); // לשונית דיווחים
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * מאזין לשינויים ברשימת המשתמשים ב-Firestore ומעדכן את המסך בזמן אמת.
     */
    private void startUsersRealtimeListener() {
        stopUsersListener();
        usersListener = FirebaseFirestore.getInstance().collection("users")
                .addSnapshotListener((value, error) -> {
                    if (value != null && tabLayout.getSelectedTabPosition() == 0) {
                        currentList.clear();
                        currentList.addAll(value.toObjects(DataManager.User.class));
                        rvAdminList.setAdapter(new AdminAdapter(currentList, 1)); // קוד 1 מייצג משתמשים
                    }
                });
    }

    /**
     * מפסיק את המאזין לרשימת המשתמשים כדי למנוע דליפות זיכרון.
     */
    private void stopUsersListener() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
        }
    }

    /**
     * טוען ומציג את רשימת כל הסיכומים הקיימים במערכת.
     */
    private void showSummaries() {
        DataManager.getInstance().getSummaries(summaries -> {
            currentList.clear();
            if (summaries != null) currentList.addAll(summaries);
            rvAdminList.setAdapter(new AdminAdapter(currentList, 2)); // קוד 2 מייצג סיכומים
        });
    }

    /**
     * טוען ומציג את רשימת עדכוני האפליקציה שפורסמו.
     */
    private void showUpdates() {
        DataManager.getInstance().getAppUpdates(updates -> {
            currentList.clear();
            if (updates != null) currentList.addAll(updates);
            rvAdminList.setAdapter(new AdminAdapter(currentList, 3)); // קוד 3 מייצג עדכונים
        });
    }

    /**
     * טוען ומציג את רשימת הדיווחים ששלחו המשתמשים על תוכן בעייתי.
     */
    private void showReports() {
        DataManager.getInstance().getReports(reports -> {
            currentList.clear();
            if (reports != null) currentList.addAll(reports);
            rvAdminList.setAdapter(new AdminAdapter(currentList, 4)); // קוד 4 מייצג דיווחים
        });
    }

    /**
     * מציג חלונית (דיאלוג) המאפשרת למנהל להזין ולפרסם עדכון מערכת חדש.
     */
    private void showAddUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Update");
        
        // יצירת פריסת טקסט עבור הדיאלוג
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        final EditText etTitle = new EditText(this);
        etTitle.setHint("Update Title");
        layout.addView(etTitle);
        
        final EditText etContent = new EditText(this);
        etContent.setHint("Update Content");
        etContent.setMinLines(3);
        layout.addView(etContent);
        
        builder.setView(layout);
        
        // כפתור אישור ופרסום העדכון
        builder.setPositiveButton("Publish", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (!title.isEmpty() && !content.isEmpty()) {
                // יצירת אובייקט עדכון חדש ושמירתו בשרת
                DataManager.AppUpdate update = new DataManager.AppUpdate(UUID.randomUUID().toString(), title, content);
                DataManager.getInstance().addAppUpdate(update, new DataManager.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        showUpdates(); // רענון הרשימה לאחר הפרסום
                        Toast.makeText(AdminActivity.this, "Published", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(String e) {}
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // עדכון סטטוס מחובר של המנהל
        DataManager.getInstance().setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // עדכון סטטוס לא מחובר ביציאה מהמסך
        DataManager.getInstance().setUserOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUsersListener(); // ניקוי המאזין לסגירת המסך
    }
}
