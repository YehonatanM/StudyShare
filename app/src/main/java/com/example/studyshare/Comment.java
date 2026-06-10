package com.example.studyshare;

import java.util.UUID;

/**
 * תגובה - מחלקה זו מייצגת תגובה שמשתמש כתב על סיכום מסוים.
 * לכל תגובה יש כותב, טקסט ומזהה ייחודי.
 */
public class Comment {
    private String id; // מזהה ייחודי לתגובה
    private String userEmail; // האימייל של כותב התגובה
    private String userName; // שם המשתמש שכתב את התגובה
    private String text; // תוכן התגובה עצמה

    /**
     * בנאי ריק שדרוש עבור Firebase כדי שיוכל לקרוא את הנתונים ולהמירם לאובייקט.
     */
    public Comment() {}

    /**
     * יצירת תגובה חדשה עם פרטי הכותב והטקסט.
     * המזהה (ID) נוצר באופן אוטומטי ואקראי.
     */
    public Comment(String userEmail, String userName, String text) {
        this.id = UUID.randomUUID().toString(); // יצירת מזהה אקראי ייחודי (UUID)
        this.userEmail = userEmail;
        this.userName = userName;
        this.text = text;
    }

    // פונקציות לקבלת ועדכון (Getters & Setters) של פרטי התגובה

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
