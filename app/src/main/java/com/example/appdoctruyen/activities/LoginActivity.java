package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.utils.EmailVerificationChecker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextView txtSignUp;
    private TextView txtForgotPassword;
    private EditText editMail;
    private EditText editPass;
    private Button btnLogin;
    private Button btnCancel;
    private FirebaseAuth Auth;
    private ImageView account_Icon;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtSignUp = findViewById(R.id.signup_txt);
        txtForgotPassword = findViewById(R.id.forgot_password_txt);
        btnLogin = findViewById(R.id.login_btn);
        btnCancel = findViewById(R.id.cancel_button);
        editMail = findViewById(R.id.username_input);
        editPass = findViewById(R.id.password_input);
        Auth = FirebaseAuth.getInstance();

        txtSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        txtForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String email = editMail.getText().toString();
            String pass = editPass.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(LoginActivity.this, "Địa chỉ email không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "Attempting to log in with email: " + email);

            // thực hiện đăng nhập
            Auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful");
                            FirebaseUser user = Auth.getCurrentUser();
                            if (user != null) {
                                // Đầu tiên, kiểm tra xem người dùng có phải admin không
                                checkIfAdmin(user, isAdmin -> {
                                    if (isAdmin) {
                                        // Nếu là admin, không cần xác thực email
                                        checkAdminAndProceed(user);
                                    } else {
                                        // Nếu không phải admin, kiểm tra xác thực email
                                        EmailVerificationChecker.checkEmailVerificationStatus(new EmailVerificationChecker.VerificationCallback() {
                                            @Override
                                            public void onVerified() {
                                                // Đã xác thực email, tiếp tục đăng nhập bình thường
                                                checkAdminAndProceed(user);
                                            }

                                            @Override
                                            public void onNotVerified() {
                                                // Hiển thị thông báo yêu cầu xác thực email
                                                Toast.makeText(LoginActivity.this,
                                                        "Một số tính năng sẽ bị hạn chế cho đến khi bạn xác thực email.",
                                                        Toast.LENGTH_LONG).show();
                                                checkAdminAndProceed(user);
                                            }
                                        });
                                    }
                                });
                            }
                        } else {
                            Log.e(TAG, "Login failed: " + task.getException().getMessage());
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnCancel.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // Kiểm tra quyền admin và chuyển tới màn hình chính
    private void checkAdminAndProceed(FirebaseUser user) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isAdmin = snapshot.child("isAdmin").getValue(Boolean.class);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("isAdmin", isAdmin != null && isAdmin);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check admin status", error.toException());
                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkIfAdmin(FirebaseUser user, AdminCheckCallback callback) {
        if (user == null) {
            callback.onAdminCheck(false);
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(user.getUid());

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

    // Interface callback cho kiểm tra admin
    public interface AdminCheckCallback {
        void onAdminCheck(boolean isAdmin);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}