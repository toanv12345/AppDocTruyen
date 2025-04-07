package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ScrollView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdoctruyen.R;
import com.example.appdoctruyen.adapter.ChapterAdapter;
import com.example.appdoctruyen.decor.DividerItemDecoration;
import com.example.appdoctruyen.object.Chapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NovelsInfoActivity extends AppCompatActivity {

    private DatabaseReference database;
    private RecyclerView recyclerView;
    private ChapterAdapter adapter;
    private List<Chapter> chapterList = new ArrayList<>();
    private String novelId;
    private TextView txt_description;
    private ImageView novelImageView;
    private ImageView novelBg;
    private TextView stat;
    private TextView auth;
    private TextView genre;
    private TextView novelName;
    private TextView publishDate;
    private Button btnRead;

    private Button btnReadLatest;
    private boolean isHeartFilled = false;
    private ImageView heartIcon;
    private ImageButton btnEditNovel;
    private Button btnAddChapter;
    private Button btnHome;
    private TextView chapterCount;
    private Set<String> readChapterIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_novels_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRead = findViewById(R.id.readButton);
        btnReadLatest = findViewById(R.id.readLatestButton);
        stat = findViewById(R.id.novelStatus);
        auth = findViewById(R.id.novelAuthor);
        genre = findViewById(R.id.novelGenre);
        novelName = findViewById(R.id.novelTitle);
        publishDate = findViewById(R.id.novelPublishDate);
        chapterCount = findViewById(R.id.novelChapterCount);
        txt_description = findViewById(R.id.storyContent);
        novelImageView = findViewById(R.id.novelImage);
        novelBg = findViewById(R.id.img_bg);
        heartIcon = findViewById(R.id.heartIcon);
        btnEditNovel = findViewById(R.id.btnEditNovel);
        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        btnAddChapter = findViewById(R.id.btn_chapter);
        btnHome = findViewById(R.id.btn_home);

        database = FirebaseDatabase.getInstance().getReference();
        String description = getIntent().getStringExtra("tomtat");
        if (description != null) {
            txt_description.setText(description);
        }
        setupTextViewPopups();

        novelId = getIntent().getStringExtra("novelId");
        if (novelId == null || novelId.isEmpty()) {
            Toast.makeText(this, "Invalid novel ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ChapterAdapter(chapterList, (chapter, position) -> {
            Intent intent = new Intent(NovelsInfoActivity.this, ReadChapterActivity.class);
            intent.putExtra("chapterList", new ArrayList<>(chapterList));
            intent.putExtra("currentIndex", position);
            intent.putExtra("novelId", novelId);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        fetchChapters();
        fetchStoryDetails(novelId);
        loadReadChapters();

        // Đọc từ đầu (chap 1)
        btnRead.setOnClickListener(v -> {
            if (!chapterList.isEmpty()) {
                Intent intent = new Intent(NovelsInfoActivity.this, ReadChapterActivity.class);
                intent.putExtra("chapterList", new ArrayList<>(chapterList));
                intent.putExtra("currentIndex", chapterList.size() - 1); // Index của chapter cuối cùng (cũ nhất)
                intent.putExtra("novelId", novelId);
                startActivity(intent);
            } else {
                Toast.makeText(NovelsInfoActivity.this, "Không có chapter nào", Toast.LENGTH_SHORT).show();
            }
        });

        // Đọc mới nhất
        btnReadLatest.setOnClickListener(v -> {
            if (!chapterList.isEmpty()) {
                Intent intent = new Intent(NovelsInfoActivity.this, ReadChapterActivity.class);
                intent.putExtra("chapterList", new ArrayList<>(chapterList));
                intent.putExtra("currentIndex", 0); // Index của chapter đầu tiên (mới nhất)
                intent.putExtra("novelId", novelId);
                startActivity(intent);
            } else {
                Toast.makeText(NovelsInfoActivity.this, "Không có chapter nào", Toast.LENGTH_SHORT).show();
            }
        });

        heartIcon.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Người dùng đã đăng nhập, cho phép theo dõi/hủy theo dõi
                isHeartFilled = !isHeartFilled;
                heartIcon.setImageResource(isHeartFilled ? R.drawable.ic_heart_filled : R.drawable.ic_heart_empty);
                updateFollowStatus(novelId, isHeartFilled);
            } else {
                // Người dùng chưa đăng nhập, hiển thị thông báo và chuyển đến màn hình đăng nhập
                AlertDialog.Builder builder = new AlertDialog.Builder(NovelsInfoActivity.this);
                builder.setTitle("Đăng nhập")
                        .setMessage("Bạn cần đăng nhập để có thể theo dõi truyện!")
                        .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                            // Chuyển đến màn hình đăng nhập
                            Intent intent = new Intent(NovelsInfoActivity.this, LoginActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });

        btnEditNovel.setOnClickListener(v -> {
            Intent intent = new Intent(NovelsInfoActivity.this, EditNovelActivity.class);
            intent.putExtra("novelId", novelId);
            startActivity(intent);
        });

        // Thiết lập sự kiện click cho nút trang chủ
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(NovelsInfoActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        checkFollowStatus();

        btnAddChapter.setOnClickListener(v -> {
            Intent intent = new Intent(NovelsInfoActivity.this, AddChapterActivity.class);
            intent.putExtra("novelId", novelId);
            startActivity(intent);
        });

        checkIfAdmin();
    }

    private void fetchChapters() {
        database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chaptersRef = database.child("truyen").child(novelId).child("chapter");

        chaptersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(NovelsInfoActivity.this, "Không có chương nào!", Toast.LENGTH_SHORT).show();
                    chapterCount.setText("Số lượng chap: 0");
                    return;
                }

                chapterList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chapter chapter = snapshot.getValue(Chapter.class);
                    if (chapter != null) {
                        // Chỉ thêm chapter có nội dung vào danh sách
                        String content = chapter.getNoidung();
                        if (content != null && !content.trim().isEmpty()) {
                            // Đặt ID cho chapter để sử dụng khi sửa
                            chapter.setId(snapshot.getKey());
                            chapterList.add(chapter);
                        }
                    }
                }

                // Cập nhật số lượng chap
                chapterCount.setText("Số lượng chap: " + chapterList.size());

                if (chapterList.isEmpty()) {
                    Toast.makeText(NovelsInfoActivity.this, "Không có chapter nào có nội dung!", Toast.LENGTH_SHORT).show();
                }

                // Sắp xếp chapter theo ngày (giảm dần - mới nhất đến cũ nhất)
                chapterList.sort((chapter1, chapter2) -> {
                    String date1 = chapter1.getNgayup();
                    String date2 = chapter2.getNgayup();
                    return date2.compareTo(date1); // Đảo ngược so sánh để sắp xếp giảm dần
                });

                runOnUiThread(() -> adapter.updateList(chapterList));

                // Cập nhật danh sách chapter đã đọc sau khi tải xong danh sách chapter
                loadReadChapters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Xử lý lỗi
                Toast.makeText(NovelsInfoActivity.this, "Lỗi khi tải danh sách chapter", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchStoryDetails(String novelId) {
        DatabaseReference storyRef = FirebaseDatabase.getInstance().getReference()
                .child("truyen").child(novelId);

        storyRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(NovelsInfoActivity.this, "Truyện không tồn tại!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = dataSnapshot.child("tentruyen").getValue(String.class);
                String author = dataSnapshot.child("tacgia").getValue(String.class);
                String category = dataSnapshot.child("theloai").getValue(String.class);
                String coverUrl = dataSnapshot.child("linkanh").getValue(String.class);
                String description = dataSnapshot.child("tomtat").getValue(String.class);
                String status = dataSnapshot.child("tinhtrang").getValue(String.class);
                String pubDate = dataSnapshot.child("ngayxuatban").getValue(String.class);

                novelName.setText("Tên truyện: " + name);
                auth.setText("Tác giả: " + author);
                genre.setText("Thể loại: " + category);
                txt_description.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
                stat.setText("Tình trạng: " + status);

                // Hiển thị ngày xuất bản nếu có
                if (pubDate != null && !pubDate.trim().isEmpty()) {
                    publishDate.setText("Ngày xuất bản: " + pubDate);
                    publishDate.setVisibility(View.VISIBLE);
                } else {
                    publishDate.setText("Ngày xuất bản: Chưa cập nhật");
                    publishDate.setVisibility(View.VISIBLE);
                }

                Glide.with(NovelsInfoActivity.this)
                        .load(coverUrl)
                        .into(novelImageView);

                Glide.with(NovelsInfoActivity.this)
                        .load(coverUrl)
                        .into(novelBg);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NovelsInfoActivity.this, "Lỗi tải dữ liệu: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReadChapters() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && adapter != null) {
            DatabaseReference readChaptersRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("readChapters")
                    .child(novelId);

            readChaptersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    readChapterIds.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        readChapterIds.add(snapshot.getKey());
                    }
                    adapter.setReadChapters(readChapterIds);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(NovelsInfoActivity.this, "Lỗi tải thông tin chapter đã đọc", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkFollowStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("favorite");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.hasChild(novelId)) {
                        isHeartFilled = true;
                        heartIcon.setImageResource(R.drawable.ic_heart_filled);
                    } else {
                        isHeartFilled = false;
                        heartIcon.setImageResource(R.drawable.ic_heart_empty);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(NovelsInfoActivity.this, "Lỗi tải trạng thái theo dõi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFollowStatus(String novelId, boolean isFollowed) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("favorite");

            if (isFollowed) {
                userRef.child(novelId).setValue(true)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(NovelsInfoActivity.this, "Đã theo dõi truyện", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(NovelsInfoActivity.this, "Lỗi cập nhật trạng thái theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                userRef.child(novelId).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(NovelsInfoActivity.this, "Đã bỏ theo dõi truyện", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(NovelsInfoActivity.this, "Lỗi cập nhật trạng thái theo dõi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void checkIfAdmin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            userRef.child("isAdmin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Boolean isAdmin = dataSnapshot.getValue(Boolean.class);
                    if (isAdmin != null && isAdmin) {
                        // Người dùng là admin
                        btnAddChapter.setVisibility(View.VISIBLE);
                        btnEditNovel.setVisibility(View.VISIBLE);
                        heartIcon.setVisibility(View.GONE);
                    } else {
                        // Người dùng không phải admin
                        btnAddChapter.setVisibility(View.GONE);
                        btnEditNovel.setVisibility(View.GONE);
                        heartIcon.setVisibility(View.VISIBLE);
                        checkFollowStatus(); // Kiểm tra trạng thái yêu thích cho người dùng thường
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(NovelsInfoActivity.this, "Failed to check admin status: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    btnAddChapter.setVisibility(View.GONE);
                    btnEditNovel.setVisibility(View.GONE);
                    heartIcon.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        fetchStoryDetails(novelId);
        fetchChapters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReadChapters();
    }

    private void setupTextViewPopups() {
        // Setup popup cho tiêu đề
        novelName.setOnClickListener(v -> {
            String title = novelName.getText().toString();
            if (!title.isEmpty()) {
                showPopup("Tiêu đề truyện", title);
            }
        });

        // Setup popup cho tác giả
        auth.setOnClickListener(v -> {
            String author = auth.getText().toString();
            if (!author.isEmpty()) {
                showPopup("Tác giả", author);
            }
        });

        // Setup popup cho thể loại
        genre.setOnClickListener(v -> {
            String genreText = genre.getText().toString();
            if (!genreText.isEmpty()) {
                showPopup("Thể loại", genreText);
            }
        });

        // Setup popup cho cốt truyện
        txt_description.setOnClickListener(v -> {
            String description = txt_description.getText().toString();
            if (!description.isEmpty()) {
                showPopup("Cốt truyện", description);
            }
        });

        // Setup popup cho ngày xuất bản
        publishDate.setOnClickListener(v -> {
            String pubDateText = publishDate.getText().toString();
            if (!pubDateText.isEmpty()) {
                showPopup("Ngày xuất bản", pubDateText);
            }
        });

        // Setup popup cho tình trạng
        stat.setOnClickListener(v -> {
            String status = stat.getText().toString();
            if (!status.isEmpty()) {
                showPopup("Tình trạng", status);
            }
        });
    }

    private void showPopup(String title, String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Tạo ScrollView để nội dung có thể cuộn nếu quá dài
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setText(content);
        textView.setPadding(30, 30, 30, 30);
        textView.setTextSize(16);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Thiết lập kích thước tối đa cho dialog
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            // Đặt chiều rộng là 90% màn hình
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            // Đặt chiều cao tối đa là 80% màn hình
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            layoutParams.height = Math.min(layoutParams.height,
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.8));
            window.setAttributes(layoutParams);
        }
    }
}