package com.example.studyshare;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * מסך חיפוש (SearchActivity) - מאפשר למשתמשים למצוא סיכומים לפי מקצוע או לפי מילות חיפוש חופשיות.
 * המסך תומך בסינון בזמן אמת, כך שכל שינוי בטקסט החיפוש או בבחירת המקצוע מעדכן מיד את התוצאות המוצגות.
 */
public class SearchActivity extends AppCompatActivity {

    private RecyclerView rvSummaries; // רכיב המציג את רשימת הסיכומים שנמצאו
    private SummaryAdapter adapter; // המתאם שמחבר את נתוני הסיכומים לרשימה
    private List<Summary> summaryList; // רשימת הסיכומים הנוכחית שמוצגת לאחר סינון
    private List<Summary> allSummariesCache = new ArrayList<>(); // עותק מקומי של כל הסיכומים לביצוע חיפוש מהיר ללא פנייה נוספת לשרת
    private AutoCompleteTextView spinnerFilterSubject; // שדה בחירת מקצוע לסינון (Dropdown)
    private EditText etSearchQuery; // תיבת טקסט להזנת מילות חיפוש חופשיות
    
    // רשימת המקצועות הזמינים לסינון במערכת
    private final String[] subjects = {"All Subjects", "Mathematics", "History", "Physics", "Literature", "Biology", "Computer Science", "English"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initUI(); // אתחול רכיבי הממשק הגרפי
        setupDropdown(); // הגדרת רשימת בחירת המקצועות
        
        // בדיקה האם המסך נפתח עם בקשה לסינון מקצוע ספציפי מראש
        String initialSubject = getIntent().getStringExtra("filterSubject");
        if (initialSubject != null) {
            spinnerFilterSubject.setText(initialSubject, false);
        }

        setupSearchListeners(); // הגדרת מאזינים לשינויים בחיפוש
        loadAllSummaries(); // טעינת כלל הסיכומים מהשרת לצורך חיפוש מקומי מהיר
    }

    /**
     * מקשר בין המשתנים בקוד לרכיבים הגרפיים המוגדרים בקובץ ה-XML.
     */
    private void initUI() {
        rvSummaries = findViewById(R.id.rvSummaries);
        spinnerFilterSubject = findViewById(R.id.spinnerFilterSubject);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        
        rvSummaries.setLayoutManager(new LinearLayoutManager(this));
        summaryList = new ArrayList<>();
        adapter = new SummaryAdapter(summaryList);
        rvSummaries.setAdapter(adapter);
    }

    /**
     * מגדיר את רשימת הבחירה (Dropdown) של המקצועות ומגדיר ערך ברירת מחדל.
     */
    private void setupDropdown() {
        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, subjects);
        spinnerFilterSubject.setAdapter(subjectsAdapter);
        if (spinnerFilterSubject.getText().toString().isEmpty()) {
            spinnerFilterSubject.setText(subjects[0], false);
        }

        // ביצוע סינון מחדש בכל פעם שהמשתמש בוחר מקצוע אחר מהרשימה
        spinnerFilterSubject.setOnItemClickListener((parent, view, position, id) -> applyFilters());
    }

    /**
     * מגדיר מאזין לתיבת החיפוש שמעדכן את התוצאות בכל פעם שהמשתמש מקליד תו חדש.
     */
    private void setupSearchListeners() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                applyFilters(); // עדכון הסינון בזמן אמת
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * טוען את כל הסיכומים הקיימים בשרת ושומר אותם בזיכרון (Cache) לשיפור מהירות החיפוש.
     */
    private void loadAllSummaries() {
        DataManager.getInstance().getSummaries(allSummaries -> {
            if (allSummaries != null) {
                allSummariesCache = allSummaries;
            } else {
                allSummariesCache = new ArrayList<>();
            }
            applyFilters(); // החלת הסינונים על הנתונים שנטענו
            handleIntents(); // טיפול במעברים למסך עם פרמטרים ספציפיים
        });
    }

    /**
     * מבצע את הסינון בפועל על רשימת הסיכומים לפי המקצוע שנבחר וטקסט החיפוש שהוזן.
     */
    private void applyFilters() {
        String query = etSearchQuery.getText().toString().toLowerCase().trim();
        String subjectFilter = spinnerFilterSubject.getText().toString();

        summaryList.clear();
        if (allSummariesCache != null) {
            for (Summary s : allSummariesCache) {
                // בדיקה אם הסיכום מתאים למקצוע שנבחר (או אם נבחר "הכל")
                boolean matchesSubject = subjectFilter.equals("All Subjects") || s.getSubject().equalsIgnoreCase(subjectFilter);
                
                // בדיקה אם טקסט החיפוש מופיע בשם המקצוע, בתוכן הסיכום או בשם המשתמש שהעלה אותו
                boolean matchesQuery = query.isEmpty() || 
                                     s.getSubject().toLowerCase().contains(query) || 
                                     s.getContent().toLowerCase().contains(query) ||
                                     (s.getUserName() != null && s.getUserName().toLowerCase().contains(query));

                if (matchesSubject && matchesQuery) {
                    summaryList.add(s);
                }
            }
        }
        adapter.notifyDataSetChanged(); // רענון הרשימה המוצגת במסך
    }

    /**
     * מטפל במקרים שבהם נשלחנו למסך כדי להדגיש סיכום מסוים (למשל מתוך התראה).
     */
    private void handleIntents() {
        String highlightId = getIntent().getStringExtra("highlightSummaryId");
        if (highlightId == null) return;

        // בדיקה אם הסיכום המבוקש נמצא תחת המסננים הנוכחיים
        boolean found = false;
        for (Summary s : summaryList) {
            if (s.getId().equals(highlightId)) {
                found = true;
                break;
            }
        }

        // אם לא נמצא תחת המסנן, מאפס את המסננים כדי שיופיע
        if (!found) {
            spinnerFilterSubject.setText(subjects[0], false);
            etSearchQuery.setText("");
            applyFilters();
        }

        // גלילה למיקום הסיכום והדגשתו ויזואלית
        for (int i = 0; i < summaryList.size(); i++) {
            if (summaryList.get(i).getId().equals(highlightId)) {
                final int pos = i;
                rvSummaries.post(() -> {
                    rvSummaries.scrollToPosition(pos);
                    adapter.setHighlightId(highlightId);
                });
                break;
            }
        }
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
