package com.example.appdoctruyen.utils;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class EmailVerificationChecker {
    public interface AdminCheckCallback {
        void onAdminCheck(boolean isAdmin);
    }
    public interface VerificationCallback {
        void onVerified();
        void onNotVerified();
    }
    public static void isUserAdmin(String userId, AdminCheckCallback callback) {
        if (userId == null || callback == null) {
            if (callback != null) {
                callback.onAdminCheck(false);
            }
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);

        userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Boolean isAdmin = dataSnapshot.getValue(Boolean.class);
                callback.onAdminCheck(isAdmin != null && isAdmin);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onAdminCheck(false);
            }
        });
    }
    /**
     * Kiểm tra nếu người dùng cần xác thực email (không phải admin) hoặc đã xác thực
     * @param forceCheck Buộc kiểm tra ngay cả khi là admin
     */
    public static void checkVerificationRequired(boolean forceCheck, final VerificationCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (callback != null) {
                callback.onNotVerified();
            }
            return;
        }

        // Nếu email đã xác thực thì không cần kiểm tra admin
        if (currentUser.isEmailVerified()) {
            updateEmailVerificationStatus(currentUser);
            if (callback != null) {
                callback.onVerified();
            }
            return;
        }

        // Nếu forceCheck = true thì luôn kiểm tra xác thực mà không cần xem người dùng có phải admin không
        if (forceCheck) {
            currentUser.reload().addOnCompleteListener(task -> {
                FirebaseUser reloadedUser = FirebaseAuth.getInstance().getCurrentUser();
                if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                    updateEmailVerificationStatus(reloadedUser);
                    if (callback != null) {
                        callback.onVerified();
                    }
                } else {
                    if (callback != null) {
                        callback.onNotVerified();
                    }
                }
            });
            return;
        }

        // Kiểm tra xem người dùng có phải admin không
        isUserAdmin(currentUser.getUid(), isAdmin -> {
            if (isAdmin) {
                // Người dùng là admin, coi như đã xác thực
                if (callback != null) {
                    callback.onVerified();
                }
            } else {
                // Người dùng không phải admin, kiểm tra xác thực email
                currentUser.reload().addOnCompleteListener(task -> {
                    FirebaseUser reloadedUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (reloadedUser != null && reloadedUser.isEmailVerified()) {
                        updateEmailVerificationStatus(reloadedUser);
                        if (callback != null) {
                            callback.onVerified();
                        }
                    } else {
                        if (callback != null) {
                            callback.onNotVerified();
                        }
                    }
                });
            }
        });
    }
    /**
     * Tải lại thông tin người dùng và kiểm tra trạng thái xác thực email
     *
     * @param callback Interface callback để xử lý sau khi kiểm tra
     */
    public static void checkEmailVerificationStatus(final VerificationCallback callback) {
        checkVerificationRequired(true, callback);
    }

    /**
     * Hiển thị dialog thông báo xác thực email
     */
    public static void showVerificationDialog(Activity activity, boolean showDismissButton) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Xác thực email");
        builder.setMessage("Bạn cần xác thực email để sử dụng đầy đủ tính năng của ứng dụng. Vui lòng kiểm tra hộp thư email của bạn.");

        builder.setPositiveButton("Gửi lại email xác thực", (dialog, which) -> {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Đã gửi email")
                            .setMessage("Vui lòng kiểm tra hộp thư của bạn và nhấn vào liên kết xác thực.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        });

        builder.setNeutralButton("Tôi đã xác thực, kiểm tra lại", (dialog, which) -> {
            // Hiển thị trạng thái đang tải
            AlertDialog loadingDialog = new AlertDialog.Builder(activity)
                    .setTitle("Đang kiểm tra")
                    .setMessage("Vui lòng đợi...")
                    .setCancelable(false)
                    .show();

            // Kiểm tra lại trạng thái xác thực
            checkEmailVerificationStatus(new VerificationCallback() {
                @Override
                public void onVerified() {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(activity)
                            .setTitle("Xác thực thành công")
                            .setMessage("Email của bạn đã được xác thực!")
                            .setPositiveButton("OK", null)
                            .show();
                }

                @Override
                public void onNotVerified() {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(activity)
                            .setTitle("Chưa xác thực")
                            .setMessage("Email của bạn vẫn chưa được xác thực. Vui lòng kiểm tra hộp thư và nhấn vào liên kết xác thực.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        });

        if (showDismissButton) {
            builder.setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss());
        }

        builder.setCancelable(showDismissButton);
        builder.show();
    }

    /**
     * Cập nhật trạng thái xác thực email trong database
     */
    private static void updateEmailVerificationStatus(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.child("emailVerified").setValue(true);
    }
}