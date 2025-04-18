package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText editMail;
    private EditText editPass;
    private FirebaseAuth Auth;
    private FirebaseFirestore db;
    private DatabaseReference dbRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Button btnSignUp;
        Button btnCancel;

        editMail = findViewById(R.id.username_sigup);
        editPass = findViewById(R.id.password_sigup);
        btnSignUp = findViewById(R.id.signup_btn_2);
        btnCancel = findViewById(R.id.cancel_signup_btn);

        Auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        btnSignUp.setOnClickListener(v -> {
            String email = editMail.getText().toString();
            String pass = editPass.getText().toString();

            // Kiểm tra thông tin đăng ký
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            Auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    FirebaseUser currentUser = Auth.getCurrentUser();
                    if (currentUser != null) {
                        // Gửi email xác thực
                        currentUser.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                            if (verifyTask.isSuccessful()) {
                                // Lưu thông tin người dùng vào Firestore
                                Map<String, Object> user = new HashMap<>();
                                user.put("email", email);
                                user.put("favorite", new ArrayList<String>());

                                db.collection("users").document(currentUser.getUid())
                                        .set(user)
                                        .addOnSuccessListener(aVoid -> {
                                            // Lưu thông tin người dùng vào Realtime Database
                                            DatabaseReference userRef = dbRef.child("users").child(currentUser.getUid());
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("email", email);
                                            userData.put("isAdmin", false); // Mặc định không phải admin
                                            userData.put("emailVerified", false); // Thêm trạng thái xác thực email

                                            userRef.setValue(userData).addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    // Hiển thị thông báo nhưng vẫn giữ phiên đăng nhập hiện tại
                                                    showVerificationDialog(email);
                                                } else {
                                                    Toast.makeText(SignUpActivity.this, "Lỗi khi lưu thông tin người dùng!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(SignUpActivity.this, "Lỗi khi lưu thông tin người dùng!", Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                Toast.makeText(SignUpActivity.this, "Không thể gửi email xác thực.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Lỗi khi lấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại!";
                    Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnCancel.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Hiển thị hộp thoại thông báo yêu cầu xác thực email
    private void showVerificationDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Xác thực email")
                .setMessage("Chúng tôi đã gửi một email xác thực đến " + email + ". Vui lòng kiểm tra hộp thư và xác thực email của bạn để sử dụng đầy đủ tính năng của ứng dụng.")
                .setPositiveButton("Tiếp tục", (dialog, which) -> {
                    // Người dùng đã đăng nhập, chuyển thẳng đến trang chính
                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}