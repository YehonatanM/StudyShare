package com.example.studyshare;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * מסך ראשי (MainActivity) - נקודת הכניסה לאפליקציה.
 * תפקידו לבדוק אם המשתמש כבר מחובר או שצריך לשלוח אותו למסך ההתחברות.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // בדיקה האם יש משתמש שמחובר כרגע למכשיר
        String currentUserEmail = DataManager.getInstance().getCurrentUserEmail();

        if (currentUserEmail != null) {
            // אם המשתמש מחובר, בודקים שהחשבון שלו עדיין תקין ולא חסום
            DataManager.getInstance().getCurrentUser(user -> {
                if (user != null && !user.isBlocked()) {
                    // המשתמש תקין - עוברים למסך הבית
                    navigateTo(HomeActivity.class);
                } else {
                    // המשתמש חסום או לא קיים - מוציאים אותו מהמערכת ושולחים להתחברות
                    DataManager.getInstance().logout();
                    navigateTo(LoginActivity.class);
                }
            });
        } else {
            // אין משתמש מחובר - עוברים למסך ההתחברות
            navigateTo(LoginActivity.class);
        }
    }

    /**
     * פונקציה פשוטה למעבר בין מסכים וסגירת המסך הנוכחי.
     */
    private void navigateTo(Class<?> targetClass) {
        startActivity(new Intent(MainActivity.this, targetClass));
        finish();
    }
}
