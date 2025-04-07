// FavoriteAdapter.java
package com.example.appdoctruyen.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdoctruyen.R;
import com.example.appdoctruyen.object.Novel;
import com.example.appdoctruyen.utils.FavoriteUtils;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private List<Novel> favoriteNovels;
    private OnItemClickListener listener;
    private OnDeleteClickListener deleteListener;
    private OnDeleteClickListener imageClickListener;
    private Context context;

    public FavoriteAdapter(Context context, List<Novel> favoriteNovels, OnItemClickListener listener, OnDeleteClickListener deleteListener, OnDeleteClickListener imageClickListener) {
        this.context = context;
        this.favoriteNovels = favoriteNovels;
        this.listener = listener;
        this.deleteListener = deleteListener;
        this.imageClickListener = imageClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_follow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Novel novel = favoriteNovels.get(position);
        holder.title.setText(novel.getTentruyen());
        Glide.with(context).load(novel.getLinkanh()).into(holder.imageView);

        View.OnClickListener clickListener = v -> listener.onItemClick(novel, position);
        holder.title.setOnClickListener(clickListener);
        holder.imageView.setOnClickListener(v -> imageClickListener.onDeleteClick(novel, position));
        holder.deleteButton.setOnClickListener(v -> deleteListener.onDeleteClick(novel, position));
    }

    @Override
    public int getItemCount() {
        return favoriteNovels.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView imageView;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.favorite_title);
            imageView = itemView.findViewById(R.id.img_follow);
            deleteButton = itemView.findViewById(R.id.follow_delete);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Novel novel, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Novel novel, int position);

    }

}
