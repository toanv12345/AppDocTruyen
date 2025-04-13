package com.example.appdoctruyen.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editCurrentPassword;
    private EditText editNewPassword;
    private EditText editConfirmPassword;
    private Button btnChangePassword;
    private Button btnCancel;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editCurrentPassword = findViewById(R.id.edit_current_password);
        editNewPassword = findViewById(R.id.edit_new_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnCancel = findViewById(R.id.btn_cancel);
        progressBar = findViewById(R.id.progress_bar);

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentPassword = editCurrentPassword.getText().toString().trim();
                String newPassword = editNewPassword.getText().toString().trim();
                String confirmPassword = editConfirmPassword.getText().toString().trim();

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(ChangePasswordActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(ChangePasswordActivity.this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPassword.length() < 6) {
                    Toast.makeText(ChangePasswordActivity.this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                changePassword(currentPassword, newPassword);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void changePassword(String currentPassword, String newPassword) {
        // Xác thực lại người dùng trước khi đổi mật khẩu
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Xác thực thành công, đổi mật khẩu
                            currentUser.updatePassword(newPassword)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            progressBar.setVisibility(View.GONE);
                                            if (task.isSuccessful()) {
                                                Toast.makeText(ChangePasswordActivity.this,
                                                        "Đổi mật khẩu thành công",
                                                        Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(ChangePasswordActivity.this,
                                                        "Đổi mật khẩu thất bại: " + task.getException().getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Mật khẩu hiện tại không đúng",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}