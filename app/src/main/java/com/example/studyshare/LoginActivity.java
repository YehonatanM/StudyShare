package com.example.studyshare;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * מסך התחברות (LoginActivity) - המסך שבו המשתמש מזין אימייל וסיסמה כדי להיכנס לאפליקציה.
 * מסך זה מטפל באימות המשתמש מול Firebase דרך ה-DataManager.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword; // תיבות טקסט להזנת אימייל וסיסמה
    private Button btnLogin; // כפתור לביצוע פעולת ההתחברות
    private TextView tvGoToRegister; // טקסט המאפשר מעבר למסך ההרשמה למשתמשים חדשים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אם המשתמש כבר מחובר (קיים אימייל שמור ב-Auth), נעביר אותו ישירות למסך הבית
        if (DataManager.getInstance().getCurrentUserEmail() != null) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        initUI(); // אתחול וקישור רכיבי הממשק

        // הגדרת מה קורה כשלוחצים על כפתור התחברות
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // בדיקה שכל השדות מולאו לפני שליחה לשרת
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }

            // ניסיון התחברות דרך מנהל הנתונים
            DataManager.getInstance().loginUser(email, password, new DataManager.ActionCallback() {
                @Override
                public void onSuccess() {
                    // התחברות הצליחה - מעבר למסך הבית וסגירת מסך הלוגין
                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    // התחברות נכשלה - הצגת הודעת שגיאה מפורטת למשתמש
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        });

        // מעבר למסך ההרשמה בלחיצה על הטקסט המתאים
        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    /**
     * מקשר בין המשתנים המוגדרים בקוד לבין רכיבי התצוגה הגרפיים בקובץ ה-XML.
     */
    private void initUI() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
    }
}
