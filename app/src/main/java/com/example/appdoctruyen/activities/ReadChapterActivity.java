package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.adapter.ChapterAdapter;
import com.example.appdoctruyen.object.Chapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ReadChapterActivity extends AppCompatActivity {

    private TextView txtTitle, txtContent;
    private BottomNavigationView bottomNavigationView;
    private List<Chapter> chapterList;
    private int currentIndex;
    private ChapterAdapter chapterAdapter;
    private ImageButton btnEditChapter; // Thêm biến cho nút sửa

    // Biến để lưu trữ novelId và chapterId hiện tại
    private String novelId;
    private String currentChapterId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_read_chapter);

        // Hiển thị ActionBar để có thể hiển thị menu
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đọc truyện");
        }

        txtTitle = findViewById(R.id.txtTitle);
        txtContent = findViewById(R.id.txtContent);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        View rootLayout = findViewById(R.id.rootLayout);
        btnEditChapter = findViewById(R.id.btnEditChapter); // Ánh xạ nút sửa

        // Lấy dữ liệu từ intent
        chapterList = (List<Chapter>) getIntent().getSerializableExtra("chapterList");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);
        novelId = getIntent().getStringExtra("novelId");

        if (chapterList != null && !chapterList.isEmpty()) {
            Chapter currentChapter = chapterList.get(currentIndex);
            currentChapterId = currentChapter.getId();
            displayChapter(currentChapter);
        } else {
            Log.e("ReadChapterActivity", "Chapter list is null or empty");
        }

        chapterAdapter = new ChapterAdapter(chapterList, (chapter, position) -> {
            // Handle item click if needed
            currentIndex = position;
            currentChapterId = chapter.getId();
            displayChapter(chapter);
        });

        displayChapter(chapterList.get(currentIndex));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.previous_chapter) {
                if (currentIndex > 0) {
                    currentIndex--;
                    Chapter chapter = chapterList.get(currentIndex);
                    currentChapterId = chapter.getId();
                    displayChapter(chapter);
                } else {
                    Toast.makeText(this, "This is the first chapter", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (itemId == R.id.chapter_list) {
                finish();
                return true;
            } else if (itemId == R.id.next_chapter) {
                if (currentIndex < chapterList.size() - 1) {
                    currentIndex++;
                    Chapter chapter = chapterList.get(currentIndex);
                    currentChapterId = chapter.getId();
                    displayChapter(chapter);
                } else {
                    Toast.makeText(this, "This is the last chapter", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            if(itemId == R.id.home) {
                Intent intent = new Intent(ReadChapterActivity.this, MainActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Thiết lập nút sửa chapter
        setupEditButton();
    }

    // Phương thức thiết lập nút sửa
    private void setupEditButton() {
        // Kiểm tra nếu người dùng là admin
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid());

            userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && Boolean.TRUE.equals(dataSnapshot.getValue(Boolean.class))) {
                        // Người dùng là admin, hiển thị nút sửa
                        btnEditChapter.setVisibility(View.VISIBLE);
                    } else {
                        // Người dùng không phải admin, ẩn nút
                        btnEditChapter.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    btnEditChapter.setVisibility(View.GONE);
                }
            });
        }

        // Thiết lập sự kiện click cho nút sửa
        btnEditChapter.setOnClickListener(v -> {
            if (novelId != null && currentChapterId != null) {
                Intent intent = new Intent(ReadChapterActivity.this, EditChapterActivity.class);
                intent.putExtra("novelId", novelId);
                intent.putExtra("chapterId", currentChapterId);
                startActivity(intent);
            } else {
                Toast.makeText(ReadChapterActivity.this, "Không thể sửa chapter này", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayChapter(Chapter chapter) {
        txtTitle.setText(chapter.getTitle());
        txtContent.setText(Html.fromHtml(chapter.getNoidung(), Html.FROM_HTML_MODE_LEGACY));
    }

    // Cập nhật dữ liệu khi quay lại từ màn hình sửa
    @Override
    protected void onRestart() {
        super.onRestart();

        if (novelId != null && currentChapterId != null) {
            DatabaseReference chapterRef = FirebaseDatabase.getInstance().getReference("truyen")
                    .child(novelId).child("chapter").child(currentChapterId);

            chapterRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String title = dataSnapshot.child("title").getValue(String.class);
                        String content = dataSnapshot.child("noidung").getValue(String.class);
                        String ngayup = dataSnapshot.child("ngayup").getValue(String.class);

                        if (title != null && content != null &&
                                chapterList != null && currentIndex >= 0 && currentIndex < chapterList.size()) {

                            // Cập nhật object trong list
                            Chapter chapter = chapterList.get(currentIndex);
                            chapter.setTitle(title);
                            chapter.setNoidung(content);
                            chapter.setNgayup(ngayup);

                            // Cập nhật UI
                            displayChapter(chapter);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("ReadChapterActivity", "Lỗi khi refresh chapter: " + databaseError.getMessage());
                }
            });
        }
    }
}