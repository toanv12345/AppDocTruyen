package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.activities.SignUpActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private Button btnSignUp1;
    private EditText editMail;
    private EditText editPass;
    private Button btnLogin;
    private FirebaseAuth Auth;
    private ImageView account_Icon;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        btnSignUp1 = findViewById(R.id.signup_btn);
        btnLogin = findViewById(R.id.login_btn);
        editMail = findViewById(R.id.username_input);
        editPass = findViewById(R.id.password_input);
        Auth = FirebaseAuth.getInstance();

        btnSignUp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
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
                        } else {
                            Log.e(TAG, "Login failed: " + task.getException().getMessage());
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
