package com.example.studyshare;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
// שורה 113,121
/**
 * מתאם סיכומים (SummaryAdapter) - מחלקה זו אחראית להצגת רשימת הסיכומים באפליקציה.
 * היא מחברת בין נתוני הסיכומים לבין העיצוב הגרפי שלהם ברשימה ומטפלת באינטראקציות כמו לייקים, שמירה וניווט לפרטים.
 */
public class SummaryAdapter extends RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder> {

    private final List<Summary> summaryList; // רשימת הסיכומים שיוצגו
    private final String currentUserEmail; // אימייל המשתמש המחובר לצורך בדיקת לייקים ושמירות
    private boolean isAdmin = false; // האם המשתמש הצופה הוא מנהל
    private String highlightId = null; // מזהה סיכום שצריך להדגיש (למשל לאחר חיפוש)
    private List<String> savedSummaryIds = new ArrayList<>(); // רשימת מזהי הסיכומים שהמשתמש שמר

    /**
     * בנאי למתאם הסיכומים.
     * @param summaryList רשימת הסיכומים להצגה
     */
    public SummaryAdapter(List<Summary> summaryList) {
        this.summaryList = summaryList;
        this.currentUserEmail = DataManager.getInstance().getCurrentUserEmail();
        refreshUserData(); // טעינת נתוני המשתמש הרלוונטיים לתצוגה
    }

    /**
     * מרענן את נתוני המשתמש (סטטוס ניהול וסיכומים שמורים) כדי לעדכן את הממשק.
     */
    private void refreshUserData() {
        DataManager.getInstance().getCurrentUser(user -> {
            if (user != null) {
                // המשתמש הוא מנהל אם הוא אדמין או סופר אדמין
                this.isAdmin = user.isAdmin() || DataManager.getInstance().isSuperAdmin();
                this.savedSummaryIds = user.getSavedSummaries() != null ? user.getSavedSummaries() : new ArrayList<>();
                notifyDataSetChanged(); // עדכון הרשימה כולה
            }
        });
    }

    /**
     * מגדיר מזהה סיכום להדגשה ויזואלית ברשימה.
     */
    public void setHighlightId(String id) {
        this.highlightId = id;
        notifyDataSetChanged();
    }

