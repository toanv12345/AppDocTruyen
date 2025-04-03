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
    SearchView searchView;

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
    }

    private void setUp() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, novelArrayList.size());
        ArrayList<Novel> pageData = new ArrayList<>(novelArrayList.subList(start, end));

        novelAdapter.updateList(pageData);
    }

    private void setClick() {
        accountIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnNextPage.setOnClickListener(v -> nextPage());
        btnPrevPage.setOnClickListener(v -> prevPage());

        // yeu cau dang nhap moi bam duoc
        gvDSTruyen.setOnItemClickListener((parent, view, position, id) -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(MainActivity.this, "Vui lòng đăng nhập để xem truyện!", Toast.LENGTH_SHORT).show();
            } else {
                Novel novel = (Novel) novelAdapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, NovelsInfoActivity.class);
                intent.putExtra("novelId", novel.getId());
                intent.putExtra("tomtat", novel.getTomtat());
                startActivity(intent);
            }
        });

        accountIcon.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Đăng xuất")
                        .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                        .setPositiveButton("Có", (dialog, which) -> {
                            auth.signOut();
                            checkLoginStatus();
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
    //lay ngay up chapter
    private String getLatestChapterDate(Novel novel) {
        String latestDate = "";
        for (Chapter chapter : novel.getChapter().values()) {
            if(chapter.getNgayup().compareTo(latestDate) > 0) {
                latestDate = chapter.getNgayup();
            }
        }
        return latestDate;
    }
}

