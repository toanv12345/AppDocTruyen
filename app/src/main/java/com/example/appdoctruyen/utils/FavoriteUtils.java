// FavoriteUtils.java
package com.example.appdoctruyen.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.appdoctruyen.object.Novel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class FavoriteUtils {

    private static final String PREFS_NAME = "FavoriteNovels";
    private static final String FAVORITE_LIST_KEY = "FavoriteList";

    public static void saveFavoriteNovels(Context context, List<Novel> favoriteNovels) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(favoriteNovels);
        editor.putString(FAVORITE_LIST_KEY, json);
        editor.apply();
        Log.d("FavoriteUtils", "Saved favorite novels: " + json);

        // Save to Firebase Realtime Database
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("favorite");
            userRef.setValue(favoriteNovels);
        }
    }

    public static List<Novel> loadFavoriteNovels(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(FAVORITE_LIST_KEY, null);
        Type type = new TypeToken<List<Novel>>() {}.getType();
        List<Novel> favoriteNovels = gson.fromJson(json, type);
        Log.d("FavoriteUtils", "Loaded favorite novels: " + json);
        return favoriteNovels;
    }
}
