package com.example.appdoctruyen.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.object.Chapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {

    private List<Chapter> chapterList;
    private OnItemClickListener listener;
    private Set<String> readChapterIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(Chapter chapter, int position);
    }

    public ChapterAdapter(List<Chapter> chapterList, OnItemClickListener listener) {
        this.chapterList = chapterList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chapter, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = chapterList.get(position);
        holder.chapterTitle.setText(chapter.getTitle());

        // Kiểm tra xem chapter đã được đọc chưa
        if (readChapterIds.contains(chapter.getId())) {
            // Nếu đã đọc, hiển thị nghiêng và màu xám
            holder.chapterTitle.setTypeface(holder.chapterTitle.getTypeface(), Typeface.ITALIC);
            holder.chapterTitle.setTextColor(Color.GRAY); // Đổi màu chữ sang xám
        } else {
            // Nếu chưa đọc, hiển thị bình thường và màu đen
            holder.chapterTitle.setTypeface(holder.chapterTitle.getTypeface(), Typeface.NORMAL);
            holder.chapterTitle.setTextColor(Color.BLACK); // Màu chữ đen mặc định
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(chapter, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return chapterList.size();
    }

    public void updateList(List<Chapter> newList) {
        this.chapterList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    // Cập nhật danh sách chapter đã đọc
    public void setReadChapters(Set<String> readChapterIds) {
        if (readChapterIds == null) {
            this.readChapterIds = new HashSet<>();
        } else {
            this.readChapterIds = new HashSet<>(readChapterIds);
        }
        notifyDataSetChanged();
    }

    // Xóa một chapter khỏi danh sách đã đọc
    public void removeReadChapter(String chapterId) {
        if (readChapterIds.contains(chapterId)) {
            readChapterIds.remove(chapterId);
            notifyDataSetChanged();
        }
    }

    // Lấy vị trí của chapter theo ID
    public int getPositionById(String chapterId) {
        for (int i = 0; i < chapterList.size(); i++) {
            if (chapterId != null && chapterId.equals(chapterList.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterTitle;

        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            chapterTitle = itemView.findViewById(R.id.tv_chapter_title);
        }
    }
}