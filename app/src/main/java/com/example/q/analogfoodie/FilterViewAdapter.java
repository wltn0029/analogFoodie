package com.example.q.analogfoodie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FilterViewAdapter extends RecyclerView.Adapter<FilterViewAdapter.ViewHolder> {
    private static final String TAG = "RecyclerViewAdapter";
    private Context mContext;
    public interface OnFilterSelected{
        void onFilterClicked(FilterSelection filterSelection);
    }
    private List<FilterSelection> filters;
    private final OnFilterSelected listener;


    public FilterViewAdapter(List<FilterSelection> filters, OnFilterSelected listener, Context context) {
        this.filters = filters;
        this.listener = listener;
        mContext = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        CircleImageView image;
        TextView name;
        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_view);
            name = itemView.findViewById(R.id.name);
        }
    }

    @Override
    public FilterViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.filter_sample, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");
        final FilterSelection filterSelection = filters.get(position);
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(),R.drawable.sample);
        holder.name.setText(filterSelection.name);
        Glide.with(mContext)
                .asBitmap()
                .load(filters.get(position).filter.apply(bitmap))
                .into(holder.image);
        Log.d("***check filter",filterSelection.name);
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onFilterClicked(filterSelection);
                Log.d(TAG, "onClick: clicked on an image: " + filters.get(position));
                Toast.makeText(mContext, filters.get(position).name, Toast.LENGTH_SHORT).show();
            }
        });

       }
    @Override
    public int getItemCount() {
        return filters.size();
    }

}
