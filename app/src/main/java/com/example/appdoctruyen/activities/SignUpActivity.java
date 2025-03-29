package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText editMail;
    private EditText editPass;

    private FirebaseAuth Auth;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sigup);
        Button btnSignUp;
        editMail = findViewById(R.id.username_sigup);
        editPass = findViewById(R.id.password_sigup);
        btnSignUp = findViewById(R.id.signup_btn_2);
        Auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSignUp.setOnClickListener(v -> {
            String email = editMail.getText().toString();
            String pass = editPass.getText().toString();

            Auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                    FirebaseUser currentUser = Auth.getCurrentUser();
                    if (currentUser != null) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        db.collection("users").document(currentUser.getUid())
                                .set(user)
                                .addOnSuccessListener(aVoid -> {

                                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "Lỗi khi lưu thông tin người dùng!", Toast.LENGTH_SHORT).show();
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

    }
}
