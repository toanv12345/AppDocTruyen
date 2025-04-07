package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.adapter.NovelAdapter;
import com.example.appdoctruyen.object.Chapter;
import com.example.appdoctruyen.object.Novel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    GridView gvDSTruyen;
    private DatabaseReference mDatabase;
    NovelAdapter novelAdapter;
    ArrayList<Novel> novelArrayList;
    private Button btnNextPage, btnPrevPage;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 12;

    private ImageView accountIcon;
    private FirebaseAuth auth;
    private ImageView imgFollow;
    SearchView searchView;
    private Button btnAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        super.setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("truyen");

        init();
        anhXa();
        setUp();
        setClick();
        checkLoginStatus();
        checkAdminStatus();
        //createDefaultAdminAccount(); //vì tạo tài khoản admin mặc định nên chỉ cần chạy 1 lần

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        retrieveDataFromFirebase();

        // Set up SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNovels(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterNovels(newText);
                return false;
            }
        });
    }

    private void init() {
        novelArrayList = new ArrayList<>();
        novelAdapter = new NovelAdapter(this, novelArrayList);
    }

    private void anhXa() {
        gvDSTruyen = findViewById(R.id.rvDSTruyen);
        gvDSTruyen.setAdapter(novelAdapter);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        accountIcon = findViewById(R.id.account_icon);
        searchView = findViewById(R.id.search_view);
        imgFollow = findViewById(R.id.img_follow);
        btnAdd = findViewById(R.id.btn_add);
    }

    private void setUp() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, novelArrayList.size());
        ArrayList<Novel> pageData = new ArrayList<>(novelArrayList.subList(start, end));

        for(Novel novel : pageData) {
            String latestChapterName = getLatestChapterName(novel);
            novel.setLatestChapter(latestChapterName);
        }

        novelAdapter.updateList(pageData);
    }

    private void setClick() {
        accountIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNovelActivity.class);
            startActivity(intent);
        });

        btnNextPage.setOnClickListener(v -> nextPage());
        btnPrevPage.setOnClickListener(v -> prevPage());

        imgFollow.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Đăng nhập")
                        .setMessage("Bạn cần đăng nhập để có thể theo dõi truyện!")
                        .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                            // Chuyển đến màn hình đăng nhập
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton("Để sau", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                Intent intent = new Intent(MainActivity.this, FollowActivity.class);
                startActivity(intent);
            }
        });

        gvDSTruyen.setOnItemClickListener((parent, view, position, id) -> {
            Novel novel = (Novel) novelAdapter.getItem(position);
            Intent intent = new Intent(MainActivity.this, NovelsInfoActivity.class);
            intent.putExtra("novelId", novel.getId());
            intent.putExtra("tomtat", novel.getTomtat());
            startActivity(intent);
        });

        // đăng xuất
        accountIcon.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            auth.signOut();
                            checkLoginStatus();
                            btnAdd.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Không", null)
                        .show();
            } else {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private void nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < novelArrayList.size()) {
            currentPage++;
            setUp();
            gvDSTruyen.post(() -> gvDSTruyen.smoothScrollToPosition(0));
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            setUp();
            gvDSTruyen.post(() -> gvDSTruyen.smoothScrollToPosition(0));
        }
    }

    private void retrieveDataFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                novelArrayList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Novel novel = snapshot.getValue(Novel.class);
                    novel.setId(snapshot.getKey());
                    novelArrayList.add(novel);
                }
                // sap xep theo ngay up chapter
                novelArrayList.sort((novel1, novel2) -> {
                    String date1 = getLatestChapterDate(novel1);
                    String date2 = getLatestChapterDate(novel2);
                    if (date1.isEmpty() && date2.isEmpty()) {
                        return 0;
                    } else if (date1.isEmpty()) {
                        return 1;
                    } else if (date2.isEmpty()) {
                        return -1;
                    } else {
                        return date2.compareTo(date1);
                    }
                });
                setUp();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to read data", databaseError.toException());
            }
        });
    }
    private void checkLoginStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Người dùng đã đăng nhập → Đổi icon tài khoản thành avatar
            accountIcon.setImageResource(R.drawable.new_img_login_foreground);
        } else {
            // Chưa đăng nhập → Hiển thị icon mặc định
            accountIcon.setImageResource( R.drawable.img_login_foreground);
        }
    }
    // tim kiem truyen
    private void filterNovels(String query) {
        ArrayList<Novel> filteredList = new ArrayList<>();
        for (Novel novel : novelArrayList) {
            String title = novel.getTentruyen() != null ? novel.getTentruyen().toLowerCase() : "";
            String author = novel.getTacgia() != null ? novel.getTacgia().toLowerCase() : "";
            String status = novel.getTinhtrang() != null ? novel.getTinhtrang().toLowerCase() : "";
            String genre = novel.getTheloai() != null ? novel.getTheloai().toLowerCase() : "";

            if (title.contains(query.toLowerCase()) ||
                    author.contains(query.toLowerCase()) ||
                    status.contains(query.toLowerCase()) ||
                    genre.contains(query.toLowerCase())) {
                filteredList.add(novel);
            }
        }
        novelAdapter.updateList(filteredList);
    }

    // Lấy ngày up của chapter mới nhất có nội dung
    private String getLatestChapterDate(Novel novel) {
        String latestDate = "";
        for (Chapter chapter : novel.getChapter().values()) {
            // Chỉ xét chapter có nội dung
            if (chapter.getNoidung() != null && !chapter.getNoidung().trim().isEmpty()) {
                if (chapter.getNgayup().compareTo(latestDate) > 0) {
                    latestDate = chapter.getNgayup();
                }
            }
        }
        return latestDate;
    }

    private void checkAdminStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean isAdmin = snapshot.child("isAdmin").getValue(Boolean.class);
                        if (isAdmin != null && isAdmin) {
                            btnAdd.setVisibility(View.VISIBLE);
                        } else {
                            btnAdd.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("MainActivity", "Failed to check admin status", error.toException());
                }
            });
        } else {
            btnAdd.setVisibility(View.GONE);
        }
    }

    //thêm tài khoản admin
    private void createDefaultAdminAccount() {
        String defaultAdminEmail = "admin@gmail.com";
        String defaultAdminPassword = "admin123";
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(defaultAdminEmail, defaultAdminPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        if (user != null) {
                            usersRef.child(user.getUid()).child("isAdmin").setValue(true)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d("MainActivity", "Admin account created successfully");
                                        } else {
                                            Log.e("MainActivity", "Failed to set admin status: " + task1.getException().getMessage());
                                        }
                                    });
                        }
                    } else {
                        Log.e("MainActivity", "Admin account creation failed: " + task.getException().getMessage());
                    }
                });
    }

    private String getLatestChapterName(Novel novel) {
        String latestChapterName = "";
        String latestDate = "";
        for (Chapter chapter : novel.getChapter().values()) {
            if (chapter.getNoidung() != null && !chapter.getNoidung().trim().isEmpty()) {
                if (chapter.getNgayup().compareTo(latestDate) > 0) {
                    latestDate = chapter.getNgayup();
                    latestChapterName = chapter.getTitle();
                }
            }
        }
        return latestChapterName;
    }
}