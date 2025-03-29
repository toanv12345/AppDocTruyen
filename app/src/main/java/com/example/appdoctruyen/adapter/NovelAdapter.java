// NovelAdapter.java
package com.example.appdoctruyen.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdoctruyen.R;
import com.example.appdoctruyen.object.Novel;

import java.util.List;

public class NovelAdapter extends RecyclerView.Adapter<NovelAdapter.NovelViewHolder> {
    private List<Novel> novelList;

    public NovelAdapter(List<Novel> novelList) {
        this.novelList = novelList;
    }

    @NonNull
    @Override
    public NovelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_truyen, parent, false);
        return new NovelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NovelAdapter.NovelViewHolder holder, int position) {
        Novel novel = novelList.get(position);
        holder.titleTextView.setText(novel.getTenTruyen());
    }

    @Override
    public int getItemCount() {
        return novelList.size();
    }

    public static class NovelViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;

        public NovelViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.txtTenTruyen);
        }
    }

    public void updateList(List<Novel> newList) {
        this.novelList = newList;
        notifyDataSetChanged();
    }
}