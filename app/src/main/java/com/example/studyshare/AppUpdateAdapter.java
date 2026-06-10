package com.example.studyshare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * מתאם עדכוני אפליקציה (AppUpdateAdapter) - מחלקה זו אחראית להצגת רשימת העדכונים בממשק המשתמש.
 * היא מחברת בין נתוני העדכונים לבין העיצוב הגרפי של כל פריט ברשימה.
 */
public class AppUpdateAdapter extends RecyclerView.Adapter<AppUpdateAdapter.UpdateViewHolder> {

    private final List<DataManager.AppUpdate> updateList; // רשימת העדכונים להצגה
    private final boolean isAdmin; // משתנה המציין אם המשתמש הנוכחי הוא מנהל

    /**
     * בנאי למתאם העדכונים.
     * @param updateList רשימת העדכונים
     * @param isAdmin האם המשתמש מנהל
     */
    public AppUpdateAdapter(List<DataManager.AppUpdate> updateList, boolean isAdmin) {
        this.updateList = updateList;
        this.isAdmin = isAdmin;
    }

    /**
     * יוצר את ה-ViewHolder שמחזיק את העיצוב של עדכון בודד.
     */
    @NonNull
    @Override
    public UpdateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_update, parent, false);
        return new UpdateViewHolder(view);
    }

    /**
     * מחבר את נתוני העדכון (כותרת, תוכן ותאריך) לרכיבי התצוגה.
     */
    @Override
    public void onBindViewHolder(@NonNull UpdateViewHolder holder, int position) {
        DataManager.AppUpdate update = updateList.get(position);
        holder.tvTitle.setText(update.getTitle());
        holder.tvContent.setText(update.getContent());
        
        // עיצוב התאריך והזמן לתצוגה קריאה בעברית/אנגלית
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(update.getTimestamp())));

        // אם המשתמש הוא מנהל, ניתן להוסיף פעולות מיוחדות בלחיצה ארוכה
        if (isAdmin) {
            holder.itemView.setOnLongClickListener(v -> {
                // ניתן להוסיף כאן לוגיקה למחיקה או עריכה על ידי מנהל
                return false;
            });
        }
    }

    /**
     * מחזיר את כמות העדכונים ברשימה.
     */
    @Override
    public int getItemCount() {
        return updateList.size();
    }

    /**
     * מחלקה פנימית המייצגת את רכיבי התצוגה של עדכון בודד.
     */
    static class UpdateViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvContent;
        public UpdateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvUpdateTitle);
            tvDate = itemView.findViewById(R.id.tvUpdateDate);
            tvContent = itemView.findViewById(R.id.tvUpdateContent);
        }
    }
}
