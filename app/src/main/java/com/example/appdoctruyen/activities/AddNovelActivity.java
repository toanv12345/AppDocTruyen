package com.example.appdoctruyen.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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

public class AddNovelActivity extends AppCompatActivity {
    private EditText edtNewNovelImage, edtNewNovelTentruyen, edtNewNovelTacgia,
            edtNewNovelTheloai, edtNewNovelTomtat, edtNewNovelPublishDate;
    private ImageView imgNovelPreview;
    private Button btnAddNovel, btnPreviewImage;
    private Spinner spinnerStatus;
    private DatabaseReference databaseReference;
    private int novelCount = 0;
    private Calendar calendar = Calendar.getInstance();
    private String[] statusOptions;
    private int selectedStatusPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_novel);

        // Ánh xạ các view
        edtNewNovelImage = findViewById(R.id.edt_new_novel_image);
        edtNewNovelTentruyen = findViewById(R.id.edt_new_novel_tentruyen);
        edtNewNovelTacgia = findViewById(R.id.edt_new_novel_tacgia);
        edtNewNovelTheloai = findViewById(R.id.edt_new_novel_theloai);
        edtNewNovelTomtat = findViewById(R.id.edt_new_novel_tomtat);
        edtNewNovelPublishDate = findViewById(R.id.edt_new_novel_publish_date);
        imgNovelPreview = findViewById(R.id.img_novel_preview);
        btnAddNovel = findViewById(R.id.btnAddNovel);
        btnPreviewImage = findViewById(R.id.btn_preview_image);
        spinnerStatus = findViewById(R.id.spinner_status);

        // Thiết lập DatePicker cho ngày xuất bản
        final DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        edtNewNovelPublishDate.setOnClickListener(v -> {
            new DatePickerDialog(AddNovelActivity.this, dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

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

        // Xem trước ảnh
        btnPreviewImage.setOnClickListener(v -> {
            String imageUrl = edtNewNovelImage.getText().toString().trim();
            if (!imageUrl.isEmpty()) {
                loadImageFromUrl(imageUrl);
            } else {
                Toast.makeText(AddNovelActivity.this, "Vui lòng nhập URL ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        // Khởi tạo Firebase Database
        databaseReference = FirebaseDatabase.getInstance().getReference("truyen");

        // Lấy số lượng truyện hiện tại để tạo key mới
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String key = snapshot.getKey();
                    if (key != null && key.startsWith("truyen")) {
                        try {
                            int number = Integer.parseInt(key.substring(6));
                            if (number > novelCount) {
                                novelCount = number;
                            }
                        } catch (NumberFormatException e) {
                            // Bỏ qua nếu không phải số
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AddNovelActivity.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnAddNovel.setOnClickListener(v -> {
            // Hiển thị dialog loading
            AlertDialog loadingDialog = showLoadingDialog();

            String linkanh = edtNewNovelImage.getText().toString().trim();
            String tentruyen = edtNewNovelTentruyen.getText().toString().trim();
            String tacgia = edtNewNovelTacgia.getText().toString().trim();
            String theloai = edtNewNovelTheloai.getText().toString().trim();
            String tomtat = edtNewNovelTomtat.getText().toString().trim();
            String ngayxuatban = edtNewNovelPublishDate.getText().toString().trim();
            String tinhtrang = statusOptions[selectedStatusPosition];

            // Kiểm tra các trường bắt buộc
            if (tentruyen.isEmpty()) {
                loadingDialog.dismiss();
                edtNewNovelTentruyen.setError("Vui lòng nhập tên truyện");
                return;
            }

            if (linkanh.isEmpty()) {
                loadingDialog.dismiss();
                edtNewNovelImage.setError("Vui lòng nhập URL ảnh bìa");
                return;
            }

            // Tạo dữ liệu để lưu vào database
            Map<String, Object> novelData = new HashMap<>();
            novelData.put("linkanh", linkanh);
            novelData.put("tentruyen", tentruyen);
            novelData.put("tacgia", tacgia);
            novelData.put("theloai", theloai);
            novelData.put("tomtat", tomtat);
            novelData.put("ngayxuatban", ngayxuatban);
            novelData.put("tinhtrang", tinhtrang);

            // Tạo ID mới cho truyện
            String novelId = "truyen" + (novelCount + 1);

            // Lưu thông tin truyện vào database
            databaseReference.child(novelId).setValue(novelData)
                    .addOnSuccessListener(aVoid -> {
                        // Tạo node chapter trống
                        databaseReference.child(novelId).child("chapter").setValue(null)
                                .addOnSuccessListener(aVoid1 -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(AddNovelActivity.this, "Thêm truyện thành công!", Toast.LENGTH_SHORT).show();

                                    // Quay về MainActivity
                                    Intent intent = new Intent(AddNovelActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    loadingDialog.dismiss();
                                    Toast.makeText(AddNovelActivity.this, "Lỗi khi tạo chương: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        Toast.makeText(AddNovelActivity.this, "Lỗi khi thêm truyện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void updateDateLabel() {
        String format = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        edtNewNovelPublishDate.setText(sdf.format(calendar.getTime()));
    }

    private void loadImageFromUrl(String imageUrl) {
        try {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imgNovelPreview);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể tải ảnh từ URL này", Toast.LENGTH_SHORT).show();
        }
    }

    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_loading);
        builder.setCancelable(false);
        return builder.show();
    }
}