    /**
     * יוצר את ה-ViewHolder שמחזיק את העיצוב של סיכום בודד.
     */
    @NonNull
    @Override
    public SummaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_summary, parent, false);
        return new SummaryViewHolder(view);
    }

    /**
     * מחבר את נתוני הסיכום לרכיבי התצוגה ומגדיר את הלוגיקה של הכפתורים.
     */
    @Override
    public void onBindViewHolder(@NonNull SummaryViewHolder holder, int position) {
        Summary summary = summaryList.get(position);
        
        // החלת צבע רקע להדגשה במידה וזהו הסיכום המבוקש
        if (highlightId != null && highlightId.equals(summary.getId())) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFDE7"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // עדכון הטקסטים של הסיכום
        holder.tvSubject.setText(summary.getSubject());
        holder.tvTitle.setText(summary.getSubject() + " Summary");
        holder.tvContent.setText(summary.getContent());
        holder.tvLikes.setText(String.valueOf(summary.getLikes()));
        holder.tvCommentsCount.setText(String.valueOf(summary.getCommentsCount()));

        // הצגת שם המעלה ומידע נוסף למנהלים
        String userName = summary.getUserName() != null ? summary.getUserName() : "StudyShare User";
        if (isAdmin && summary.getUploaderId() > 0) {
            String idText = " (ID: " + summary.getUploaderId() + ")";
            String fullText = userName + idText;
            SpannableString ss = new SpannableString(fullText);
            int start = userName.length();
            // הקטנת ה-ID וצביעתו באפור עבור מנהלים
            ss.setSpan(new RelativeSizeSpan(0.75f), start, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(Color.GRAY), start, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.tvUser.setText(ss);
        } else {
            holder.tvUser.setText(userName);
        }
        
        // טעינת תמונת הפרופיל של מעלה הסיכום
        if (summary.getUserProfileImageUri() != null && !summary.getUserProfileImageUri().isEmpty()) {
            try {
                holder.ivUserIcon.setImageURI(Uri.parse(summary.getUserProfileImageUri()));
            } catch (Exception e) {
                holder.ivUserIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivUserIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // לחיצה על כרטיס הסיכום תפתח את מסך הפרטים המלא
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SummaryDetailActivity.class);
            intent.putExtra("summaryId", summary.getId());
            v.getContext().startActivity(intent);
        });

        // פעולת הורדה - פתיחת קובץ ה-PDF המשויך
        holder.btnDownload.setOnClickListener(v -> {
            if (summary.getFileUri() != null && !summary.getFileUri().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(summary.getFileUri()), "application/pdf");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                v.getContext().startActivity(intent);
            }
        });

        // לחיצה על כפתור התגובות תפתח את מסך השיחה
        holder.btnComments.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("summaryId", summary.getId());
            v.getContext().startActivity(intent);
        });

        // ניהול לייקים - עדכון השרת והממשק המקומי
        updateLikeUI(holder, summary);
        holder.btnLike.setOnClickListener(v -> {
            if (currentUserEmail == null) return;
            boolean isLiked = summary.isLikedByUser(currentUserEmail);
            
            if (isLiked) {
                summary.getLikedByUsers().remove(currentUserEmail);
                summary.setLikes(summary.getLikes() - 1);
            } else {
                summary.getLikedByUsers().add(currentUserEmail);
                summary.setLikes(summary.getLikes() + 1);
            }
            updateLikeUI(holder, summary);
            DataManager.getInstance().updateLikes(summary.getId(), summary.getUserId(), currentUserEmail, !isLiked);
        });

        // ניהול שמירה בסימניות (Saved Summaries)
        boolean isSaved = savedSummaryIds.contains(summary.getId());
        holder.btnSaveSummary.setImageResource(isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        holder.btnSaveSummary.setColorFilter(isSaved ? Color.parseColor("#7C3AED") : Color.GRAY);
        
        holder.btnSaveSummary.setOnClickListener(v -> {
            if (currentUserEmail == null) return;
            boolean nowSaved = !savedSummaryIds.contains(summary.getId());
            if (nowSaved) {
                savedSummaryIds.add(summary.getId());
                Toast.makeText(v.getContext(), "נשמר בסימניות", Toast.LENGTH_SHORT).show();
            } else {
                savedSummaryIds.remove(summary.getId());
                Toast.makeText(v.getContext(), "הוסר מהסימניות", Toast.LENGTH_SHORT).show();
            }
            DataManager.getInstance().toggleSaveSummary(summary.getId(), nowSaved);
            notifyItemChanged(position);
        });
    }

    /**
     * מעדכן את נראות כפתור הלייק (צבע ואייקון) בהתאם למצב הנוכחי.
     */
    private void updateLikeUI(SummaryViewHolder holder, Summary summary) {
        holder.tvLikes.setText(String.valueOf(summary.getLikes()));
        boolean isLiked = summary.isLikedByUser(currentUserEmail);
        int activeColor = Color.parseColor("#E91E63"); // צבע ורוד ללייק פעיל
        int inactiveColor = Color.GRAY;
        
        holder.ivLikeIcon.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
        holder.ivLikeIcon.setColorFilter(isLiked ? activeColor : inactiveColor);
        holder.tvLikes.setTextColor(isLiked ? activeColor : inactiveColor);
    }

    /**
     * מחזיר את כמות הסיכומים ברשימה.
     */
    @Override
    public int getItemCount() { return summaryList.size(); }

    /**
     * מחלקה פנימית המייצגת את רכיבי התצוגה של סיכום בודד.
     */
    public static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvContent, tvLikes, tvUser, tvCommentsCount;
        View btnLike, btnComments, btnDownload;
        ImageView ivUserIcon, ivLikeIcon, btnSaveSummary;

        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvSummaryTitle);
            tvSubject = itemView.findViewById(R.id.tvSummarySubject);
            tvContent = itemView.findViewById(R.id.tvSummaryContent);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvUser = itemView.findViewById(R.id.tvSummaryUser);
            tvCommentsCount = itemView.findViewById(R.id.tvCommentsCount);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComments = itemView.findViewById(R.id.btnComments);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            ivUserIcon = itemView.findViewById(R.id.ivUserIcon);
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            btnSaveSummary = itemView.findViewById(R.id.btnSaveSummary);
        }
    }
}
