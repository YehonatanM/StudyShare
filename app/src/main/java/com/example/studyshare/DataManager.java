package com.example.studyshare;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * מנהל הנתונים (DataManager) - מחלקת סינגלטון לניהול האינטגרציה עם Firebase.
 * מחלקה זו מרכזת את כל הפעולות מול Firebase Auth ו-Firestore.
 */
public class DataManager {
    private static DataManager instance; // מופע יחיד של המחלקה
    private final FirebaseAuth auth; // אובייקט האימות של Firebase
    private final FirebaseFirestore db; // אובייקט מסד הנתונים Firestore
    private ListenerRegistration currentUserListener; // רישום מאזין לשינויים בנתוני המשתמש הנוכחי
    // שורה 56 + 71 +184
    // כתובת האימייל המוגדרת כסופר אדמין
    public static final String SUPER_ADMIN_EMAIL = "yehevg12@gmail.com";
    
    // שמות האוספים (Collections) במסד הנתונים
    private static final String USERS_COLLECTION = "users";
    private static final String SUMMARIES_COLLECTION = "summaries";
    private static final String UPDATES_COLLECTION = "app_updates";
    private static final String METADATA_COLLECTION = "metadata";
    private static final String REPORTS_COLLECTION = "reports";
    private static final String ADMINS_COLLECTION = "admins";

    // רשימת המקצועות הזמינים במערכת
    public static final String[] SUBJECTS = {"Mathematics", "History", "Physics", "Literature", "Biology", "Computer Science", "English"};

    /**
     * ממשק עבור פעולות המצפות לתגובת הצלחה או כישלון ללא נתונים.
     */
    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * ממשק עבור פעולות המצפות לקבלת נתונים מסוג מסוים.
     */
    public interface DataCallback<T> {
        void onDataRetrieved(T data);
    }

    /**
     * בנאי פרטי למניעת יצירת מופעים חיצוניים (Singleton).
     */
    private DataManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * מחזירה את המופע היחיד של המחלקה. אם אינו קיים - יוצרת אותו.
     */
    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    /**
     * מנרמלת את כתובת האימייל על ידי הפיכתה לאותיות קטנות והסרת רווחים מיותרים.
     */
    private String normalize(String email) {
        return email != null ? email.toLowerCase().trim() : "";
    }

    /**
     * בודקת האם המשתמש המחובר כרגע הוא סופר אדמין לפי כתובת האימייל שלו.
     */
    public boolean isSuperAdmin() {
        String email = getCurrentUserEmail();
        return email != null && email.equalsIgnoreCase(SUPER_ADMIN_EMAIL);
    }

