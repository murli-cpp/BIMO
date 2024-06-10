package com.example.project;
import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by paetztm on 2/6/2017.
 */
public class SingerAdapter extends RecyclerView.Adapter<SingerAdapter.ViewHolder> {
    private final LayoutInflater layoutInflater;
    private final List<Singer> singerList;
    private final int rowLayout;

    public SingerAdapter(LayoutInflater layoutInflater, List<Singer> singerList, @LayoutRes int rowLayout) {
        this.layoutInflater = layoutInflater;
        this.singerList = singerList;
        this.rowLayout = rowLayout;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = layoutInflater.inflate(rowLayout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Singer singer = singerList.get(position);
        holder.fullName.setText(singer.getName());
    }

    @Override
    public int getItemCount() {
        return singerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView fullName;

        public ViewHolder(View view) {
            super(view);
            fullName = view.findViewById(R.id.full_name_tv);
        }
    }
}