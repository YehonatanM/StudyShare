package com.example.studyshare;

/**
 * דיווח - מחלקה זו מייצגת דיווח על סיכום שאינו הולם.
 * היא שומרת את פרטי הדיווח כדי שהמנהלים יוכלו לבדוק אותו ולפעול בהתאם.
 */
public class Report {
    private String id; // מזהה ייחודי לדיווח
    private String summaryId; // המזהה של הסיכום עליו מדווחים
    private String summaryTitle; // כותרת הסיכום הבעייתי
    private String reporterEmail; // האימייל של המשתמש שדיווח
    private String reason; // סיבת הדיווח (למה התוכן לא הולם)
    private long timestamp; // הזמן שבו בוצע הדיווח (במילישניות)

    /**
     * בנאי ריק שדרוש עבור Firebase כדי שיוכל ליצור מופע של המחלקה מהנתונים השמורים.
     */
    public Report() {}

    /**
     * יצירת דיווח חדש עם כל הפרטים הדרושים.
     * 
     * @param id מזהה הדיווח
     * @param summaryId מזהה הסיכום
     * @param summaryTitle כותרת הסיכום
     * @param reporterEmail אימייל המדווח
     * @param reason סיבת הדיווח
     */
    public Report(String id, String summaryId, String summaryTitle, String reporterEmail, String reason) {
        this.id = id;
        this.summaryId = summaryId;
        this.summaryTitle = summaryTitle;
        this.reporterEmail = reporterEmail;
        this.reason = reason;
        this.timestamp = System.currentTimeMillis(); // שמירת הזמן הנוכחי של הדיווח
    }

    // שיטות גישה ועדכון (Getters & Setters) לכל המשתנים
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSummaryId() { return summaryId; }
    public void setSummaryId(String summaryId) { this.summaryId = summaryId; }
    
    public String getSummaryTitle() { return summaryTitle; }
    public void setSummaryTitle(String summaryTitle) { this.summaryTitle = summaryTitle; }
    
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
