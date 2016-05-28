package com.example.jaewonkim.tmapproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback, TMapView.OnLongClickListenerCallback {

    final int crosswalk_id = 1;

    TMapView tmapview= null;
    TMapGpsManager tmapgps;
    TMapPoint current_point = null;
    TMapPoint end_point;
    TMapPoint crosswalk;
    ArrayList arPoint;
    String arr;

    Beacon mBeaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        double lat = intent.getDoubleExtra("lat", 0);
        double lon = intent.getDoubleExtra("lon", 0);
        end_point = new TMapPoint(lat, lon);

        tmap_init();
        tmapgps = new TMapGpsManager(this);
        current_point = new TMapPoint(0, 0);
        mBeaconManager = new Beacon(getApplicationContext());
        crosswalk = new TMapPoint(36.373692, 127.365086);
        draw_point(crosswalk);
        arPoint = new ArrayList<TMapPoint>();

        mBeaconManager.scanAdvertise(crosswalk_id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tmapgps.CloseGps();
    }

    @Override
    protected void onResume() {
        super.onResume();

        tmapgps.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tmapgps.setLocationCallback();
        tmapgps.setMinTime(1000);
        tmapgps.OpenGps();


    }

    public void tmap_init() {
        tmapview = (TMapView)findViewById(R.id.mapView);
        tmapview.setSKPMapApiKey("e530567b-36ab-3770-b6a1-90056818d340");
        tmapview.setCenterPoint(127.379678, 36.369720);
        tmapview.setLocationPoint(127.379678, 36.369720);
        tmapview.setIconVisibility(true);
        tmapview.setZoomLevel(15);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);
        tmapview.setCompassMode(true);
        tmapview.setTrackingMode(true);
    }

    public void draw_path() {
        TMapData tmapdata = new TMapData();
        arPoint = new ArrayList<TMapPoint>();

        try {
            tmapdata.findPathData(current_point, end_point, new TMapData.FindPathDataListenerCallback() {
                @Override
                public void onFindPathData(TMapPolyLine polyLine) {
                    tmapview.addTMapPath(polyLine);
                    arPoint.addAll(polyLine.getLinePoint());
                    Log.d("arPoint", arPoint.toString());
//                    draw_point();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw_point(TMapPoint p) {

        TMapMarkerItem tItem = new TMapMarkerItem();

        tItem.setTMapPoint(p);
        tItem.setVisible(TMapMarkerItem.VISIBLE);
        tItem.setPosition((float) 0.5, (float) 1.0);
        tmapview.addMarkerItem(""+crosswalk_id, tItem);

//        for(int i = 0 ; i < arPoint.size() ; i++) {
//
//            TMapPoint p = (TMapPoint) arPoint.get(i);
//            TMapMarkerItem tItem = new TMapMarkerItem();
//
//            tItem.setTMapPoint(p);
//            tItem.setName(""+i);
//            tItem.setVisible(TMapMarkerItem.VISIBLE);
//            tItem.setPosition((float) 0.5, (float) 1.0);
//            tmapview.addMarkerItem(""+i, tItem);
//        }
    }

    @Override
    public void onLocationChange(final Location location) {
        current_point.setLatitude(location.getLatitude());
        current_point.setLongitude(location.getLongitude());

        final double lat = current_point.getLatitude();
        final double lon = current_point.getLongitude();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                checkRange(lat, lon);
            }
        });
        thread.start();

        tmapview.setCenterPoint(lon, lat);
        tmapview.setLocationPoint(lon, lat);

        draw_path();
    }
    @Override
    public void onLongPressEvent(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, final TMapPoint tMapPoint) {

        Log.d("TestPoint", "Lat: " + tMapPoint.getLatitude() + " Lon: " + tMapPoint.getLongitude());
        //Lat: 36.37343974435254 Lon: 127.36568212509155
        if(arrayList.size() == 0) {
            Log.d("Marker", "No Marker");
            dialog_no_marker(tMapPoint);
        }
        else {
            Log.d("Marker", "" + arrayList.size());
            dialog_yes_marker(arrayList);
        }
    }

    public void dialog_no_marker(TMapPoint tMapPoint) {
        final TMapPoint point = tMapPoint;
        new AlertDialog.Builder(this)
                .setMessage("Do you want to add point of crosswalk?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        arPoint.add(point);
                        draw_point(point);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }
    public void dialog_yes_marker(ArrayList<TMapMarkerItem> arrayList) {

        TMapMarkerItem temp = arrayList.get(0);
        final String markerID = temp.getID();
        final TMapPoint p = temp.getTMapPoint();

        new AlertDialog.Builder(this)
                .setMessage("Do you want to delete point of crosswalk?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tmapview.removeMarkerItem(markerID);
                        boolean success = arPoint.remove(p);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        }).show();
    }

    public void checkRange(double lat, double lon) { // check whether the crosswalk is in range
        boolean in = false;

        double c_lat = crosswalk.getLatitude();
        double c_lon = crosswalk.getLongitude();
        double dist = measure(lat, lon, c_lat, c_lon);
        if(dist < 50) {
//            //do something about BLE
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getApplicationContext(), "CrossWalk!", Toast.LENGTH_LONG).show();
//                }
//            });
        }

    }

    public double measure(double lat1, double lon1, double lat2, double lon2){  // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = (lat2 - lat1) * Math.PI / 180;
        double dLon = (lon2 - lon1) * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
    }

//    public void give_warning() { // run as thread, give warning when BLE signal is detected && has same crosswalk_id
//        mBeaconManager.scanAdvertise();
//        while(true) {
//            // scanning BLE
//            // if it receives ble warning msg,
//            // and if given id is same with crosswalk_id,
//            // give warning msg like Toast or something like that
//        }
//    }
}
