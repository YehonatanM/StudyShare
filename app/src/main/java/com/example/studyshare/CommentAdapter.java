package com.example.studyshare;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * מתאם תגובות (CommentAdapter) - מחלקה זו אחראית לניהול והצגת רשימת התגובות על סיכום מסוים.
 * המתאם מחבר בין נתוני התגובות לבין העיצוב הגרפי שלהן ברשימה ומאפשר פעולות מחיקה למורשים.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList; // רשימת אובייקטי התגובות להצגה
    private String summaryId; // המזהה של הסיכום אליו שייכות התגובות
    private String currentUserEmail; // כתובת האימייל של המשתמש המחובר כרגע

    /**
     * בנאי למתאם התגובות.
     * @param commentList רשימת התגובות
     * @param summaryId מזהה הסיכום
     */
    public CommentAdapter(List<Comment> commentList, String summaryId) {
        this.commentList = commentList;
        this.summaryId = summaryId;
        this.currentUserEmail = DataManager.getInstance().getCurrentUserEmail();
    }

    /**
     * יוצר את ה-ViewHolder שמחזיק את העיצוב של תגובה בודדת.
     */
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    /**
     * מחבר את נתוני התגובה לרכיבי התצוגה ומגדיר את אפשרות המחיקה.
     */
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        
        // הצגת שם הכותב ותוכן התגובה
        holder.tvCommentUser.setText(comment.getUserName());
        holder.tvCommentText.setText(comment.getText());

        // הגדרת מאזין ללחיצה ארוכה לצורך מחיקת תגובה
        holder.itemView.setOnLongClickListener(v -> {
            // אם המשתמש הוא כותב התגובה, הוא רשאי למחוק אותה
            if (comment.getUserEmail() != null && comment.getUserEmail().equals(currentUserEmail)) {
                showDeleteDialog(v, comment, position);
                return true;
            }
            
            // גם מנהלים (כולל סופר אדמין) רשאים למחוק תגובות של אחרים
            DataManager.getInstance().getCurrentUser(user -> {
                if (user != null && user.isAdmin()) {
                    showDeleteDialog(v, comment, position);
                }
            });
            
            return false;
        });
    }

    /**
     * מציג חלונית אישור לפני ביצוע מחיקה של תגובה.
     */
    private void showDeleteDialog(View v, Comment comment, int position) {
        new AlertDialog.Builder(v.getContext())
                .setTitle("מחיקת תגובה")
                .setMessage("האם אתה בטוח שברצונך למחוק את התגובה?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    // קריאה ל-DataManager לביצוע המחיקה מבסיס הנתונים
                    DataManager.getInstance().deleteComment(summaryId, comment, new DataManager.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            // הסרה מהרשימה המקומית ועדכון התצוגה במקרה של הצלחה
                            commentList.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(v.getContext(), "התגובה נמחקה", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(v.getContext(), "שגיאה: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * מחזיר את כמות התגובות ברשימה.
     */
    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    /**
     * מחלקה פנימית המייצגת את רכיבי התצוגה של תגובה בודדת.
     */
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentUser, tvCommentText;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentUser = itemView.findViewById(R.id.tvCommentUser);
            tvCommentText = itemView.findViewById(R.id.tvCommentText);
        }
    }
}
