package com.example.appdoctruyen.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.object.Chapter;

import java.util.List;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private List<Chapter> chapterList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Chapter chapter, int position);
    }

    public ChapterAdapter(List<Chapter> chapterList, OnItemClickListener onItemClickListener) {
        this.chapterList = chapterList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        holder.titleTextView.setText(chapter.getTitle());
        holder.uploadDateTextView.setText(chapter.getNgayup());
        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(chapter, position));

    }

    @Override
    public int getItemCount() {
        return chapterList.size();
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView uploadDateTextView;

        public ChapterViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.chapterText);
            uploadDateTextView = itemView.findViewById(R.id.textUploadDate);
        }
    }


    public void updateList(List<Chapter> newList) {
        this.chapterList = newList;
        notifyDataSetChanged();
    }
}