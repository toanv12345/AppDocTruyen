// NovelAdapter.java
package com.example.appdoctruyen.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdoctruyen.R;
import com.example.appdoctruyen.object.Novel;

import java.util.List;

public class NovelAdapter extends BaseAdapter {
    private Context context;
    private List<Novel> novelList;

    public NovelAdapter(Context context, List<Novel> novelList) {
        this.context = context;
        this.novelList = novelList;
    }

    @Override
    public int getCount() {
        return novelList.size();
    }

    @Override
    public Object getItem(int position) {
        return novelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_truyen, parent, false);
        }

        Novel novel = novelList.get(position);
        TextView titleTextView = convertView.findViewById(R.id.txtTenTruyen);
        ImageView imageView = convertView.findViewById(R.id.imgAnhTruyen);

        titleTextView.setText(novel.getTentruyen());

        Glide.with(context)
                .load(novel.getLinkanh())
                .into(imageView);

        return convertView;
    }

    public void updateList(List<Novel> newList) {
        this.novelList = newList;
        notifyDataSetChanged();
    }
}