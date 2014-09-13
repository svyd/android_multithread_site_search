package com.blogspot.vsvydenko.multithread_site_searcher.ui;

import com.blogspot.vsvydenko.multithread_site_searcher.R;
import com.blogspot.vsvydenko.multithread_site_searcher.entity.UrlItem;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by vsvydenko on 07.09.14.
 */
public class UrlAdapter extends BaseAdapter {

    private Context mContext;
    private List<UrlItem> mUrlItems;

    public UrlAdapter(Context context, List<UrlItem> items) {
        this.mContext = context;
        this.mUrlItems = items;
    }
    @Override
    public int getCount() {
        return mUrlItems != null ? mUrlItems.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mUrlItems != null ? mUrlItems.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.list_item_url, parent, false);
        }

        TextView txtUrlName = ViewHolder.get(convertView, R.id.txtUrlName);
        TextView txtUrlStatus = ViewHolder.get(convertView, R.id.txtUrlStatus);
        TextView txtUrlFound = ViewHolder.get(convertView, R.id.txtUrlFoundCounter);

        UrlItem mUrlItem = (UrlItem) getItem(position);

        txtUrlName.setText(mUrlItem.getName());
        if (mUrlItem.getStatus().contains("Error")) {
            txtUrlStatus.setTextColor(Color.RED);
        } else {
            txtUrlStatus.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
        }
        txtUrlStatus.setText(mUrlItem.getStatus());
        txtUrlFound.setText(String.valueOf(mUrlItem.getFoundNumber()));

        return convertView;
    }

    public static class ViewHolder {

        // I added a generic return type to reduce the casting noise in client code
        @SuppressWarnings("unchecked")
        public static <T extends View> T get(View view, int id) {
            SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
            if (viewHolder == null) {
                viewHolder = new SparseArray<View>();
                view.setTag(viewHolder);
            }
            View childView = viewHolder.get(id);
            if (childView == null) {
                childView = view.findViewById(id);
                viewHolder.put(id, childView);
            }
            return (T) childView;
        }
    }
}
