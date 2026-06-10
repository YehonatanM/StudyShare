package com.example.studyshare;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.List;

/**
 * מסך לוח מובילים (LeaderboardActivity) - מציג את רשימת המשתמשים שצברו הכי הרבה נקודות באפליקציה.
 * המטרה היא לעודד תחרות חיובית בין המשתמשים שמשתפים חומרי לימוד ותורמים לקהילה.
 */
public class LeaderboardActivity extends AppCompatActivity {

    private RecyclerView rvLeaderboard; // הרכיב האחראי על הצגת רשימת המובילים
    private LeaderboardAdapter adapter; // המתאם שמחבר את נתוני המשתמשים לרשימה בתצוגה
    private final List<DataManager.User> topUsersList = new ArrayList<>(); // רשימה המכילה את נתוני המשתמשים המובילים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // הגדרת כפתור חזרה בשורת הכותרת (Toolbar)
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // אתחול והגדרת ה-RecyclerView להצגת הרשימה בצורה אנכית
        rvLeaderboard = findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        
        // יצירת המתאם וקישורו לרשימה
        adapter = new LeaderboardAdapter(topUsersList);
        rvLeaderboard.setAdapter(adapter);

        loadLeaderboard(); // טעינת נתוני המשתמשים המובילים מבסיס הנתונים
    }

    /**
     * טוען את 50 המשתמשים עם מספר הנקודות הגבוה ביותר ומעדכן את הממשק.
     */
    private void loadLeaderboard() {
        DataManager.getInstance().getTopUsers(50, users -> {
            topUsersList.clear();
            if (users != null) {
                topUsersList.addAll(users);
            }
            adapter.notifyDataSetChanged(); // רענון רשימת התצוגה לאחר טעינת הנתונים
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // עדכון סטטוס מחובר עבור המשתמש הנוכחי
        DataManager.getInstance().setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // עדכון סטטוס לא מחובר ביציאה מהמסך
        DataManager.getInstance().setUserOnlineStatus(false);
    }
}
