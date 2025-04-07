// AddNovelActivity.java
package com.example.appdoctruyen.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.appdoctruyen.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AddNovelActivity extends Activity {
    private EditText edtNewNovelImage, edtNewNovelTentruyen, edtNewNovelTacgia, edtNewNovelTheloai, edtNewNovelTomtat;
    private Button btnAddNovel;
    private DatabaseReference databaseReference;
    private int novelCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_novel);

        edtNewNovelImage = findViewById(R.id.edt_new_novel_image);
        edtNewNovelTentruyen = findViewById(R.id.edt_new_novel_tentruyen);
        edtNewNovelTacgia = findViewById(R.id.edt_new_novel_tacgia);
        edtNewNovelTheloai = findViewById(R.id.edt_new_novel_theloai);
        edtNewNovelTomtat = findViewById(R.id.edt_new_novel_tomtat);
        btnAddNovel = findViewById(R.id.btnAddNovel);

        // Initialize Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("truyen");

        // Get the current count of novels to generate the next key
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    if (key != null && key.startsWith("truyen")) {
                        int number = Integer.parseInt(key.substring(6));
                        if (number > novelCount) {
                            novelCount = number;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });

        btnAddNovel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String linkanh = edtNewNovelImage.getText().toString();
                String tentruyen = edtNewNovelTentruyen.getText().toString();
                String tacgia = edtNewNovelTacgia.getText().toString();
                String theloai = edtNewNovelTheloai.getText().toString();
                String tomtat = edtNewNovelTomtat.getText().toString();

                if (linkanh.isEmpty() || tentruyen.isEmpty() || tacgia.isEmpty() || theloai.isEmpty() || tomtat.isEmpty()) {
                    Toast.makeText(AddNovelActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else {
                    String novelId = "truyen" + (novelCount + 1);
                    DatabaseReference novelRef = databaseReference.child(novelId);
                    novelRef.child("linkanh").setValue(linkanh);
                    novelRef.child("tentruyen").setValue(tentruyen);
                    novelRef.child("tacgia").setValue(tacgia);
                    novelRef.child("theloai").setValue(theloai);
                    novelRef.child("tomtat").setValue(tomtat);
                    novelRef.child("chapter").setValue(null); // Create an empty chapter node
                    Toast.makeText(AddNovelActivity.this, "Novel added successfully", Toast.LENGTH_SHORT).show();

                    // Return to MainActivity
                    Intent intent = new Intent(AddNovelActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }
}