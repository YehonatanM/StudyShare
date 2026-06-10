package com.example.studyshare;

import com.google.firebase.firestore.PropertyName;
import java.util.ArrayList;
import java.util.List;

/**
 * סיכום - מחלקה זו מייצגת סיכום לימודי שהועלה לאפליקציה.
 * היא מכילה את כל המידע על הסיכום: המקצוע, התוכן, הלייקים, התגובות ופרטי המשתמש שהעלה אותו.
 */
public class Summary {
    private String id; // מזהה ייחודי לסיכום
    private String subject; // המקצוע אליו שייך הסיכום (למשל: מתמטיקה, היסטוריה)
    private String content; // תיאור קצר או תוכן הסיכום כפי שהוזן על ידי המשתמש
    private int likes; // מספר הלייקים הכולל שקיבל הסיכום
    private int commentsCount; // מונה התגובות שיש לסיכום זה
    private String userId; // כתובת האימייל של המשתמש שהעלה את הסיכום (משמש כמזהה)
    private String userName; // שם המשתמש שהעלה את הסיכום להצגה מהירה
    private String userProfileImageUri; // קישור לתמונת הפרופיל של המשתמש המעלה
    private int uploaderId; // המזהה המספרי (ID) של המשתמש שהעלה את הסיכום
    private String fileName; // שם הקובץ המקורי (למשל: summary.pdf)
    private String fileUri; // קישור (URI) לקובץ המאוחסן המאפשר גישה אליו
    private long timestamp; // הזמן שבו הועלה הסיכום (במילישניות)
    private List<String> likedByUsers = new ArrayList<>(); // רשימה של כתובות אימייל של משתמשים שנתנו לייק
    private List<Comment> comments = new ArrayList<>(); // רשימת אובייקטי התגובות שנכתבו על הסיכום

    /**
     * בנאי ריק הנדרש עבור Firebase Firestore כדי לשחזר את האובייקט מהמסמך.
     */
    public Summary() {}

    /**
     * יצירת סיכום חדש עם המידע ההתחלתי הנדרש.
     */
    public Summary(String id, String subject, String content, int likes, String userId, String userName, String userProfileImageUri, int uploaderId, String fileName, String fileUri) {
        this.id = id;
        this.subject = subject;
        this.content = content;
        this.likes = likes;
        this.userId = userId;
        this.userName = userName;
        this.userProfileImageUri = userProfileImageUri;
        this.uploaderId = uploaderId;
        this.fileName = fileName;
        this.fileUri = fileUri;
        this.timestamp = System.currentTimeMillis(); // קביעת זמן היצירה לזמן הנוכחי
        this.commentsCount = 0; // אתחול מונה התגובות ל-0
    }

    // שיטות גישה ועדכון (Getters & Setters) לכל שדות המחלקה

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    
    /**
     * מחזיר את מספר התגובות. אם המונה הוא אפס אך הרשימה אינה ריקה, מחזיר את גודל הרשימה.
     */
    public int getCommentsCount() { 
        if (comments != null && !comments.isEmpty() && commentsCount == 0) {
            return comments.size();
        }
        return commentsCount; 
    }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserProfileImageUri() { return userProfileImageUri; }
    public void setUserProfileImageUri(String userProfileImageUri) { this.userProfileImageUri = userProfileImageUri; }

    @PropertyName("uploaderId")
    public int getUploaderId() { return uploaderId; }
    @PropertyName("uploaderId")
    public void setUploaderId(int uploaderId) { this.uploaderId = uploaderId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getFileUri() { return fileUri; }
    public void setFileUri(String fileUri) { this.fileUri = fileUri; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public List<String> getLikedByUsers() { return likedByUsers; }
    public void setLikedByUsers(List<String> likedByUsers) { this.likedByUsers = likedByUsers; }
    
    public List<Comment> getComments() { return comments; }
    
    /**
     * מעדכן את רשימת התגובות ומסנכרן את המונה בהתאם לגודל הרשימה.
     */
    public void setComments(List<Comment> comments) { 
        this.comments = comments;
        if (comments != null) this.commentsCount = comments.size();
    }

    /**
     * הוספת תגובה חדשה לסיכום ועדכון המונה.
     */
    public void addComment(Comment comment) {
        if (comments == null) comments = new ArrayList<>();
        comments.add(comment);
        this.commentsCount = comments.size();
    }

    /**
     * בדיקה האם משתמש מסוים (לפי אימייל) כבר נתן לייק לסיכום זה.
     */
    public boolean isLikedByUser(String userEmail) {
        return likedByUsers != null && likedByUsers.contains(userEmail);
    }
}
