package com.example.studyshare;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * מתאם למנהלים (AdminAdapter) - מחלקה זו אחראית לניהול והצגת רשימות בתוך מסך הניהול.
 * היא תומכת בהצגת משתמשים, סיכומים, עדכוני אפליקציה ודיווחים, ומאפשרת למנהל לבצע פעולות על כל פריט.
 */
public class AdminAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // הגדרת סוגי הפריטים השונים שהמתאם יכול להציג
    private static final int TYPE_USER = 1;
    private static final int TYPE_SUMMARY = 2;
    private static final int TYPE_UPDATE = 3;
    private static final int TYPE_REPORT = 4;

    private final List<Object> itemList; // רשימת הנתונים הגנרית (יכולה להכיל סוגים שונים)
    private final int currentType; // הסוג שנבחר להצגה כרגע
    private final boolean isSuperAdmin; // האם המשתמש המחובר הוא סופר אדמין

    /**
     * בנאי למתאם המנהלים.
     * @param itemList רשימת האובייקטים להצגה
     * @param type סוג המידע (משתמשים, סיכומים וכו')
     */
    public AdminAdapter(List<Object> itemList, int type) {
        this.itemList = itemList;
        this.currentType = type;
        this.isSuperAdmin = DataManager.getInstance().isSuperAdmin();
    }

    /**
     * מחזיר את סוג התצוגה של הפריט לפי המיקום שלו (במקרה זה, כל הרשימה היא מאותו סוג).
     */
    @Override
    public int getItemViewType(int position) {
        return currentType;
    }

    /**
     * יוצר את ה-ViewHolder המתאים לפי סוג הפריט.
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            return new UserViewHolder(inflater.inflate(R.layout.item_admin_user, parent, false));
        } else if (viewType == TYPE_SUMMARY) {
            return new SummaryViewHolder(inflater.inflate(R.layout.item_admin_summary, parent, false));
        } else if (viewType == TYPE_UPDATE) {
            return new UpdateViewHolder(inflater.inflate(R.layout.item_app_update, parent, false));
        } else {
            return new ReportViewHolder(inflater.inflate(R.layout.item_admin_report, parent, false));
        }
    }

    /**
     * מחבר את הנתונים ל-ViewHolder המתאים.
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof UserViewHolder) {
            bindUser((UserViewHolder) holder, (DataManager.User) itemList.get(position), position);
        } else if (holder instanceof SummaryViewHolder) {
            bindSummary((SummaryViewHolder) holder, (Summary) itemList.get(position), position);
        } else if (holder instanceof UpdateViewHolder) {
            bindUpdate((UpdateViewHolder) holder, (DataManager.AppUpdate) itemList.get(position), position);
        } else if (holder instanceof ReportViewHolder) {
            bindReport((ReportViewHolder) holder, (Report) itemList.get(position), position);
        }
    }

    /**
     * קישור נתוני משתמש וניהול פעולות מנהל (חסימה, מחיקה, הרשאות).
     */
    private void bindUser(UserViewHolder vh, DataManager.User user, int position) {
        vh.tvName.setText(user.getName() + " [ID: " + user.getId() + "]");
        vh.tvEmail.setText(user.getEmail());
        
        boolean isBlocked = user.isBlocked();
        boolean userIsAdmin = user.getAdmin(); 
        boolean isThisUserSuperAdmin = user.getEmail() != null && user.getEmail().equalsIgnoreCase(DataManager.SUPER_ADMIN_EMAIL);

        // הגדרת תצוגת הסטטוס (חסימה / מחובר / לא מחובר)
        vh.tvStatus.setText(isBlocked ? "Status: BLOCKED" : (user.isOnline() ? "Status: ACTIVE" : "Status: INACTIVE"));
        vh.tvStatus.setTextColor(isBlocked ? Color.RED : (user.isOnline() ? Color.parseColor("#10B981") : Color.GRAY));
        
        vh.btnBlock.setText(isBlocked ? "Unblock" : "Block");
        vh.tvAdminBadge.setVisibility(userIsAdmin || isThisUserSuperAdmin ? View.VISIBLE : View.GONE);

        // ניהול הרשאות אדמין - זמין רק עבור סופר אדמין
        if (isSuperAdmin && !isThisUserSuperAdmin) {
            vh.btnToggleAdmin.setVisibility(View.VISIBLE);
            vh.btnToggleAdmin.setText(userIsAdmin ? "Remove Admin" : "Make Admin");
            vh.btnToggleAdmin.setOnClickListener(v -> {
                DataManager.getInstance().setUserAdminStatus(user.getEmail(), !userIsAdmin, new DataManager.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        user.setAdmin(!userIsAdmin);
                        notifyItemChanged(position);
                    }
                    @Override
                    public void onFailure(String e) {
                        Toast.makeText(v.getContext(), e, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            vh.btnToggleAdmin.setVisibility(View.GONE);
        }

        // פעולת חסימה/ביטול חסימה של משתמש
        vh.btnBlock.setOnClickListener(v -> {
            DataManager.getInstance().toggleUserBlock(user.getEmail(), !isBlocked, new DataManager.ActionCallback() {
                @Override
                public void onSuccess() { 
                    user.setBlocked(!isBlocked);
                    notifyItemChanged(position); 
                }
                @Override
                public void onFailure(String e) { Toast.makeText(v.getContext(), e, Toast.LENGTH_SHORT).show(); }
            });
        });

        // מחיקת משתמש לצמיתות מהמערכת
        vh.btnDelete.setOnClickListener(v -> {
            if (isThisUserSuperAdmin) {
                Toast.makeText(v.getContext(), "Cannot delete סופר אדמין", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(v.getContext()).setTitle("Delete User").setMessage("Permanent delete?")
                    .setPositiveButton("Yes", (d, w) -> DataManager.getInstance().deleteUserPermanently(user.getEmail(), new DataManager.ActionCallback() {
                        @Override
                        public void onSuccess() { itemList.remove(position); notifyItemRemoved(position); }
                        @Override
                        public void onFailure(String e) {}
                    })).show();
        });

        // מעבר לצפייה בפרופיל המשתמש
        vh.btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userEmail", user.getEmail());
            v.getContext().startActivity(intent);
        });
    }

    /**
     * קישור נתוני סיכום וניהול אפשרות מחיקה על ידי מנהל.
     */
    private void bindSummary(SummaryViewHolder vh, Summary summary, int position) {
        vh.tvSubject.setText(summary.getSubject());
        vh.tvUser.setText("Uploaded by: " + (summary.getUserName() != null ? summary.getUserName() : "Unknown"));
        
        int likes = summary.getLikes();
        int comments = summary.getCommentsCount();
        vh.tvStats.setText(likes + " Likes • " + comments + " Comments");

        vh.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SummaryDetailActivity.class);
            intent.putExtra("summaryId", summary.getId());
            v.getContext().startActivity(intent);
        });
        
        // מחיקת סיכום מהמערכת על ידי מנהל
        vh.btnDelete.setOnClickListener(v -> {
            DataManager.getInstance().deleteSummary(summary.getId());
            itemList.remove(position);
            notifyItemRemoved(position);
        });
    }

    /**
     * קישור נתוני עדכון מערכת.
     */
    private void bindUpdate(UpdateViewHolder vh, DataManager.AppUpdate update, int position) {
        vh.tvTitle.setText(update.getTitle());
        vh.tvContent.setText(update.getContent());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        vh.tvDate.setText(sdf.format(new Date(update.getTimestamp())));

        // מחיקת עדכון בלחיצה ארוכה
        vh.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext()).setTitle("Delete Update").setMessage("Remove this update?")
                    .setPositiveButton("Delete", (d, w) -> DataManager.getInstance().deleteAppUpdate(update.getId(), new DataManager.ActionCallback() {
                        @Override
                        public void onSuccess() { itemList.remove(position); notifyItemRemoved(position); }
                        @Override
                        public void onFailure(String e) { Toast.makeText(v.getContext(), e, Toast.LENGTH_SHORT).show(); }
                    })).show();
            return true;
        });
    }

    /**
     * קישור נתוני דיווח וטיפול בו (מחיקת תוכן או התעלמות).
     */
    private void bindReport(ReportViewHolder vh, Report report, int position) {
        vh.tvTitle.setText(report.getSummaryTitle());
        vh.tvReason.setText("Reason: " + report.getReason());
        vh.tvReporter.setText("Reported by: " + report.getReporterEmail());

        vh.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SummaryDetailActivity.class);
            intent.putExtra("summaryId", report.getSummaryId());
            v.getContext().startActivity(intent);
        });

        // מחיקת הסיכום המדווח והדיווח עצמו
        vh.btnDeleteSummary.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext()).setTitle("Delete Reported Content")
                .setMessage("This will remove the summary and the report. Continue?")
                .setPositiveButton("Delete", (d, w) -> {
                    DataManager.getInstance().deleteSummary(report.getSummaryId());
                    DataManager.getInstance().deleteReport(report.getId(), new DataManager.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            itemList.remove(position);
                            notifyItemRemoved(position);
                            Toast.makeText(v.getContext(), "Content removed", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(String e) {}
                    });
                }).setNegativeButton("Cancel", null).show();
        });

        // הסרת הדיווח בלבד (התעלמות מהתלונה)
        vh.btnDismiss.setOnClickListener(v -> {
            DataManager.getInstance().deleteReport(report.getId(), new DataManager.ActionCallback() {
                @Override
                public void onSuccess() {
                    itemList.remove(position);
                    notifyItemRemoved(position);
                }
                @Override
                public void onFailure(String e) {}
            });
        });
    }

    @Override
    public int getItemCount() { return itemList.size(); }

    /**
     * ViewHolder עבור פריט משתמש ברשימת המנהל.
     */
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvStatus;
        MaterialButton btnBlock, btnDelete, btnProfile, btnToggleAdmin;
        View tvAdminBadge;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvAdminUserName);
            tvEmail = itemView.findViewById(R.id.tvAdminUserEmail);
            tvStatus = itemView.findViewById(R.id.tvUserStatus);
            btnBlock = itemView.findViewById(R.id.btnBlockUser);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            btnProfile = itemView.findViewById(R.id.btnViewUserProfile);
            btnToggleAdmin = itemView.findViewById(R.id.btnToggleAdmin);
            tvAdminBadge = itemView.findViewById(R.id.tvAdminBadge);
        }
    }

    /**
     * ViewHolder עבור פריט סיכום ברשימת המנהל.
     */
    static class SummaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvUser, tvStats;
        MaterialButton btnView, btnDelete;
        public SummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvAdminSummarySubject);
            tvUser = itemView.findViewById(R.id.tvAdminSummaryUser);
            tvStats = itemView.findViewById(R.id.tvAdminSummaryStats);
            btnView = itemView.findViewById(R.id.btnViewSummary);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteSummary);
        }
    }

    /**
     * ViewHolder עבור פריט עדכון ברשימת המנהל.
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

    /**
     * ViewHolder עבור פריט דיווח ברשימת המנהל.
     */
    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvReason, tvReporter;
        MaterialButton btnView, btnDeleteSummary, btnDismiss;
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReportSummaryTitle);
            tvReason = itemView.findViewById(R.id.tvReportReason);
            tvReporter = itemView.findViewById(R.id.tvReporterEmail);
            btnView = itemView.findViewById(R.id.btnViewReportedSummary);
            btnDeleteSummary = itemView.findViewById(R.id.btnAdminDeleteReportedSummary);
            btnDismiss = itemView.findViewById(R.id.btnDismissReport);
        }
    }
}
