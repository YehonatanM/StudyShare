package com.example.studyshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
// שורה 66
/**
 * מסך פרטי סיכום (SummaryDetailActivity) - מציג את הפרטים המלאים של סיכום לימודי.
 * מאפשר למשתמש לצפות בתוכן, להוריד את הקובץ המצורף, לצפות בתגובות ולדווח על תוכן בעייתי.
 */
public class SummaryDetailActivity extends AppCompatActivity {

    private TextView tvDetailSubject, tvDetailTitle, tvDetailUser, tvDetailDate, tvDetailContent;
    private ShapeableImageView ivDetailUserIcon;
    private MaterialButton btnDetailDownload, btnDetailComments, btnReportSummary;
    private Summary currentSummary; // האובייקט המכיל את נתוני הסיכום הנוכחי

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_detail);

        // שליפת מזהה הסיכום שהועבר ב-Intent
        String summaryId = getIntent().getStringExtra("summaryId");
        if (summaryId == null) {
            finish();
            return;
        }

        initUI(); // אתחול רכיבי הממשק
        loadSummary(summaryId); // טעינת נתוני הסיכום מהשרת
    }

    /**
     * מקשר בין המשתנים בקוד לבין רכיבי התצוגה ב-XML ומגדיר את כפתור החזרה.
     */
    private void initUI() {
        tvDetailSubject = findViewById(R.id.tvDetailSubject);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailUser = findViewById(R.id.tvDetailUser);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailContent = findViewById(R.id.tvDetailContent);
        ivDetailUserIcon = findViewById(R.id.ivDetailUserIcon);
        btnDetailDownload = findViewById(R.id.btnDetailDownload);
        btnDetailComments = findViewById(R.id.btnDetailComments);
        btnReportSummary = findViewById(R.id.btnReportSummary);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // הגדרת מאזין לכפתור הדיווח
        btnReportSummary.setOnClickListener(v -> showReportDialog());
    }

    /**
     * טוען את פרטי הסיכום מבסיס הנתונים ומעדכן את הממשק.
     */
    private void loadSummary(String id) {
        DataManager.getInstance().getSummaryById(id, summary -> {
            if (summary == null) {
                Toast.makeText(this, "הסיכום לא נמצא", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            currentSummary = summary;
            updateUI(); // עדכון רכיבי המסך בנתונים
        });
    }

    /**
     * מעדכן את רכיבי התצוגה בפרטי הסיכום הטעונים.
     */
    private void updateUI() {
        tvDetailSubject.setText(currentSummary.getSubject());
        tvDetailTitle.setText(currentSummary.getSubject() + " Summary");
        tvDetailContent.setText(currentSummary.getContent());
        
        // הצגת שם המשתמש המעלה (כולל מזהה אם המשתמש הוא סופר אדמין)
        String userDisplay = currentSummary.getUserName();
        if (DataManager.getInstance().isSuperAdmin()) {
            userDisplay += " (ID: " + currentSummary.getUploaderId() + ")";
        }
        tvDetailUser.setText(userDisplay);

        // פורמט והצגת תאריך ההעלאה
        if (currentSummary.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDetailDate.setText("Uploaded on " + sdf.format(new Date(currentSummary.getTimestamp())));
        }

        // טעינת תמונת הפרופיל של המעלה אם קיימת
        if (currentSummary.getUserProfileImageUri() != null && !currentSummary.getUserProfileImageUri().isEmpty()) {
            try {
                ivDetailUserIcon.setImageURI(Uri.parse(currentSummary.getUserProfileImageUri()));
            } catch (Exception e) {
                ivDetailUserIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        // הגדרת כפתור הורדת הקובץ (PDF)
        btnDetailDownload.setOnClickListener(v -> {
            if (currentSummary.getFileUri() != null && !currentSummary.getFileUri().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(currentSummary.getFileUri()), "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                Toast.makeText(this, "אין קובץ מצורף", Toast.LENGTH_SHORT).show();
            }
        });

        // עדכון כפתור התגובות והעברה למסך התגובות
        int commentCount = currentSummary.getCommentsCount();
        btnDetailComments.setText("View Discussion (" + commentCount + " comments)");
        btnDetailComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("summaryId", currentSummary.getId());
            startActivity(intent);
        });
    }

    /**
     * מציג דיאלוג למשתמש לצורך דיווח על תוכן לא הולם.
     */
    private void showReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("דיווח על תוכן");
        
        final EditText input = new EditText(this);
        input.setHint("סיבת הדיווח...");
        builder.setView(input);

        builder.setPositiveButton("שלח דיווח", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            if (!reason.isEmpty()) {
                String reporterEmail = DataManager.getInstance().getCurrentUserEmail();
                // יצירת אובייקט דיווח חדש ושליחתו
                Report report = new Report(
                    UUID.randomUUID().toString(),
                    currentSummary.getId(),
                    currentSummary.getSubject() + " Summary",
                    reporterEmail,
                    reason
                );
                
                DataManager.getInstance().addReport(report, new DataManager.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(SummaryDetailActivity.this, "תודה. הדיווח הועבר לבדיקת מנהל.", Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onFailure(String e) {
                        Toast.makeText(SummaryDetailActivity.this, "הדיווח נכשל: " + e, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
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
        // עדכון סטטוס לא מחובר ביציאה
        DataManager.getInstance().setUserOnlineStatus(false);
    }
}
