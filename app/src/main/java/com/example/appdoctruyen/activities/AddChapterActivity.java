package com.example.appdoctruyen.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class AddChapterActivity extends AppCompatActivity {
    private EditText edtChapterTitle, edtChapterNgayup, edtChapterNoidung;
    private Button btnAddChapter;
    private DatabaseReference databaseReference;
    private String novelId;
    private int chapterCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_chapter);

        edtChapterTitle = findViewById(R.id.edt_new_chapter_title);
        edtChapterNgayup = findViewById(R.id.edt_new_chapter_ngayup);
        edtChapterNoidung = findViewById(R.id.edt_new_chapter_noidung);
        btnAddChapter = findViewById(R.id.btnAddChapter);

        novelId = getIntent().getStringExtra("novelId");
        databaseReference = FirebaseDatabase.getInstance().getReference("truyen").child(novelId).child("chapter");

        // Get the current count of chapters to generate the next key
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    if (key != null && key.startsWith("chap")) {
                        int number = Integer.parseInt(key.substring(4));
                        if (number > chapterCount) {
                            chapterCount = number;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });

        btnAddChapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = edtChapterTitle.getText().toString();
                String noidung = edtChapterNoidung.getText().toString();
                String ngayup = edtChapterNgayup.getText().toString();

                if (title.isEmpty() || noidung.isEmpty() || ngayup.isEmpty()) {
                    Toast.makeText(AddChapterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else {
                    String chapterId = "chap" + (chapterCount + 1);
                    DatabaseReference chapterRef = databaseReference.child(chapterId);
                    chapterRef.child("title").setValue(title);
                    chapterRef.child("noidung").setValue(noidung);
                    chapterRef.child("ngayup").setValue(ngayup)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AddChapterActivity.this, "Chapter added successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AddChapterActivity.this, "Failed to add chapter: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });
    }
}