package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.List;
import android.text.Html;


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
    private Button btnRead;
    private boolean isHeartFilled = false;

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
        stat = findViewById(R.id.novelStatus);
        auth = findViewById(R.id.novelAuthor);
        genre = findViewById(R.id.novelGenre);
        novelName = findViewById(R.id.novelTitle);
        txt_description = findViewById(R.id.storyContent);
        novelImageView = findViewById(R.id.novelImage);
        novelBg = findViewById(R.id.img_bg);
        ImageView heartIcon = findViewById(R.id.heartIcon);
        recyclerView = findViewById(R.id.recyclerViewChapters);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        database = FirebaseDatabase.getInstance().getReference("truyen").child("truyen3").child("linkanh");
        String description = getIntent().getStringExtra("tomtat");
        txt_description.setText(description);

        novelId = getIntent().getStringExtra("novelId");
        if (novelId == null) {
            Toast.makeText(this, "Invalid novel ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ChapterAdapter(chapterList, (chapter, position) -> {
            Intent intent = new Intent(NovelsInfoActivity.this, ReadChapterActivity.class);
            intent.putExtra("chapterList", new ArrayList<>(chapterList));
            intent.putExtra("currentIndex", position);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
        fetchChapters();
        fetchStoryDetails(novelId);

        btnRead.setOnClickListener(v -> {
            if (!chapterList.isEmpty()) {
                Intent intent = new Intent(NovelsInfoActivity.this, ReadChapterActivity.class);
                intent.putExtra("chapterList", new ArrayList<>(chapterList));
                intent.putExtra("currentIndex", 0);
                startActivity(intent);
            } else {
                Toast.makeText(NovelsInfoActivity.this, "No chapters available", Toast.LENGTH_SHORT).show();
            }
        });

        heartIcon.setOnClickListener(v -> {
            if (isHeartFilled) {
                heartIcon.setImageResource(R.drawable.ic_heart_empty);
            } else {
                heartIcon.setImageResource(R.drawable.ic_heart_filled);
            }
            isHeartFilled = !isHeartFilled;
        });
    }

    private void fetchChapters() {
        database = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chaptersRef = database.child("truyen").child(novelId).child("chapter");

        chaptersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(NovelsInfoActivity.this, "Không có chương nào!", Toast.LENGTH_SHORT).show();
                    return;
                }

                chapterList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chapter chapter = snapshot.getValue(Chapter.class);
                    if (chapter != null) {
                        chapterList.add(chapter);
                    }
                }

                runOnUiThread(() -> adapter.updateList(chapterList));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NovelsInfoActivity.this, "Lỗi tải chương: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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

                novelName.setText("Tên truyện: " + name);
                auth.setText("Tác giả: " + author);
                genre.setText("Thể loại: " + category);
                txt_description.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
                stat.setText("Tình trạng: " + status);

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
}