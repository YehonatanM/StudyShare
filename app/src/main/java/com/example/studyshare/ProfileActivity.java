package com.example.studyshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * מסך פרופיל (ProfileActivity) - מציג את הפרטים האישיים של המשתמש.
 * מאפשר למשתמש לעדכן את שמו, תמונת הפרופיל והכיתה שלו, וכן לצפות בסיכומים שהוא העלה ובנקודות שצבר.
 */
public class ProfileActivity extends AppCompatActivity {

    // רכיבי התצוגה של המסך (טקסטים, כפתורים ותמונות)
    private TextView tvEmail, tvClass, tvDob, tvUploadCount, tvUserPoints, tvProfileTitle;
    private Button btnBackHome, btnAdminPanel, btnSavedSummaries;
    private ImageView ivEditName, ivProfileImage;
    private View profileImageCard, cvMyUploadsLink, cvLeaderboardLink;
    private String userEmail; // כתובת האימייל של המשתמש שאת הפרופיל שלו מציגים

    /**
     * כלי לבחירת תמונה מהגלריה של המכשיר ועדכונה בבסיס הנתונים.
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            // קבלת הרשאה קבועה לגישה לתמונה שנבחרה
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            // עדכון כתובת התמונה החדשה ב-DataManager
                            DataManager.getInstance().updateUserImage(uri.toString());
                            loadUserProfile(); // רענון התצוגה של הפרופיל
                        } catch (Exception e) {
                            Toast.makeText(this, "שגיאה בשמירת התמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initUI(); // אתחול וקישור רכיבי הממשק

        // קביעה איזה פרופיל להציג - של המשתמש המחובר או של משתמש אחר שנבחר
        userEmail = getIntent().getStringExtra("userEmail");
        if (userEmail == null) {
            userEmail = DataManager.getInstance().getCurrentUserEmail();
        }

        // בדיקה האם המשתמש המחובר צופה בפרופיל האישי שלו
        boolean isOwnProfile = userEmail != null && userEmail.equalsIgnoreCase(DataManager.getInstance().getCurrentUserEmail());
        
        loadUserProfile(); // טעינת נתוני המשתמש
        loadUserStats(); // טעינת נתונים סטטיסטיים (כמות העלאות)

        // במידה וזהו הפרופיל האישי, נאפשר עריכה של הפרטים
        if (isOwnProfile) {
            enableEditing();
        } else {
            ivEditName.setVisibility(View.GONE);
            btnSavedSummaries.setVisibility(View.GONE);
        }
        
        // הגדרת מאזינים ללחיצה על כפתורי הניווט
        btnBackHome.setOnClickListener(v -> finish());
        btnSavedSummaries.setOnClickListener(v -> startActivity(new Intent(this, SavedSummariesActivity.class)));
        
        cvMyUploadsLink.setOnClickListener(v -> {
            startActivity(new Intent(this, MyUploadsActivity.class));
        });
        
        cvLeaderboardLink.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
    }

    /**
     * מקשר בין המשתנים המוגדרים בקוד לבין הרכיבים הגרפיים ב-XML.
     */
    private void initUI() {
        tvProfileTitle = findViewById(R.id.tvProfileTitle);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvClass = findViewById(R.id.tvProfileClass);
        tvDob = findViewById(R.id.tvProfileDob);
        tvUploadCount = findViewById(R.id.tvUploadCount);
        tvUserPoints = findViewById(R.id.tvUserPoints);
        btnBackHome = findViewById(R.id.btnBackHome);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);
        btnSavedSummaries = findViewById(R.id.btnSavedSummaries);
        ivEditName = findViewById(R.id.ivEditName);
        ivProfileImage = findViewById(R.id.ivProfileImage);
        profileImageCard = findViewById(R.id.profileImageCard);
        cvMyUploadsLink = findViewById(R.id.cvMyUploadsLink);
        cvLeaderboardLink = findViewById(R.id.cvLeaderboardLink);
    }

    /**
     * מאפשר למשתמש לבצע עריכה של פרטיו האישיים.
     */
    private void enableEditing() {
        ivEditName.setVisibility(View.VISIBLE);
        ivEditName.setOnClickListener(v -> showEditNameDialog());
        
        // לחיצה על מסגרת התמונה תפתח את בחירת הקבצים
        profileImageCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // טיפול בעריכת שדה הכיתה
        if (DataManager.getInstance().isSuperAdmin() && userEmail.equalsIgnoreCase(DataManager.getInstance().getCurrentUserEmail())) {
            tvClass.setOnClickListener(v -> Toast.makeText(this, "כיתת מערכת לא ניתנת לשינוי", Toast.LENGTH_SHORT).show());
        } else {
            tvClass.setOnClickListener(v -> showClassSelection());
        }

        // תאריך הלידה נקבע בהרשמה ולא ניתן לשינוי לאחר מכן
        tvDob.setOnClickListener(v -> Toast.makeText(this, "תאריך לידה לא ניתן לשינוי לאחר הרישום", Toast.LENGTH_SHORT).show());

        // הצגת כפתור ניהול במידה והמשתמש הוא סופר אדמין
        if (DataManager.getInstance().isSuperAdmin()) {
            btnAdminPanel.setVisibility(View.VISIBLE);
            btnAdminPanel.setOnClickListener(v -> startActivity(new Intent(this, AdminActivity.class)));
        }
    }

    /**
     * שולף את פרטי הפרופיל מבסיס הנתונים ומציג אותם על המסך.
     */
    private void loadUserProfile() {
        DataManager.getInstance().getUserByEmail(userEmail, user -> {
            if (user != null) {
                String displayName = user.getName();
                
                // מנהלים (כולל סופר אדמין) יכולים לראות את המזהה המספרי של המשתמש
                boolean currentIsAdmin = DataManager.getInstance().isSuperAdmin();
                if (currentIsAdmin && user.getId() > 0) {
                    displayName += " [ID: " + user.getId() + "]";
                }
                
                tvProfileTitle.setText(displayName);
                tvEmail.setText("Email: " + user.getEmail());
                tvClass.setText("Class: " + user.getClassName());
                tvDob.setText("Date of Birth: " + user.getDob());
                tvUserPoints.setText(String.valueOf(user.getPoints()));
                
                // טעינת תמונת הפרופיל האישית או הצגת סמל ברירת מחדל
                if (user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
                    try {
                        ivProfileImage.setImageURI(Uri.parse(user.getProfileImageUri()));
                    } catch (Exception e) {
                        ivProfileImage.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } else {
                    ivProfileImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        });
    }

    /**
     * שולף את מספר הסיכומים שהועלו על ידי המשתמש.
     */
    private void loadUserStats() {
        DataManager.getInstance().getSummariesByUser(userEmail, summaries -> {
            tvUploadCount.setText(String.valueOf(summaries.size()));
        });
    }

    /**
     * מציג דיאלוג המאפשר למשתמש לעדכן את השם שלו.
     */
    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("עריכת שם");
        final android.widget.EditText input = new android.widget.EditText(this);
        
        // הסרת ה-ID מהכותרת לצורך עריכת השם בלבד
        String currentTitle = tvProfileTitle.getText().toString();
        if (currentTitle.contains(" [ID:")) {
            currentTitle = currentTitle.substring(0, currentTitle.indexOf(" [ID:"));
        }
        input.setText(currentTitle);
        
        builder.setView(input);
        builder.setPositiveButton("שמור", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                DataManager.getInstance().updateUserName(name);
                loadUserProfile(); // רענון הנתונים במסך
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    /**
     * מציג רשימת בחירה של כיתות לצורך עדכון הכיתה של המשתמש.
     */
    private void showClassSelection() {
        String[] classes = {"7th Grade", "8th Grade", "9th Grade", "10th Grade", "11th Grade", "12th Grade", "University"};
        new AlertDialog.Builder(this).setTitle("בחר כיתה")
                .setItems(classes, (d, which) -> {
                    DataManager.getInstance().updateFullProfile(classes[which], tvDob.getText().toString().replace("Date of Birth: ", ""));
                    tvClass.setText("Class: " + classes[which]);
                }).show();
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
