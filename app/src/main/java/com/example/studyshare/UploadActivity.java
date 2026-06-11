package com.example.studyshare;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.UUID;
// שורה 84,92,133,144
/**
 * מסך העלאה (UploadActivity) - מסך זה מאפשר למשתמשים להעלות סיכומים חדשים לאפליקציה.
 * המשתמש בוחר מקצוע, כותב תיאור קצר ומצרף קובץ (בדרך כלל PDF).
 */
public class UploadActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerSubject; // בחירת המקצוע
    private EditText etContent; // תיבת טקסט לתיאור הסיכום
    private TextView tvFileName; // מציג את שם הקובץ שנבחר
    private View btnSelectFile, btnUpload; // כפתורי בחירת קובץ והעלאה
    private String selectedFileUri = ""; // הכתובת של הקובץ שנבחר במכשיר

    // כלי לפתיחת מנהל הקבצים של המכשיר ובחירת קובץ
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handleFileSelection(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        initUI(); // אתחול הממשק
        setupSubjectDropdown(); // הגדרת רשימת המקצועות

        // הגדרת לחיצה על כפתור בחירת קובץ
        btnSelectFile.setOnClickListener(v -> openFilePicker());
        // הגדרת לחיצה על כפתור העלאה
        btnUpload.setOnClickListener(v -> performUpload());
    }

    /**
     * מקשר בין הקוד לרכיבים הגרפיים ומגדיר את כפתור החזרה.
     */
    private void initUI() {
        spinnerSubject = findViewById(R.id.spinnerSubject);
        etContent = findViewById(R.id.etContent);
        tvFileName = findViewById(R.id.tvFileName);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUpload = findViewById(R.id.btnUpload);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish()); // סגירת המסך וחזרה אחורה
    }

    /**
     * מגדיר את רשימת המקצועות לבחירה מתוך הרשימה הקיימת במנהל הנתונים.
     */
    private void setupSubjectDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, DataManager.SUBJECTS);
        spinnerSubject.setAdapter(adapter);
    }

    /**
     * פותח את סייר הקבצים של הטלפון כדי שהמשתמש יוכל לבחור קובץ PDF.
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        filePickerLauncher.launch(intent);
    }

    /**
     * מטפל בקובץ שנבחר על ידי המשתמש ושומר את הכתובת שלו.
     */
    private void handleFileSelection(Uri uri) {
        try {
            // קבלת הרשאה לקרוא את הקובץ גם בעתיד
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            selectedFileUri = uri.toString();
            String fileName = uri.getLastPathSegment();
            tvFileName.setText("Attached: " + (fileName != null ? fileName : "Document"));
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בבחירת הקובץ", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * מבצע את פעולת ההעלאה בפועל: בודק שכל השדות מלאים ושולח את המידע לבסיס הנתונים.
     */
    private void performUpload() {
        String content = etContent.getText().toString().trim();
        String subject = spinnerSubject.getText().toString().trim();

        // בדיקה שהמשתמש מילא הכל
        if (subject.isEmpty() || content.isEmpty() || selectedFileUri.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות וצרף קובץ", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false); // נטרול הכפתור כדי למנוע לחיצות כפולות

        // קבלת פרטי המשתמש המעלה כדי לשייך אליו את הסיכום
        DataManager.getInstance().getCurrentUser(user -> {
            if (user == null) {
                btnUpload.setEnabled(true);
                return;
            }

            // יצירת אובייקט סיכום חדש עם כל הפרטים
            String id = UUID.randomUUID().toString();
            Summary summary = new Summary(id, subject, content, 0, user.getEmail(), user.getName(), 
                    user.getProfileImageUri(), user.getId(), "Summary.pdf", selectedFileUri);

            // שמירה בבסיס הנתונים
            DataManager.getInstance().addSummary(summary, new DataManager.ActionCallback() {
                @Override
                public void onSuccess() {
                    // העלאה הצליחה - המשתמש מקבל 10 נקודות בונוס
                    DataManager.getInstance().addPoints(user.getEmail(), 10);
                    Toast.makeText(UploadActivity.this, "הסיכום פורסם! קיבלת 10 נקודות! 💎", Toast.LENGTH_LONG).show();
                    finish(); // חזרה למסך הקודם
                }
                @Override
                public void onFailure(String errorMessage) {
                    btnUpload.setEnabled(true);
                    Toast.makeText(UploadActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataManager.getInstance().setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataManager.getInstance().setUserOnlineStatus(false);
    }
}
