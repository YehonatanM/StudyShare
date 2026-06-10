package com.example.studyshare;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך תגובות (CommentsActivity) - מסך זה מנהל את שרשור התגובות עבור סיכום ספציפי.
 * הוא מאפשר למשתמשים לצפות בתגובות שכתבו אחרים ולהוסיף תגובה חדשה משלהם בזמן אמת.
 */
public class CommentsActivity extends AppCompatActivity {

    private RecyclerView rvCommentsList; // רכיב המציג את רשימת התגובות
    private CommentAdapter adapter; // המתאם המקשר בין רשימת התגובות לתצוגה
    private List<Comment> commentList; // רשימת אובייקטי התגובות של הסיכום הנוכחי
    private EditText etCommentInput; // תיבת טקסט להזנת תוכן תגובה חדשה
    private FloatingActionButton btnSendComment; // כפתור שליחת התגובה
    private Summary currentSummary; // אובייקט הסיכום אליו משויכות התגובות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // קבלת מזהה הסיכום שהועבר מהמסך הקודם
        String summaryId = getIntent().getStringExtra("summaryId");
        if (summaryId == null) {
            finish(); // סגירת המסך אם לא סופק מזהה תקין
            return;
        }
        
        initUI(); // אתחול וקישור רכיבי הממשק הגרפי
        loadSummaryAndComments(summaryId); // טעינת נתוני הסיכום והתגובות שלו מהשרת
    }

    /**
     * מקשר בין המשתנים בקוד לרכיבים הגרפיים המוגדרים בקובץ ה-XML ומגדיר את כפתור החזרה.
     */
    private void initUI() {
        rvCommentsList = findViewById(R.id.rvCommentsList);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish()); // חזרה למסך הקודם
    }

    /**
     * שולף את פרטי הסיכום המלאים ואת רשימת התגובות שלו מבסיס הנתונים.
     */
    private void loadSummaryAndComments(String summaryId) {
        DataManager.getInstance().getSummaryById(summaryId, summary -> {
            if (summary == null) {
                Toast.makeText(this, "הסיכום כבר לא קיים", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            currentSummary = summary;
            commentList = currentSummary.getComments();
            if (commentList == null) {
                commentList = new ArrayList<>();
            }
            
            // הגדרת רשימת התצוגה (RecyclerView) עם מנהל פריסה ומתאם
            adapter = new CommentAdapter(commentList, currentSummary.getId());
            rvCommentsList.setLayoutManager(new LinearLayoutManager(this));
            rvCommentsList.setAdapter(adapter);

            setupSendButton(); // הגדרת פעולת כפתור שליחת התגובה
        });
    }

    /**
     * מגדיר את הלוגיקה של כפתור השליחה: יצירת תגובה חדשה, שמירתה בשרת ועדכון הממשק.
     */
    private void setupSendButton() {
        btnSendComment.setOnClickListener(v -> {
            String text = etCommentInput.getText().toString().trim();
            if (!text.isEmpty()) {
                // קבלת פרטי המשתמש המחובר לצורך שיוך התגובה אליו
                DataManager.getInstance().getCurrentUser(user -> {
                    if (user == null) return;

                    // יצירת אובייקט תגובה חדש
                    Comment newComment = new Comment(user.getEmail(), user.getName(), text);
                    
                    // שמירת התגובה בבסיס הנתונים דרך ה-DataManager
                    DataManager.getInstance().addComment(currentSummary.getId(), newComment, new DataManager.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            // הוספת התגובה לרשימה המקומית וגלילה לסוף הרשימה
                            commentList.add(newComment);
                            adapter.notifyItemInserted(commentList.size() - 1);
                            rvCommentsList.scrollToPosition(commentList.size() - 1);
                            etCommentInput.setText(""); // ניקוי שדה הקלט
                            etCommentInput.clearFocus();
                        }
                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(CommentsActivity.this, "שגיאה: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // עדכון סטטוס מחובר
        DataManager.getInstance().setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // עדכון סטטוס לא מחובר ביציאה מהמסך
        DataManager.getInstance().setUserOnlineStatus(false);
    }
}
