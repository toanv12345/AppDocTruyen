// FollowActivity.java
package com.example.appdoctruyen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.adapter.FavoriteAdapter;
import com.example.appdoctruyen.adapter.NovelAdapter;
import com.example.appdoctruyen.object.Novel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FollowActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;
    private List<Novel> favoriteNovelsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);

        recyclerView = findViewById(R.id.recyclerviewfollow);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoriteNovelsList = new ArrayList<>();
        adapter = new FavoriteAdapter(this, favoriteNovelsList, (novel, position) -> {
            Intent intent = new Intent(FollowActivity.this, NovelsInfoActivity.class);
            intent.putExtra("novelId", novel.getId());
            intent.putExtra("tomtat", novel.getTomtat());
            startActivity(intent);
        }, (novel, position) -> {
            removeFavoriteNovel(novel.getId(), position);
        }, (novel, position) -> {
            Intent intent = new Intent(FollowActivity.this, NovelsInfoActivity.class);
            intent.putExtra("novelId", novel.getId());
            intent.putExtra("tomtat", novel.getTomtat());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        fetchFavoriteNovels();
    }

    private void fetchFavoriteNovels() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("favorite");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    favoriteNovelsList.clear(); // Clear the list before adding new items
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String novelId = snapshot.getKey();
                        if (novelId != null) {
                            Log.d("FollowActivity", "Novel ID: " + novelId);
                            fetchNovelDetails(novelId);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(FollowActivity.this, "Failed to load favorites", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void fetchNovelDetails(String novelId) {
        DatabaseReference novelRef = FirebaseDatabase.getInstance().getReference("truyen").child(novelId);
        novelRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Novel novel = dataSnapshot.getValue(Novel.class);
                if (novel != null) {
                    novel.setId(novelId);
                    favoriteNovelsList.add(novel);
                    Log.d("FollowActivity", "Novel added: " + novel.getTentruyen());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(FollowActivity.this, "Failed to load novel details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFavoriteNovel(String novelId, int position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("favorite")
                    .child(novelId);
            userRef.removeValue().addOnSuccessListener(aVoid -> {
                favoriteNovelsList.remove(position);
                adapter.notifyItemRemoved(position);
                Toast.makeText(FollowActivity.this, "Đã xóa khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(FollowActivity.this, "Failed to remove from favorites", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
