package com.example.jaewonkim.tmapproject;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapTapi;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Jaewon Kim on 2016-05-18.
 */
public class FindDestination extends Activity {

    ListView listview;
    ListViewAdapter adapter;

    EditText editText;
    Button search_Btt;

    TMapTapi tMapTapi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poi_list);

        adapter = new ListViewAdapter();
        listview = (ListView)findViewById(R.id.listview1);
        listview.setAdapter(adapter);

        editText = (EditText)findViewById(R.id.editText);
        search_Btt = (Button)findViewById(R.id.searchBtt);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ListViewItem item = (ListViewItem)adapter.getItem(position);

                Intent intent = new Intent(FindDestination.this, MainActivity.class);
                intent.putExtra("lat", item.getLatitude());
                intent.putExtra("lon", item.getLongitude());

                startActivity(intent);
            }
        });
        tMapTapi = new TMapTapi(this);
        tMapTapi.setSKPMapAuthentication("e530567b-36ab-3770-b6a1-90056818d340");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void search(View v) {

        adapter.clear_list();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Test","enter run");
                String dst = editText.getText().toString();
                Log.d("Test", "dst: "+ dst);
                ArrayList POIItem = new ArrayList<TMapPOIItem>();

                TMapData tMapData = new TMapData();
                try {
                    POIItem.addAll(tMapData.findAllPOI(dst));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                }

                if(POIItem == null) {
                    Toast.makeText(getApplicationContext(), "There is no such place", Toast.LENGTH_LONG);
                }

                for (int i = 0; i < POIItem.size(); i++) {
                    TMapPOIItem temp = (TMapPOIItem)POIItem.get(i);
                    TMapPoint temp_point = temp.getPOIPoint();
                    adapter.addItem(temp.getPOIName(), temp_point.getLatitude(), temp_point.getLongitude());
//                    adapter.notifyDataSetInvalidated();
                    Log.d("Test", temp.getPOIName());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });

        thread.start();
    }


}
