package com.example.appdoctruyen.activities;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.appdoctruyen.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditNovelActivity extends AppCompatActivity {

    private EditText edtTitle, edtAuthor, edtGenre, edtPublishDate, edtSummary;
    private TextView tvChapterCount;
    private ImageView imgCover;
    private Button btnSelectCover, btnUpdateNovel, btnDeleteNovel;
    private Spinner spinnerStatus;

    private String novelId;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Calendar calendar = Calendar.getInstance();

    // Mảng giá trị tình trạng
    private String[] statusOptions;
    private int selectedStatusPosition = 0;

    private DatabaseReference novelRef;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_novel);

        // Initialize Firebase
        novelRef = FirebaseDatabase.getInstance().getReference("truyen");
        storageRef = FirebaseStorage.getInstance().getReference("covers");

        // Initialize views
        edtTitle = findViewById(R.id.edtTitleNovel);
        edtAuthor = findViewById(R.id.edtAuthorNovel);
        edtGenre = findViewById(R.id.edtGenre);
        edtPublishDate = findViewById(R.id.edtPublishDate);
        edtSummary = findViewById(R.id.edtSummary);
        tvChapterCount = findViewById(R.id.tvChapterCount);
        imgCover = findViewById(R.id.imgCoverNovel);
        btnSelectCover = findViewById(R.id.btnSelectCover);
        btnUpdateNovel = findViewById(R.id.btnUpdateNovel);
        btnDeleteNovel = findViewById(R.id.btnDeleteNovel);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        // Thiết lập danh sách tình trạng
        statusOptions = new String[]{"Đang tiến hành", "Đã hoàn thành", "Tạm ngưng", "Ngưng tiến hành"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statusOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStatusPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });

        // Get novel ID from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("novelId")) {
            novelId = intent.getStringExtra("novelId");
            loadNovelData();
            countChapters();
        } else {
            Toast.makeText(this, "Không thể tìm thấy thông tin truyện", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Setup date picker for publish date
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        edtPublishDate.setOnClickListener(v -> {
            new DatePickerDialog(EditNovelActivity.this, dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Set click listeners
        btnSelectCover.setOnClickListener(v -> openFileChooser());

        btnUpdateNovel.setOnClickListener(v -> updateNovel());

        btnDeleteNovel.setOnClickListener(v -> confirmDeleteNovel());
    }

    private void updateDateLabel() {
        String format = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        edtPublishDate.setText(sdf.format(calendar.getTime()));
    }

    private void countChapters() {
        novelRef.child(novelId).child("chapter").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Đếm số lượng chapters có dữ liệu (title không rỗng)
                long count = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (snapshot.child("title").exists() &&
                            snapshot.child("title").getValue(String.class) != null &&
                            !snapshot.child("title").getValue(String.class).isEmpty()) {
                        count++;
                    }
                }
                tvChapterCount.setText("Số lượng chap: " + count);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(EditNovelActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNovelData() {
        novelRef.child(novelId).get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                String title = dataSnapshot.child("tentruyen").getValue(String.class);
                String author = dataSnapshot.child("tacgia").getValue(String.class);
                String genre = dataSnapshot.child("theloai").getValue(String.class);
                String publishDate = dataSnapshot.child("ngayxuatban").getValue(String.class);
                String summary = dataSnapshot.child("tomtat").getValue(String.class);
                String status = dataSnapshot.child("tinhtrang").getValue(String.class);
                String coverUrl = dataSnapshot.child("linkanh").getValue(String.class);

                edtTitle.setText(title);
                edtAuthor.setText(author != null ? author : "");
                edtGenre.setText(genre != null ? genre : "");
                edtPublishDate.setText(publishDate != null ? publishDate : "");
                edtSummary.setText(summary != null ? summary : "");

                // Set spinner selection based on status value
                if (status != null) {
                    status = status.trim();
                    for (int i = 0; i < statusOptions.length; i++) {
                        if (statusOptions[i].equalsIgnoreCase(status)) {
                            spinnerStatus.setSelection(i);
                            selectedStatusPosition = i;
                            break;
                        }
                    }
                }

                if (coverUrl != null && !coverUrl.isEmpty()) {
                    Glide.with(this)
                            .load(coverUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .into(imgCover);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(imgCover);
        }
    }

    private void updateNovel() {
        String title = edtTitle.getText().toString().trim();
        String author = edtAuthor.getText().toString().trim();
        String genre = edtGenre.getText().toString().trim();
        String publishDate = edtPublishDate.getText().toString().trim();
        String summary = edtSummary.getText().toString().trim();
        String status = statusOptions[selectedStatusPosition];

        if (title.isEmpty()) {
            edtTitle.setError("Vui lòng nhập tên truyện");
            return;
        }

        // Show loading dialog
        AlertDialog loadingDialog = showLoadingDialog();

        // Chuẩn bị dữ liệu để cập nhật
        Map<String, Object> novelUpdates = new HashMap<>();
        novelUpdates.put("tentruyen", title);
        novelUpdates.put("tacgia", author);
        novelUpdates.put("theloai", genre);
        novelUpdates.put("ngayxuatban", publishDate);
        novelUpdates.put("tomtat", summary);
        novelUpdates.put("tinhtrang", status);

        if (imageUri != null) {
            // Upload ảnh mới lên Firebase Storage
            String timestamp = String.valueOf(System.currentTimeMillis());
            StorageReference fileRef = storageRef.child(novelId + "_" + timestamp + ".jpg");

            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    novelUpdates.put("linkanh", uri.toString());
                    updateNovelData(novelUpdates, loadingDialog);
                });
            }).addOnFailureListener(e -> {
                loadingDialog.dismiss();
                Toast.makeText(EditNovelActivity.this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // Cập nhật dữ liệu mà không thay đổi ảnh
            updateNovelData(novelUpdates, loadingDialog);
        }
    }

    private void updateNovelData(Map<String, Object> novelUpdates, AlertDialog loadingDialog) {
        novelRef.child(novelId).updateChildren(novelUpdates)
                .addOnSuccessListener(aVoid -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditNovelActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditNovelActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteNovel() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa truyện này? Tất cả các chapter của truyện cũng sẽ bị xóa.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteNovel())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteNovel() {
        AlertDialog loadingDialog = showLoadingDialog();

        // Xóa truyện và tất cả chapter
        novelRef.child(novelId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật truyện yêu thích của người dùng
                    removeFromUserFavorites(loadingDialog);
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    Toast.makeText(EditNovelActivity.this, "Lỗi khi xóa truyện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromUserFavorites(AlertDialog loadingDialog) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Xóa truyện này khỏi danh sách yêu thích của tất cả người dùng
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    if (userSnapshot.child("favorite").child(novelId).exists()) {
                        usersRef.child(userSnapshot.getKey()).child("favorite").child(novelId).removeValue();
                    }
                }

                loadingDialog.dismiss();
                Toast.makeText(EditNovelActivity.this, "Xóa truyện thành công", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(EditNovelActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                loadingDialog.dismiss();
                Toast.makeText(EditNovelActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_loading);
        builder.setCancelable(false);
        return builder.show();
    }
}