    /**
     * רושמת משתמש חדש במערכת, כולל יצירת חשבון ב-Auth ושמירת נתונים ב-Firestore תוך ניהול מונה מזהים.
     */
    public void registerUser(String name, String email, String password, String className, String dob, ActionCallback callback) {
        String mail = normalize(email);
        // שימוש בטרנזקציה כדי להבטיח קבלת מזהה (ID) ייחודי ורציף למשתמש
        db.runTransaction((Transaction.Function<Long>) transaction -> {
            DocumentSnapshot doc = transaction.get(db.collection(METADATA_COLLECTION).document("userCounter"));
            long nextId = (doc.exists() && doc.contains("count")) ? doc.getLong("count") + 1 : 1;
            Map<String, Object> map = new HashMap<>();
            map.put("count", nextId);
            transaction.set(db.collection(METADATA_COLLECTION).document("userCounter"), map, SetOptions.merge());
            return nextId;
        }).addOnSuccessListener(id -> {
            // יצירת המשתמש ב-Firebase Authentication
            auth.createUserWithEmailAndPassword(mail, password)
                .addOnSuccessListener(authResult -> {
                    User newUser = new User(mail, name, className, dob);
                    newUser.setId(id.intValue());
                    // שמירת פרטי המשתמש במסד הנתונים
                    db.collection(USERS_COLLECTION).document(mail).set(newUser)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * מבצעת התחברות של משתמש קיים, כולל בדיקה אם החשבון חסום וטיפול בכניסה של סופר אדמין.
     */
    public void loginUser(String email, String password, ActionCallback callback) {
        String mail = normalize(email);
        // טיפול מיוחד במקרה שהמשתמש הוא סופר אדמין
        if (mail.equalsIgnoreCase(SUPER_ADMIN_EMAIL)) {
            handleSuperAdminLogin(callback);
            return;
        }
        // שליפת נתוני המשתמש לבדיקת חסימה
        db.collection(USERS_COLLECTION).document(mail).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null && user.isBlocked()) {
                    callback.onFailure("Account blocked.");
                    return;
                }
                // התחברות באמצעות Firebase Auth
                auth.signInWithEmailAndPassword(mail, password)
                    .addOnSuccessListener(res -> {
                        setUserOnlineStatus(true);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onFailure("Invalid credentials."));
            } else {
                callback.onFailure("Account not found.");
            }
        });
    }

    /**
     * מטפלת בתהליך ההתחברות של סופר אדמין ועדכון הנתונים הרלוונטיים במסד הנתונים.
     */
    private void handleSuperAdminLogin(ActionCallback callback) {
        auth.signInWithEmailAndPassword(SUPER_ADMIN_EMAIL, "123456").addOnSuccessListener(authResult -> {
            String mail = normalize(SUPER_ADMIN_EMAIL);
            WriteBatch b = db.batch(); // שימוש ב-Batch לעדכון מספר מסמכים בו-זמנית
            Map<String, Object> data = new HashMap<>();
            data.put("email", mail);
            data.put("name", "Super Admin");
            data.put("admin", true);
            data.put("online", true);
            data.put("ID", 1);
            b.set(db.collection(USERS_COLLECTION).document(mail), data, SetOptions.merge());
            
            Map<String, Object> adm = new HashMap<>();
            adm.put("email", mail);
            adm.put("role", "Super Admin");
            adm.put("assignedAt", FieldValue.serverTimestamp());
            b.set(db.collection(ADMINS_COLLECTION).document(mail), adm, SetOptions.merge());
            
            b.commit().addOnCompleteListener(task -> callback.onSuccess());
        });
    }

    /**
     * מבצעת התנתקות של המשתמש מהמערכת ומעדכנת את סטטוס המחוברות שלו.
     */
    public void logout() {
        setUserOnlineStatus(false);
        stopListeningToCurrentUser();
        auth.signOut();
    }

    /**
     * מחזירה את כתובת האימייל של המשתמש המחובר כעת.
     */
    public String getCurrentUserEmail() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
    }

    /**
     * מתחילה האזנה בזמן אמת לשינויים במסמך של המשתמש המחובר כעת.
     */
    public void listenToCurrentUser(DataCallback<User> callback) {
        String e = getCurrentUserEmail();
        if (e == null) return;
        if (currentUserListener != null) currentUserListener.remove(); // הסרת האזנה קודמת אם קיימת
        currentUserListener = db.collection(USERS_COLLECTION).document(normalize(e))
            .addSnapshotListener((doc, error) -> {
                if (doc != null && doc.exists()) {
                    callback.onDataRetrieved(doc.toObject(User.class));
                }
            });
    }

    /**
     * מפסיקה את ההאזנה לנתוני המשתמש הנוכחי.
     */
    public void stopListeningToCurrentUser() {
        if (currentUserListener != null) {
            currentUserListener.remove();
            currentUserListener = null;
        }
    }

    /**
     * שולפת את נתוני המשתמש המחובר כעת מהשרת.
     */
    public void getCurrentUser(DataCallback<User> callback) {
        String e = getCurrentUserEmail();
        if (e == null) {
            callback.onDataRetrieved(null);
            return;
        }
        db.collection(USERS_COLLECTION).document(normalize(e)).get(Source.SERVER)
            .addOnSuccessListener(doc -> callback.onDataRetrieved(doc.toObject(User.class)))
            .addOnFailureListener(err -> getUserByEmail(e, callback));
    }

    /**
     * שולפת נתוני משתמש לפי כתובת אימייל.
     */
    public void getUserByEmail(String e, DataCallback<User> cb) {
        if (e == null || e.isEmpty()) {
            cb.onDataRetrieved(null);
            return;
        }
        db.collection(USERS_COLLECTION).document(normalize(e)).get()
            .addOnSuccessListener(doc -> cb.onDataRetrieved(doc.toObject(User.class)))
            .addOnFailureListener(err -> cb.onDataRetrieved(null));
    }

    /**
     * מחזירה רשימה של משתמשים מובילים לפי מספר הנקודות שלהם, מוגבל לפי הכמות המבוקשת.
     */
    public void getTopUsers(int limit, DataCallback<List<User>> cb) {
        db.collection(USERS_COLLECTION).orderBy("points", Query.Direction.DESCENDING).limit(limit).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(User.class)))
            .addOnFailureListener(e -> cb.onDataRetrieved(new ArrayList<>()));
    }

    /**
     * מעדכנת את פרטי הפרופיל של המשתמש (כיתה ותאריך לידה). סופר אדמין לא יכול לשנות כיתה.
     */
    public void updateFullProfile(String c, String d) {
        String e = normalize(getCurrentUserEmail());
        if (e.isEmpty()) return;
        Map<String, Object> u = new HashMap<>();
        if (!e.equalsIgnoreCase(SUPER_ADMIN_EMAIL)) u.put("className", c);
        u.put("dob", d);
        db.collection(USERS_COLLECTION).document(e).update(u);
    }

    /**
     * מעדכנת את שם המשתמש גם באוסף המשתמשים וגם בכל הסיכומים שהוא העלה.
     */
    public void updateUserName(String n) {
        String e = normalize(getCurrentUserEmail());
        if (e.isEmpty()) return;
        db.collection(USERS_COLLECTION).document(e).update("name", n);
        db.collection(SUMMARIES_COLLECTION).whereEqualTo("userId", e).get().addOnSuccessListener(docs -> {
            for (DocumentSnapshot d : docs.getDocuments()) {
                d.getReference().update("userName", n);
            }
        });
    }

    /**
     * מעדכנת את תמונת הפרופיל של המשתמש בכל המקומות הרלוונטיים במסד הנתונים.
     */
    public void updateUserImage(String i) {
        String e = normalize(getCurrentUserEmail());
        if (e.isEmpty()) return;
        db.collection(USERS_COLLECTION).document(e).update("profileImageUri", i);
        db.collection(SUMMARIES_COLLECTION).whereEqualTo("userId", e).get().addOnSuccessListener(docs -> {
            for (DocumentSnapshot d : docs.getDocuments()) {
                d.getReference().update("userProfileImageUri", i);
            }
        });
    }

    /**
     * מעדכנת האם המשתמש נמצא כרגע במצב מחובר (Online) או מנותק.
     */
    public void setUserOnlineStatus(boolean o) {
        String e = getCurrentUserEmail();
        if (e != null) {
            db.collection(USERS_COLLECTION).document(normalize(e)).update("online", o);
        }
    }

    /**
     * מוסיפה (או מחסירה) נקודות למשתמש לפי כתובת האימייל שלו.
     */
    public void addPoints(String e, int p) {
        if (e != null) {
            db.collection(USERS_COLLECTION).document(normalize(e)).update("points", FieldValue.increment(p));
        }
    }

    /**
     * מוסיפה או מסירה סיכום מרשימת הסיכומים השמורים של המשתמש.
     */
    public void toggleSaveSummary(String id, boolean s) {
        String e = getCurrentUserEmail();
        if (e != null) {
            db.collection(USERS_COLLECTION).document(normalize(e)).update("savedSummaries", s ? FieldValue.arrayUnion(id) : FieldValue.arrayRemove(id));
        }
    }

    /**
     * מעדכנת סטטוס מנהל למשתמש. רק סופר אדמין רשאי לבצע פעולה זו.
     */
    public void setUserAdminStatus(String e, boolean a, ActionCallback cb) {
        if (!isSuperAdmin()) {
            cb.onFailure("Denied.");
            return;
        }
        String t = normalize(e);
        WriteBatch b = db.batch();
        b.update(db.collection(USERS_COLLECTION).document(t), "admin", a);
        if (a) {
            Map<String, Object> adm = new HashMap<>();
            adm.put("email", t);
            adm.put("role", "Administrator");
            adm.put("assignedAt", FieldValue.serverTimestamp());
            b.set(db.collection(ADMINS_COLLECTION).document(t), adm, SetOptions.merge());
        } else {
            b.delete(db.collection(ADMINS_COLLECTION).document(t));
        }
        b.commit().addOnSuccessListener(v -> cb.onSuccess()).addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /**
     * חוסמת או מבטלת חסימה למשתמש. פעולה זו שמורה למנהלים בלבד.
     */
    public void toggleUserBlock(String e, boolean b, ActionCallback cb) {
        getCurrentUser(u -> {
            if (u != null && u.isAdmin()) {
                db.collection(USERS_COLLECTION).document(normalize(e)).update("blocked", b).addOnSuccessListener(v -> cb.onSuccess());
            }
        });
    }

    /**
     * מוחקת משתמש לצמיתות מהמערכת, כולל כל הסיכומים שהעלה ופרטי הניהול שלו.
     */
    public void deleteUserPermanently(String e, ActionCallback cb) {
        getCurrentUser(u -> {
            if (u != null && u.isAdmin()) {
                String t = normalize(e);
                WriteBatch b = db.batch();
                b.delete(db.collection(USERS_COLLECTION).document(t));
                b.delete(db.collection(ADMINS_COLLECTION).document(t));
                db.collection(SUMMARIES_COLLECTION).whereEqualTo("userId", t).get().addOnSuccessListener(docs -> {
                    for (DocumentSnapshot d : docs.getDocuments()) {
                        d.getReference().delete();
                    }
                    b.commit().addOnSuccessListener(v -> cb.onSuccess());
                });
            }
        });
    }

    /**
     * מוסיפה סיכום חדש לאוסף הסיכומים.
     */
    public void addSummary(Summary s, ActionCallback cb) {
        db.collection(SUMMARIES_COLLECTION).document(s.getId()).set(s)
            .addOnSuccessListener(v -> cb.onSuccess())
            .addOnFailureListener(e -> cb.onFailure(e.getMessage()));
    }

    /**
     * שולפת את כל הסיכומים ומסדרת אותם לפי כמות הלייקים (מהגבוה לנמוך).
     */
    public void getSummaries(DataCallback<List<Summary>> cb) {
        db.collection(SUMMARIES_COLLECTION).orderBy("likes", Query.Direction.DESCENDING).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(Summary.class)));
    }

    /**
     * שולפת את הסיכומים האחרונים שהועלו לפי זמן ההעלאה.
     */
    public void getLatestSummaries(DataCallback<List<Summary>> cb) {
        db.collection(SUMMARIES_COLLECTION).orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(Summary.class)))
            .addOnFailureListener(e -> cb.onDataRetrieved(new ArrayList<>()));
    }

    /**
     * שולפת את כל הסיכומים שהועלו על ידי משתמש ספציפי.
     */
    public void getSummariesByUser(String e, DataCallback<List<Summary>> cb) {
        db.collection(SUMMARIES_COLLECTION).whereEqualTo("userId", normalize(e)).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(Summary.class)))
            .addOnFailureListener(err -> cb.onDataRetrieved(new ArrayList<>()));
    }

    /**
     * שולפת רשימה של סיכומים ספציפיים לפי רשימת המזהים שלהם (עבור סיכומים שמורים).
     */
    public void getSavedSummaries(List<String> ids, DataCallback<List<Summary>> cb) {
        if (ids == null || ids.isEmpty()) {
            cb.onDataRetrieved(new ArrayList<>());
            return;
        }
        db.collection(SUMMARIES_COLLECTION).whereIn("id", ids).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(Summary.class)))
            .addOnFailureListener(err -> cb.onDataRetrieved(new ArrayList<>()));
    }

    /**
     * שולפת סיכום בודד לפי המזהה שלו.
     */
    public void getSummaryById(String id, DataCallback<Summary> cb) {
        db.collection(SUMMARIES_COLLECTION).document(id).get()
            .addOnSuccessListener(doc -> cb.onDataRetrieved(doc.toObject(Summary.class)))
            .addOnFailureListener(err -> cb.onDataRetrieved(null));
    }

    /**
     * מעדכנת את מספר הלייקים ורשימת המשתמשים שעשו לייק לסיכום, ומעדכנת נקודות לכותב הסיכום.
     */
    public void updateLikes(String id, String aid, String cur, boolean l) {
        String m = normalize(cur);
        db.collection(SUMMARIES_COLLECTION).document(id).update("likes", FieldValue.increment(l ? 1 : -1), "likedByUsers", l ? FieldValue.arrayUnion(m) : FieldValue.arrayRemove(m));
        addPoints(aid, l ? 1 : -1);
    }

    /**
     * מוחקת סיכום מהמערכת (פעולה למנהלים) ומורידה נקודות למשתמש שהעלה אותו.
     */
    public void deleteSummary(String id) {
        getCurrentUser(u -> {
            if (u != null && u.isAdmin()) {
                db.collection(SUMMARIES_COLLECTION).document(id).get().addOnSuccessListener(doc -> {
                    Summary s = doc.toObject(Summary.class);
                    if (s != null) {
                        String authEmail = s.getUserId();
                        db.collection(SUMMARIES_COLLECTION).document(id).delete().addOnSuccessListener(v -> addPoints(authEmail, -10));
                    }
                });
            }
        });
    }

    /**
     * מוסיפה תגובה לסיכום ומעדכנת את מונה התגובות.
     */
    public void addComment(String sid, Comment c, ActionCallback cb) {
        db.collection(SUMMARIES_COLLECTION).document(sid).update("comments", FieldValue.arrayUnion(c), "commentsCount", FieldValue.increment(1)).addOnSuccessListener(v -> cb.onSuccess());
    }

    /**
     * מוחקת תגובה מסיכום ומעדכנת את מונה התגובות.
     */
    public void deleteComment(String sid, Comment c, ActionCallback cb) {
        db.collection(SUMMARIES_COLLECTION).document(sid).update("comments", FieldValue.arrayRemove(c), "commentsCount", FieldValue.increment(-1)).addOnSuccessListener(v -> cb.onSuccess());
    }

    /**
     * מוסיפה דיווח (Report) חדש למערכת.
     */
    public void addReport(Report r, ActionCallback cb) {
        db.collection(REPORTS_COLLECTION).document(r.getId()).set(r).addOnSuccessListener(v -> cb.onSuccess());
    }

    /**
     * שולפת את כל הדיווחים ומסדרת אותם לפי זמן (מהחדש לישן).
     */
    public void getReports(DataCallback<List<Report>> cb) {
        db.collection(REPORTS_COLLECTION).orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(Report.class)))
            .addOnFailureListener(e -> cb.onDataRetrieved(new ArrayList<>()));
    }

    /**
     * מוחקת דיווח מהמערכת (פעולה למנהלים).
     */
    public void deleteReport(String id, ActionCallback cb) {
        getCurrentUser(u -> {
            if (u != null && u.isAdmin()) {
                db.collection(REPORTS_COLLECTION).document(id).delete().addOnSuccessListener(v -> cb.onSuccess());
            }
        });
    }

    /**
     * מוסיפה עדכון אפליקציה חדש. זמין למנהלים בלבד.
     */
    public void addAppUpdate(AppUpdate update, ActionCallback cb) {
        getCurrentUser(usr -> {
            if (usr != null && usr.isAdmin()) {
                db.collection(UPDATES_COLLECTION).document(update.getId()).set(update).addOnSuccessListener(v -> cb.onSuccess());
            }
        });
    }

    /**
     * שולפת את כל עדכוני האפליקציה מהחדש ביותר לישן ביותר.
     */
    public void getAppUpdates(DataCallback<List<AppUpdate>> cb) {
        db.collection(UPDATES_COLLECTION).orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener(docs -> cb.onDataRetrieved(docs.toObjects(AppUpdate.class)))
            .addOnFailureListener(e -> cb.onDataRetrieved(new ArrayList<>()));
    }

    /**
     * מוחקת עדכון אפליקציה מהמערכת (פעולה למנהלים).
     */
    public void deleteAppUpdate(String id, ActionCallback cb) {
        getCurrentUser(u -> {
            if (u != null && u.isAdmin()) {
                db.collection(UPDATES_COLLECTION).document(id).delete().addOnSuccessListener(v -> cb.onSuccess());
            }
        });
    }

    /**
     * מחלקת המייצגת משתמש במערכת.
     */
    public static class User {
        private String email, name, className, dob, profileImageUri;
        private boolean admin = false, blocked = false, online = false;
        private int points = 0;
        private List<String> savedSummaries = new ArrayList<>();
        @PropertyName("ID")
        private int idValue = 0;

        // בנאי ריק הנדרש עבור Firebase
        public User() {}

        public User(String e, String n, String c, String d) {
            this.email = e;
            this.name = n;
            this.className = c;
            this.dob = d;
            this.online = true;
        }

        // שיטות גישה ועדכון (Getters & Setters)
        public String getEmail() { return email; }
        public void setEmail(String e) { this.email = e; }
        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
        public String getClassName() { return className; }
        public void setClassName(String c) { this.className = c; }
        public String getDob() { return dob; }
        public void setDob(String d) { this.dob = d; }
        public String getProfileImageUri() { return profileImageUri; }
        public void setProfileImageUri(String u) { this.profileImageUri = u; }

        @PropertyName("admin")
        public boolean getAdmin() { return admin; }

        @PropertyName("admin")
        public void setAdmin(boolean a) { this.admin = a; }

        /**
         * בודקת אם המשתמש הוא אדמין או סופר אדמין.
         */
        @Exclude
        public boolean isAdmin() {
            return admin || (email != null && email.equalsIgnoreCase(SUPER_ADMIN_EMAIL));
        }

        public boolean isBlocked() { return blocked; }
        public void setBlocked(boolean b) { this.blocked = b; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean o) { this.online = o; }
        public int getPoints() { return points; }
        public void setPoints(int p) { this.points = p; }
        public List<String> getSavedSummaries() { return savedSummaries; }
        public void setSavedSummaries(List<String> s) { this.savedSummaries = s; }

        @PropertyName("ID")
        public int getId() { return idValue; }

        @PropertyName("ID")
        public void setId(int id) { this.idValue = id; }
    }

    /**
     * מחלקת המייצגת עדכון אפליקציה.
     */
    public static class AppUpdate {
        private String id, title, content;
        private long timestamp;

        public AppUpdate() {}

        public AppUpdate(String i, String t, String c) {
            this.id = i;
            this.title = t;
            this.content = c;
            this.timestamp = System.currentTimeMillis();
        }

        public String getId() { return id; }
        public void setId(String i) { this.id = i; }
        public String getTitle() { return title; }
        public void setTitle(String t) { this.title = t; }
        public String getContent() { return content; }
        public void setContent(String c) { this.content = c; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long t) { this.timestamp = t; }
    }
}
