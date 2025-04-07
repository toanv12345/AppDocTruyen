package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ReadChapterActivity extends AppCompatActivity {

    private TextView txtTitle, txtContent;
    private BottomNavigationView bottomNavigationView;
    private List<Chapter> chapterList;
    private int currentIndex;
    private ChapterAdapter chapterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_read_chapter);

        txtTitle = findViewById(R.id.txtTitle);
        txtContent = findViewById(R.id.txtContent);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        View rootLayout = findViewById(R.id.rootLayout);

        chapterList = (List<Chapter>) getIntent().getSerializableExtra("chapterList");
        currentIndex = getIntent().getIntExtra("currentIndex", 0);

        if (chapterList != null && !chapterList.isEmpty()) {
            Chapter currentChapter = chapterList.get(currentIndex);
            displayChapter(currentChapter);
        } else {
            Log.e("ReadChapterActivity", "Chapter list is null or empty");
        }

        chapterAdapter = new ChapterAdapter(chapterList, (chapter, position) -> {
            // Handle item click if needed
            displayChapter(chapter);
        });

            displayChapter(chapterList.get(currentIndex));

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.previous_chapter) {
                if (currentIndex > 0) {
                    currentIndex--;
                    displayChapter(chapterList.get(currentIndex));
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
                    displayChapter(chapterList.get(currentIndex));
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
    }

    private void displayChapter(Chapter chapter) {
        txtTitle.setText(chapter.getTitle());
        txtContent.setText(Html.fromHtml(chapter.getNoidung(), Html.FROM_HTML_MODE_LEGACY));
    }

}
