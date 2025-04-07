package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ReadChapterActivity extends AppCompatActivity {

    private TextView txtTitle, txtContent;
    private ImageButton btnEditChapter;
    private BottomNavigationView bottomNavigationView;

    private ArrayList<Chapter> chapterList;
    private int currentIndex;
    private String novelId;
    private DatabaseReference readChaptersRef;
    private boolean isAdmin = false;
    private boolean isChapterRead = true; // Mặc định chapter hiện tại là đã đọc
    private Chapter currentChapter;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_chapter);

        // Ánh xạ các thành phần UI
        txtTitle = findViewById(R.id.txtTitle);
        txtContent = findViewById(R.id.txtContent);
        btnEditChapter = findViewById(R.id.btnEditChapter);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Lấy dữ liệu từ Intent
        Intent intent = getIntent();
        if (intent != null) {
            chapterList = (ArrayList<Chapter>) intent.getSerializableExtra("chapterList");
            currentIndex = intent.getIntExtra("currentIndex", 0);
            novelId = intent.getStringExtra("novelId");
        }

        // Kiểm tra dữ liệu
        if (chapterList == null || chapterList.isEmpty() || novelId == null) {
            Toast.makeText(this, "Không thể tải nội dung chapter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Thiết lập Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Khởi tạo reference đến danh sách chapter đã đọc
            readChaptersRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("readChapters")
                    .child(novelId);

            // Kiểm tra xem người dùng có phải là admin không
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean adminValue = snapshot.getValue(Boolean.class);
                        isAdmin = adminValue != null && adminValue;

                        // Hiện nút sửa chapter nếu là admin
                        if (isAdmin) {
                            btnEditChapter.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Không làm gì
                }
            });
        }

        // Hiển thị nội dung chapter hiện tại
        displayChapterContent(currentIndex);

        // Thiết lập sự kiện click cho nút sửa chapter
        btnEditChapter.setOnClickListener(v -> {
            if (currentChapter != null && currentChapter.getId() != null) {
                Intent editIntent = new Intent(ReadChapterActivity.this, EditChapterActivity.class);
                editIntent.putExtra("novelId", novelId);
                editIntent.putExtra("chapterId", currentChapter.getId());
                startActivity(editIntent);
            }
        });

        // Thiết lập lắng nghe sự kiện cho BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.previous_chapter) {
                navigateToPreviousChapter();
                return true;
            } else if (itemId == R.id.home) {
                navigateToHome();
                return true;
            } else if (itemId == R.id.mark_unread) {
                toggleReadStatus();
                return true;
            } else if (itemId == R.id.chapter_list) {
                navigateToChapterList();
                return true;
            } else if (itemId == R.id.next_chapter) {
                navigateToNextChapter();
                return true;
            }

            return false;
        });
    }

    // Phương thức hiển thị nội dung chapter
    private void displayChapterContent(int position) {
        if (position < 0 || position >= chapterList.size()) {
            Toast.makeText(this, "Chapter không tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }

        currentChapter = chapterList.get(position);
        if (currentChapter == null) {
            Toast.makeText(this, "Không thể tải nội dung chapter", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị tiêu đề và nội dung
        String title = currentChapter.getTitle();
        String content = currentChapter.getNoidung();

        if (title != null && !title.isEmpty()) {
            txtTitle.setText(title);
        } else {
            txtTitle.setText("Chapter không có tiêu đề");
        }

        if (content != null && !content.isEmpty()) {
            // Xử lý nội dung để hiển thị định dạng
            txtContent.setText(formatContent(content));
        } else {
            txtContent.setText("Nội dung chapter trống");
        }

        // Kiểm tra trạng thái đọc của chapter
        checkChapterReadStatus(currentChapter);

        // Đánh dấu chapter là đã đọc nếu người dùng đã đăng nhập
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            markChapterAsRead(currentChapter);
        }
    }

    // Định dạng nội dung chapter
    private CharSequence formatContent(String content) {
        // Thay thế các ký tự đặc biệt để hiển thị đúng định dạng
        content = content.replace("\n", "<br>");

        // Chuyển đổi các thẻ HTML thông thường
        return Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY);
    }

    // Điều hướng đến chapter trước
    private void navigateToPreviousChapter() {
        if (currentIndex < chapterList.size() - 1) {
            currentIndex++;
            displayChapterContent(currentIndex);
        } else {
            Toast.makeText(this, "Đây là chapter đầu tiên của truyện", Toast.LENGTH_SHORT).show();
        }
    }

    // Điều hướng đến chapter tiếp theo
    private void navigateToNextChapter() {
        if (currentIndex > 0) {
            currentIndex--;
            displayChapterContent(currentIndex);
        } else {
            Toast.makeText(this, "Đây là chapter mới nhất của truyện", Toast.LENGTH_SHORT).show();
        }
    }

    // Quay về trang chủ
    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // Hiển thị danh sách chapter thay vì quay lại màn hình trước
    private void navigateToChapterList() {
        // Hiển thị danh sách chapter trong dialog thay vì finish()
        showChapterListDialog();
    }

    // Phương thức mới để hiển thị danh sách chapter
    private void showChapterListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Danh sách chapter");

        // Tạo View cho dialog
        View view = getLayoutInflater().inflate(R.layout.dialog_chapter_list, null);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewChapterList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tạo adapter cho RecyclerView
        ChapterAdapter adapter = new ChapterAdapter(chapterList, (chapter, position) -> {
            // Khi chọn chapter, đóng dialog và hiển thị chapter được chọn
            if (currentIndex != position) {
                currentIndex = position;
                displayChapterContent(position);
            }
            dialog.dismiss();
        });

        // Đánh dấu chapter đã đọc
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference readChaptersRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("readChapters")
                    .child(novelId);

            readChaptersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Set<String> readChapterIds = new HashSet<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        readChapterIds.add(snapshot.getKey());
                    }
                    adapter.setReadChapters(readChapterIds);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Xử lý lỗi nếu cần
                }
            });
        }

        recyclerView.setAdapter(adapter);

        builder.setView(view);
        builder.setNegativeButton("Đóng", (dialogInterface, i) -> dialogInterface.dismiss());

        dialog = builder.create();
        dialog.show();
    }

    // Phương thức để chuyển đổi trạng thái đọc của chapter
    private void toggleReadStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showLoginDialog();
            return;
        }

        if (currentChapter == null || currentChapter.getId() == null) return;

        if (isChapterRead) {
            // Nếu chapter đã đọc, đánh dấu là chưa đọc
            readChaptersRef.child(currentChapter.getId()).removeValue()
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã đánh dấu chưa đọc", Toast.LENGTH_SHORT).show();

                        // Khởi động lại NovelsInfoActivity với dữ liệu mới
                        Intent intent = new Intent(ReadChapterActivity.this, NovelsInfoActivity.class);
                        intent.putExtra("novelId", novelId);

                        // Lấy các thông tin cần thiết khác từ intent hiện tại để truyền lại
                        Intent currentIntent = getIntent();
                        if (currentIntent != null && currentIntent.hasExtra("tomtat")) {
                            intent.putExtra("tomtat", currentIntent.getStringExtra("tomtat"));
                        }

                        // Đặt cờ để xóa các activity khác và tạo instance mới
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                        // Khởi động activity mới
                        startActivity(intent);
                        finish();  // Đóng ReadChapterActivity hiện tại
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Không thể đánh dấu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Nếu chapter chưa đọc, đánh dấu là đã đọc
            readChaptersRef.child(currentChapter.getId()).setValue(true)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã đánh dấu đã đọc", Toast.LENGTH_SHORT).show();
                        isChapterRead = true;
                        updateMarkUnreadIcon();
                        // Vẫn tiếp tục ở màn hình đọc chapter
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Không thể đánh dấu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Phương thức kiểm tra trạng thái đọc của chapter
    private void checkChapterReadStatus(Chapter chapter) {
        if (chapter == null || chapter.getId() == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            readChaptersRef.child(chapter.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    isChapterRead = snapshot.exists();
                    updateMarkUnreadIcon();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Xử lý lỗi nếu cần
                }
            });
        }
    }

    // Cập nhật icon cho nút đánh dấu dựa trên trạng thái hiện tại
    private void updateMarkUnreadIcon() {
        Menu menu = bottomNavigationView.getMenu();
        MenuItem markUnreadItem = menu.findItem(R.id.mark_unread);

        if (isChapterRead) {
            // Chapter đã đọc, hiển thị icon "đánh dấu chưa đọc"
            markUnreadItem.setIcon(R.drawable.ic_mark_unread);
            markUnreadItem.setTitle("Đánh dấu chưa đọc");
        } else {
            // Chapter chưa đọc, hiển thị icon "đánh dấu đã đọc"
            markUnreadItem.setIcon(R.drawable.ic_mark_read);
            markUnreadItem.setTitle("Đánh dấu đã đọc");
        }
    }

    // Phương thức hiển thị thông báo đăng nhập
    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đăng nhập")
                .setMessage("Bạn cần đăng nhập để sử dụng tính năng này!")
                .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                    Intent intent = new Intent(ReadChapterActivity.this, LoginActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Phương thức đánh dấu chapter là đã đọc
    private void markChapterAsRead(Chapter chapter) {
        if (chapter == null || chapter.getId() == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            readChaptersRef.child(chapter.getId()).setValue(true)
                    .addOnSuccessListener(unused -> {
                        isChapterRead = true;
                        updateMarkUnreadIcon();
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Khi quay lại từ màn hình khác (như EditChapterActivity)
        // Làm mới nội dung chapter hiện tại
        if (currentChapter != null && currentChapter.getId() != null) {
            refreshChapterContent(currentChapter.getId());
        }
    }

    // Phương thức làm mới nội dung chapter từ Firebase
    private void refreshChapterContent(String chapterId) {
        DatabaseReference chapterRef = FirebaseDatabase.getInstance()
                .getReference("truyen")
                .child(novelId)
                .child("chapter")
                .child(chapterId);

        chapterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Chapter updatedChapter = snapshot.getValue(Chapter.class);
                    if (updatedChapter != null) {
                        updatedChapter.setId(chapterId);

                        // Cập nhật vào danh sách
                        for (int i = 0; i < chapterList.size(); i++) {
                            if (chapterId.equals(chapterList.get(i).getId())) {
                                chapterList.set(i, updatedChapter);

                                // Nếu đây là chapter hiện tại, cập nhật giao diện
                                if (i == currentIndex) {
                                    currentChapter = updatedChapter;
                                    displayChapterContent(currentIndex);
                                }
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ReadChapterActivity.this, "Không thể cập nhật nội dung chapter", Toast.LENGTH_SHORT).show();
            }
        });
    }
}