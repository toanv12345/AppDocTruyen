package com.example.appdoctruyen.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editEmail;
    private Button btnResetPassword;
    private Button btnBack;
    private ProgressBar progressBar;
    private TextView txtStatus;
    private FirebaseAuth mAuth;
    private static final String ADMIN_EMAIL = "admin@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ các view
        editEmail = findViewById(R.id.edit_email);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        btnBack = findViewById(R.id.btn_back);
        progressBar = findViewById(R.id.progress_bar);
        txtStatus = findViewById(R.id.txt_status);

        txtStatus.setText("Nhập email của bạn để đặt lại mật khẩu");

        // Xử lý nút đặt lại mật khẩu
        btnResetPassword.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            // Kiểm tra nếu email là tài khoản admin
            if (email.equalsIgnoreCase(ADMIN_EMAIL)) {
                // Đây là tài khoản admin, không cho phép đặt lại mật khẩu
                progressBar.setVisibility(View.GONE);
                txtStatus.setText("Tài khoản admin không thể đặt lại mật khẩu thông qua ứng dụng.");
                Toast.makeText(ForgotPasswordActivity.this,
                        "Tài khoản admin không thể đặt lại mật khẩu thông qua ứng dụng",
                        Toast.LENGTH_LONG).show();
            } else {
                // Không phải tài khoản admin, kiểm tra xác thực email
                checkEmailVerification(email);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    // Phương thức kiểm tra xác thực email
    private void checkEmailVerification(String email) {
        // Tìm người dùng theo email trong database
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        Query emailQuery = usersRef.orderByChild("email").equalTo(email);

        emailQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean isVerified = false;
                    String userId = null;

                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        userId = userSnapshot.getKey();
                        // Kiểm tra trạng thái xác thực email
                        if (userSnapshot.hasChild("emailVerified")) {
                            Boolean verified = userSnapshot.child("emailVerified").getValue(Boolean.class);
                            if (verified != null && verified) {
                                isVerified = true;
                                break;
                            }
                        }
                    }

                    if (isVerified) {
                        // Email đã được xác thực, tiến hành gửi email đặt lại mật khẩu
                        sendPasswordResetEmail(email);
                    } else {
                        // Email chưa được xác thực
                        progressBar.setVisibility(View.GONE);
                        txtStatus.setText("Email chưa được xác thực. Không thể gửi email đặt lại mật khẩu.");
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Bạn cần xác thực email trước khi đặt lại mật khẩu",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Không tìm thấy email trong hệ thống
                    progressBar.setVisibility(View.GONE);
                    txtStatus.setText("Không tìm thấy email trong hệ thống.");
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Email không tồn tại trong hệ thống",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ForgotPasswordActivity.this,
                        "Lỗi: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Phương thức gửi email đặt lại mật khẩu
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        txtStatus.setText("Đã gửi email đặt lại mật khẩu.\nVui lòng kiểm tra hộp thư của bạn.");
                        editEmail.setEnabled(false);
                        btnResetPassword.setEnabled(false);
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Email đặt lại mật khẩu đã được gửi!",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Không thể gửi email đặt lại mật khẩu.";
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}