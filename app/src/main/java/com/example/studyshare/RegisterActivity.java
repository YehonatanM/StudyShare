package com.example.studyshare;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
// שורה 124
/**
 * מסך הרשמה (RegisterActivity) - מאפשר למשתמשים חדשים ליצור חשבון באפליקציה.
 * המסך אוסף פרטים אישיים כמו שם, אימייל, סיסמה, כיתה ותאריך לידה.
 */
public class RegisterActivity extends AppCompatActivity {

    // שדות הקלט להזנת פרטי המשתמש
    private TextInputEditText etName, etEmail, etPassword, etDob;
    private AutoCompleteTextView spinnerClass; // בחירת כיתה מתוך רשימה סגורה
    private Button btnRegister; // כפתור לביצוע פעולת ההרשמה
    private TextView tvGoToLogin; // טקסט למעבר מהיר למסך ההתחברות למשתמשים קיימים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initUI(); // אתחול וקישור רכיבי הממשק הגרפי
        setupClassDropdown(); // הגדרת רשימת הכיתות הזמינות לבחירה
        setupDatePicker(); // הגדרת בחירת תאריך לידה באמצעות דיאלוג לוח שנה

        // הגדרת פעולה ללחיצה על כפתור ההרשמה
        btnRegister.setOnClickListener(v -> performRegistration());

        // חזרה למסך ההתחברות למשתמשים שכבר רשומים במערכת
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    /**
     * מקשר בין המשתנים המוגדרים בקוד לבין רכיבי התצוגה הגרפיים ב-XML.
     */
    private void initUI() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etDob = findViewById(R.id.etDob);
        spinnerClass = findViewById(R.id.spinnerClass);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    /**
     * מגדיר את רשימת הכיתות הזמינות לבחירה (מכיתה ז' ועד רמת אוניברסיטה).
     */
    private void setupClassDropdown() {
        String[] classes = {"7th Grade", "8th Grade", "9th Grade", "10th Grade", "11th Grade", "12th Grade", "University"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, classes);
        spinnerClass.setAdapter(adapter);
    }

    /**
     * פותח חלונית בחירת תאריך (DatePicker) כאשר המשתמש לוחץ על שדה תאריך הלידה.
     */
    private void setupDatePicker() {
        etDob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // יצירת חלונית בחירת תאריך המציעה כברירת מחדל גיל 16 שנים אחורה
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etDob.setText(date);
                    }, year - 16, month, day);
            datePickerDialog.show();
        });
    }

    /**
     * מבצע את תהליך ההרשמה: בודק תקינות נתונים ושולח אותם ל-DataManager לצורך יצירת המשתמש.
     */
    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String className = spinnerClass.getText().toString().trim();
        String dob = etDob.getText().toString().trim();

        // בדיקה שכל השדות הנדרשים מולאו על ידי המשתמש
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || className.isEmpty() || dob.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה שהסיסמה שהוזנה עומדת בדרישת המינימום של 6 תווים
        if (password.length() < 6) {
            Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
            return;
        }

        // קריאה לפעולת ההרשמה במנהל הנתונים
        DataManager.getInstance().registerUser(name, email, password, className, dob, new DataManager.ActionCallback() {
            @Override
            public void onSuccess() {
                // ההרשמה הסתיימה בהצלחה - הודעה למשתמש ומעבר אוטומטי למסך הבית
                Toast.makeText(RegisterActivity.this, "החשבון נוצר בהצלחה!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                // ההרשמה נכשלה - הצגת הודעת השגיאה המדויקת מהשרת
                Toast.makeText(RegisterActivity.this, "ההרשמה נכשלה: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
