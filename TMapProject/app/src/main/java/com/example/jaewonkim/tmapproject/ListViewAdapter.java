package com.example.jaewonkim.tmapproject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Jaewon Kim on 2016-05-18.
 */
public class ListViewAdapter  extends BaseAdapter {

    private ArrayList<ListViewItem> listViewItemList = new ArrayList<ListViewItem>();
    private static final long serialVersionUID = 1L;

    @Override
    public int getCount() {
        return listViewItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return listViewItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int pos = position;
        Context context = parent.getContext();

        if(convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_item, parent, false);
        }

        TextView titleTextView = (TextView) convertView.findViewById(R.id.textView1) ;
        TextView descTextView = (TextView) convertView.findViewById(R.id.textView2) ;

        // Data Set(listViewItemList)에서 position에 위치한 데이터 참조 획득
        ListViewItem listViewItem = listViewItemList.get(position);
        titleTextView.setSingleLine(true);
        descTextView.setSingleLine(true);

        // 아이템 내 각 위젯에 데이터 반영
        titleTextView.setText(listViewItem.getPOI_Name());
        descTextView.setText("<" + listViewItem.getLatitude() + ", " + listViewItem.getLongitude() + ">");

        return convertView;
    }

    public void addItem(String POI_Name, double Latitude, double Longitude) {
        ListViewItem item = new ListViewItem();

        item.setPOI_Name(POI_Name);
        item.setLatitude(Latitude);
        item.setLongitude(Longitude);
        listViewItemList.add(item);
    }

    public void clear_list() {
        listViewItemList.clear();
    }
}