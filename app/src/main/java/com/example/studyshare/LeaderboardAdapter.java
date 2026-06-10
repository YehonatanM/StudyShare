package com.example.studyshare;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

/**
 * מתאם לוח מובילים (LeaderboardAdapter) - מחלקה זו אחראית להצגת רשימת המשתמשים המצטיינים במערכת.
 * היא מחברת בין נתוני המשתמשים (שם, נקודות, כיתה) לבין העיצוב הגרפי שלהם בטבלת המובילים.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private final List<DataManager.User> userList; // רשימת המשתמשים המדורגים להצגה

    /**
     * בנאי למתאם לוח המובילים.
     * @param userList רשימת המשתמשים שיוצגו
     */
    public LeaderboardAdapter(List<DataManager.User> userList) {
        this.userList = userList;
    }

    /**
     * יוצר את ה-ViewHolder שמחזיק את העיצוב של שורה בודדת בלוח המובילים.
     */
    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_user, parent, false);
        return new LeaderboardViewHolder(view);
    }

    /**
     * מחבר את נתוני המשתמש (מיקום, שם, כיתה ונקודות) לרכיבי התצוגה המתאימים.
     */
    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        DataManager.User user = userList.get(position);
        
        // הצגת הדירוג (המיקום ברשימה מתחיל מ-1)
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(user.getName());
        holder.tvClass.setText(user.getClassName());
        holder.tvPoints.setText(String.valueOf(user.getPoints()));

        // טעינת תמונת הפרופיל של המשתמש המוביל
        if (user.getProfileImageUri() != null && !user.getProfileImageUri().isEmpty()) {
            try {
                holder.ivUserIcon.setImageURI(Uri.parse(user.getProfileImageUri()));
            } catch (Exception e) {
                holder.ivUserIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivUserIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        // הדגשה ויזואלית של שלושת המקומות הראשונים בצבעים המסמלים מדליות
        if (position == 0) holder.tvRank.setTextColor(android.graphics.Color.parseColor("#FFD700")); // זהב
        else if (position == 1) holder.tvRank.setTextColor(android.graphics.Color.parseColor("#C0C0C0")); // כסף
        else if (position == 2) holder.tvRank.setTextColor(android.graphics.Color.parseColor("#CD7F32")); // ברונזה
        else holder.tvRank.setTextColor(android.graphics.Color.GRAY); // אפור לשאר המקומות
    }

    /**
     * מחזיר את כמות המשתמשים ברשימה.
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * מחלקה פנימית המייצגת את רכיבי התצוגה של שורה בלוח המובילים.
     */
    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvClass, tvPoints;
        ShapeableImageView ivUserIcon;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvName = itemView.findViewById(R.id.tvLeaderboardUserName);
            tvClass = itemView.findViewById(R.id.tvLeaderboardUserClass);
            tvPoints = itemView.findViewById(R.id.tvLeaderboardPoints);
            ivUserIcon = itemView.findViewById(R.id.ivLeaderboardUserIcon);
        }
    }
}
