package com.example.appdoctruyen.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editEmail;
    private Button btnResetPassword;
    private Button btnBack;
    private ProgressBar progressBar;
    private TextView txtStatus;
    private FirebaseAuth mAuth;

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

            // Sử dụng Firebase để gửi email đặt lại mật khẩu
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
        });

        btnBack.setOnClickListener(v -> finish());
    }
}