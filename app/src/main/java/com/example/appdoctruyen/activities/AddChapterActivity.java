package com.example.appdoctruyen.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddChapterActivity extends AppCompatActivity {
    private EditText edtChapterTitle, edtChapterNgayup, edtChapterNoidung;
    private Button btnAddChapter;
    private DatabaseReference databaseReference;
    private String novelId;
    private int chapterCount = 0;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_chapter);

        edtChapterTitle = findViewById(R.id.edt_new_chapter_title);
        edtChapterNgayup = findViewById(R.id.edt_new_chapter_ngayup);
        edtChapterNoidung = findViewById(R.id.edt_new_chapter_noidung);
        btnAddChapter = findViewById(R.id.btnAddChapter);

        // Set current date as default
        updateDateLabel();

        novelId = getIntent().getStringExtra("novelId");
        if (novelId == null || novelId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID truyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("truyen").child(novelId).child("chapter");

        // Setup DatePicker for upload date
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        edtChapterNgayup.setOnClickListener(v -> {
            new DatePickerDialog(AddChapterActivity.this, dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Get the current count of chapters to generate the next key
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    if (key != null && key.startsWith("chap")) {
                        try {
                            int number = Integer.parseInt(key.substring(4));
                            if (number > chapterCount) {
                                chapterCount = number;
                            }
                        } catch (NumberFormatException e) {
                            // Bỏ qua nếu không phải số
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AddChapterActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnAddChapter.setOnClickListener(v -> {
            String title = edtChapterTitle.getText().toString().trim();
            String noidung = edtChapterNoidung.getText().toString().trim();
            String ngayup = edtChapterNgayup.getText().toString().trim();

            // Hiển thị dialog loading
            AlertDialog loadingDialog = showLoadingDialog();

            if (title.isEmpty()) {
                loadingDialog.dismiss();
                edtChapterTitle.setError("Vui lòng nhập tên chapter");
                return;
            }

            if (noidung.isEmpty()) {
                loadingDialog.dismiss();
                edtChapterNoidung.setError("Vui lòng nhập nội dung chapter");
                return;
            }

            String chapterId = "chap" + (chapterCount + 1);

            Map<String, Object> chapterData = new HashMap<>();
            chapterData.put("title", title);
            chapterData.put("noidung", noidung);
            chapterData.put("ngayup", ngayup);

            DatabaseReference chapterRef = databaseReference.child(chapterId);
            chapterRef.setValue(chapterData)
                    .addOnSuccessListener(aVoid -> {
                        loadingDialog.dismiss();
                        Toast.makeText(AddChapterActivity.this, "Thêm chapter thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        Toast.makeText(AddChapterActivity.this, "Lỗi khi thêm chapter: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateDateLabel() {
        String format = "yyyy/MM/dd";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        edtChapterNgayup.setText(sdf.format(calendar.getTime()));
    }

    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_loading);
        builder.setCancelable(false);
        return builder.show();
    }
}