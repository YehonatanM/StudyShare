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
 * מסך סיכומים שמורים (SavedSummariesActivity) - מציג למשתמש את כל הסיכומים שהוא שמר (Bookmarks).
 * המסך מאפשר גישה מהירה לחומרים שהמשתמש מצא כשימושיים ושמר לעצמו.
 */
public class SavedSummariesActivity extends AppCompatActivity {

    private RecyclerView rvSavedSummaries; // הרכיב שמציג את רשימת הסיכומים השמורים
    private TextView tvEmptyState; // טקסט שמוצג אם המשתמש לא שמר שום סיכום עדיין
    private SummaryAdapter adapter; // המתאם שמחבר את הסיכומים לרשימה
    private final List<Summary> savedList = new ArrayList<>(); // רשימת הסיכומים שתטען מהשרת

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_summaries);

        // הגדרת כפתור חזרה למסך הקודם
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSavedSummaries = findViewById(R.id.rvSavedSummaries);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // הגדרת הרשימה (RecyclerView)
        rvSavedSummaries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SummaryAdapter(savedList);
        rvSavedSummaries.setAdapter(adapter);

        loadSavedSummaries(); // טעינת הסיכומים השמורים
    }

    /**
     * טוען את רשימת הסיכומים שהמשתמש הנוכחי שמר בסימניות שלו.
     */
    private void loadSavedSummaries() {
        DataManager.getInstance().getCurrentUser(user -> {
            // בודק אם למשתמש יש רשימת סיכומים שמורים
            if (user != null && user.getSavedSummaries() != null && !user.getSavedSummaries().isEmpty()) {
                DataManager.getInstance().getSavedSummaries(user.getSavedSummaries(), summaries -> {
                    savedList.clear();
                    if (summaries != null) {
                        savedList.addAll(summaries);
                    }
                    updateUI(); // עדכון הממשק לאחר הטעינה
                });
            } else {
                // אם אין סיכומים שמורים, מנקים את הרשימה ומעדכנים
                savedList.clear();
                updateUI();
            }
        });
    }

    /**
     * מעדכן את המסך: אם הרשימה ריקה, מציג הודעה מתאימה.
     */
    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (savedList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvSavedSummaries.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvSavedSummaries.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataManager.getInstance().setUserOnlineStatus(true);
        // רענון הרשימה למקרה שהמשתמש הסיר סימניה במסך אחר
        loadSavedSummaries();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataManager.getInstance().setUserOnlineStatus(false);
    }
}
