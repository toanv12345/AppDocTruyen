package com.example.appdoctruyen.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditChapterActivity extends AppCompatActivity {

    private EditText edtTitle, edtContent, edtUploadDate;
    private Button btnUpdateChapter, btnDeleteChapter;

    private String novelId;
    private String chapterId;
    private Calendar calendar = Calendar.getInstance();

    private DatabaseReference chapterRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_chapter);

        // Thiết lập ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sửa Chapter");
        }

        // Khởi tạo views
        edtTitle = findViewById(R.id.edtTitleChapter);
        edtContent = findViewById(R.id.edtChapterContent);
        edtUploadDate = findViewById(R.id.edtUploadDate);
        btnUpdateChapter = findViewById(R.id.btnUpdateChapter);
        btnDeleteChapter = findViewById(R.id.btnDeleteChapter);

        // Lấy dữ liệu từ intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("novelId") && intent.hasExtra("chapterId")) {
            novelId = intent.getStringExtra("novelId");
            chapterId = intent.getStringExtra("chapterId");

            // Khởi tạo Firebase
            chapterRef = FirebaseDatabase.getInstance().getReference("truyen")
                    .child(novelId).child("chapter").child(chapterId);

            loadChapterData();
        } else {
            Toast.makeText(this, "Không thể tìm thấy thông tin chapter", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Thiết lập date picker cho ngày upload
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        edtUploadDate.setOnClickListener(v -> {
            new DatePickerDialog(EditChapterActivity.this, dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Thiết lập click listeners
        btnUpdateChapter.setOnClickListener(v -> updateChapter());
        btnDeleteChapter.setOnClickListener(v -> confirmDeleteChapter());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateDateLabel() {
        String format = "M/d/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        edtUploadDate.setText(sdf.format(calendar.getTime()));
    }

    private void loadChapterData() {
        chapterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String title = dataSnapshot.child("title").getValue(String.class);
                    String content = dataSnapshot.child("noidung").getValue(String.class);
                    String uploadDate = dataSnapshot.child("ngayup").getValue(String.class);

                    edtTitle.setText(title != null ? title : "");
                    edtContent.setText(content != null ? content : "");
                    edtUploadDate.setText(uploadDate != null ? uploadDate : "");

                    // Đặt ngày cho calendar nếu có ngày upload
                    if (uploadDate != null && !uploadDate.isEmpty()) {
                        SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy", Locale.US);
                        try {
                            Date date = format.parse(uploadDate);
                            if (date != null) {
                                calendar.setTime(date);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EditChapterActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateChapter() {
        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();
        String uploadDate = edtUploadDate.getText().toString().trim();

        if (title.isEmpty()) {
            edtTitle.setError("Vui lòng nhập tên chapter");
            return;
        }

        // Hiển thị loading dialog
        AlertDialog loadingDialog = showLoadingDialog();

        // Chuẩn bị dữ liệu cập nhật
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("noidung", content);
        updates.put("ngayup", uploadDate);

        // Cập nhật dữ liệu
        chapterRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditChapterActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditChapterActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteChapter() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa nội dung chapter này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteChapter())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteChapter() {
        // Trong cấu trúc hiện tại, không thể xóa hoàn toàn chapter (vì cấu trúc cố định)
        // Thay vào đó, xóa nội dung của chapter
        AlertDialog loadingDialog = showLoadingDialog();

        Map<String, Object> emptyChapter = new HashMap<>();
        emptyChapter.put("title", "");
        emptyChapter.put("noidung", "");
        emptyChapter.put("ngayup", "");

        chapterRef.updateChildren(emptyChapter)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditChapterActivity.this, "Xóa chapter thành công", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(EditChapterActivity.this, NovelsInfoActivity.class);
                    intent.putExtra("novelId", novelId);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditChapterActivity.this, "Lỗi khi xóa chapter: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_loading);
        builder.setCancelable(false);
        return builder.show();
    }
}