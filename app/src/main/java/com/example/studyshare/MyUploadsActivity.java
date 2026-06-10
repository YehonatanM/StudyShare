package com.example.studyshare;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

/**
 * מסך העלאות שלי (MyUploadsActivity) - מציג למשתמש את כל הסיכומים שהוא העלה לאפליקציה.
 * המשתמש יכול לראות כאן את רשימת התכנים האישית שלו ולעקוב אחרי החומרים ששיתף עם הקהילה.
 */
public class MyUploadsActivity extends AppCompatActivity {

    private RecyclerView rvMyUploads; // רכיב המציג את רשימת הסיכומים של המשתמש
    private TextView tvEmptyState; // טקסט שמוצג רק במידה והמשתמש עדיין לא העלה אף סיכום
    private SummaryAdapter adapter; // מתאם שאחראי על חיבור הנתונים לרשימה
    private final List<Summary> mySummaries = new ArrayList<>(); // רשימת הסיכומים שתטען מהשרת

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_uploads);

        // הגדרת כפתור החזרה למסך הקודם בשורת הכותרת
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMyUploads = findViewById(R.id.rvMyUploads);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // הגדרת רשימת ה-RecyclerView עם מנהל פריסה ליניארי
        rvMyUploads.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SummaryAdapter(mySummaries);
        rvMyUploads.setAdapter(adapter);

        loadMyUploads(); // טעינת הסיכומים האישיים של המשתמש מבסיס הנתונים
    }

    /**
     * טוען את כל הסיכומים שהועלו על ידי המשתמש המחובר כרגע.
     */
    private void loadMyUploads() {
        String email = DataManager.getInstance().getCurrentUserEmail();
        if (email == null) return;

        // שליפת הנתונים דרך ה-DataManager
        DataManager.getInstance().getSummariesByUser(email, summaries -> {
            mySummaries.clear();
            if (summaries != null) {
                mySummaries.addAll(summaries);
            }
            updateUI(); // עדכון מצב הממשק בהתאם לתוצאות
        });
    }

    /**
     * מעדכן את הממשק: אם אין סיכומים, מציג הודעה שהרשימה ריקה, אחרת מציג את הרשימה.
     */
    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (mySummaries.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvMyUploads.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvMyUploads.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // עדכון סטטוס מחובר
        DataManager.getInstance().setUserOnlineStatus(true);
        loadMyUploads(); // רענון הרשימה בכל פעם שחוזרים למסך
    }

    @Override
    protected void onPause() {
        super.onPause();
        // עדכון סטטוס לא מחובר ביציאה מהמסך
        DataManager.getInstance().setUserOnlineStatus(false);
    }
}
