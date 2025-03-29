// MainActivity.java
package com.example.appdoctruyen.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import android.os.Handler;
import android.widget.Toast;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.adapter.NovelAdapter;
import com.example.appdoctruyen.object.Novel;
import android.content.Intent;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    RecyclerView rvDSTruyen;

    private DatabaseReference mDatabase;
    NovelAdapter novelAdapter;
    ArrayList<Novel> novelArrayList;
    private Button btnNextPage, btnPrevPage;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 12; // 4 rows x 3 columns

    private ImageView accountIcon;
    private ImageView newAccountIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this); // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("novels"); // Initialize DatabaseReference
        init();
        anhXa();
        setUp();
        setClick();
        imgNen = findViewById(R.id.Nen);
        startImageSlideshow();
        setNewAccountIcon();
        ontop();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Retrieve data from Firebase
        retrieveDataFromFirebase();
    }

    private void init() {
        novelArrayList = new ArrayList<>();
        novelAdapter = new NovelAdapter(novelArrayList); // Corrected constructor
    }

    private void anhXa() {
        rvDSTruyen = findViewById(R.id.rvDSTruyen);
        rvDSTruyen.setLayoutManager(new GridLayoutManager(this, 3));
        rvDSTruyen.setAdapter(novelAdapter);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);
    }

    private void setUp() {
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, novelArrayList.size());
        ArrayList<Novel> pageData = new ArrayList<>(novelArrayList.subList(start, end)); // Fix subList()

        // Update adapter data
        novelAdapter.updateList(pageData);
    }

    private void setClick() {
        accountIcon = findViewById(R.id.account_icon);
        accountIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnNextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextPage();
            }
        });
        btnPrevPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevPage();
            }
        });
    }

    // Image slideshow
    private int[] imageArray = {
            R.mipmap.test1,
            R.mipmap.test2,
            R.mipmap.test3
    };
    private int currentIndex = 0;
    private ImageView imgNen;

    private void startImageSlideshow() {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                currentIndex++;
                if (currentIndex >= imageArray.length) {
                    currentIndex = 0;
                }
                imgNen.setImageResource(imageArray[currentIndex]);
                handler.postDelayed(this, 3000); // Change image every 3 seconds
            }
        };
        handler.postDelayed(runnable, 3000);
    }

    // Next page
    private void nextPage() {
        if ((currentPage + 1) * ITEMS_PER_PAGE < novelArrayList.size()) {
            currentPage++;
            setUp();
            rvDSTruyen.post(() -> rvDSTruyen.scrollToPosition(0)); // Scroll to top of page
        }
    }

    // Previous page
    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            setUp();
            rvDSTruyen.post(() -> rvDSTruyen.scrollToPosition(0)); // Scroll to top of page
        }
    }

    public void setNewAccountIcon() {
        newAccountIcon = findViewById(R.id.account_icon);

        int imgResouce = getIntent().getIntExtra("imgResouce", R.drawable.new_img_login_foreground);
        newAccountIcon.setImageResource(imgResouce);
    }

    public void ontop() {
        super.onStop();
        // Sign out
        FirebaseAuth.getInstance().signOut();
        // Restore old icon
        newAccountIcon = findViewById(R.id.account_icon);
        int imgResouce = getIntent().getIntExtra("imgResouce", R.drawable.img_login_foreground);
        newAccountIcon.setImageResource(imgResouce);
    }

    private void retrieveDataFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                novelArrayList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Novel novel = snapshot.getValue(Novel.class);
                    novelArrayList.add(novel);
                }
                setUp();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("MainActivity", "Failed to read data", databaseError.toException());
            }
        });
    }
}