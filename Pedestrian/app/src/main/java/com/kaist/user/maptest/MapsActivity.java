package com.kaist.user.maptest;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    final String TAG = "Maptest";
    final float THRESHOLD_DISTANCE_TO_ALERT = 5.0f; // 5 meters

    private SensorManager mSensorManager;
    private Sensor mAccSensor;
    private Sensor mRotationSensor;
    private PopupWindow popup;

    //for GPS tracking
    String mLatitude = "36.32138824", mLongitude = "127.41972351";
    String mInitialLatitude = "0", mInitialLongitude = "0";
    Location mInitialLocation = new Location("");
    Location mCurrentLocation = new Location("");
    Location mPreLocation = new Location("");
    LocationManager locationManager;
    LocationRequest locationRequest;

    //for checking movement
    int stepCount = 0;
    int preStepCount = 0;
    boolean isMoving = false;
    boolean popup_timing = false;
    double checkPoint = 0;
    float bearingOfCrossWalk = 0.0f;
    double sigma = 0;
    final int FILTER_BUF_NUM = 16;
    final int FILTER_NUM = 11;
    float magnitudeAcc[] = new float[FILTER_BUF_NUM];
    float gravity[]=new float[3];
    float rMat[] = new float[9];
    float orientation[] = new float[3];
    private float azimuth; // View to draw a compass
    private float radianAzimuth; // View to draw a compass

    //Crosswalk info
    ArrayList<Location> mCrosswalkPosition = new ArrayList<Location>();

    Beacon mBeaconManager;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
            // See https://g.co/AppIndexing/AndroidStudio for more information.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(AppIndex.API).build();
        }
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_FASTEST);

        //set temp crosswalk position
        Location temp = new Location("");
        temp.setLatitude(36.373719);
        temp.setLongitude(127.365111);
        mCrosswalkPosition.add(temp);
        temp = new Location("");
        temp.setLatitude(36.373665);
        temp.setLongitude(127.365061);
        mCrosswalkPosition.add(temp);

        bearingOfCrossWalk = mCrosswalkPosition.get(0).bearingTo(mCrosswalkPosition.get(1));
        if (bearingOfCrossWalk < 0)
            bearingOfCrossWalk += 360;
        Log.d(TAG, "crosswalk info distance  " + mCrosswalkPosition.get(0).distanceTo(mCrosswalkPosition.get(1))
                + "  bearingTo  " + bearingOfCrossWalk);

        popup = new PopupWindow();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        Location temp = new Location("");
        Log.d(TAG, "onMapReady size  " + mCrosswalkPosition.size());
        for (int i=0; i < mCrosswalkPosition.size(); i++) {
            temp = mCrosswalkPosition.get(i);
            addMarker("green", temp.getLatitude(), temp.getLongitude());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.d(TAG, "onConnected ");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "onConnected permission problem ");
            return;
        }
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates ");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "startLocationUpdates permission problem ");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended ");

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ");

        if (location != null) {
            double lat = location.getLatitude();
            double log = location.getLongitude();
            mLatitude = String.valueOf(lat);
            mLongitude = String.valueOf(log);
            if(mInitialLatitude.equals("0")) {
                mInitialLocation.setLatitude(lat);
                mInitialLocation.setLongitude(log);
                mInitialLatitude = mLatitude;
                mInitialLongitude = mLongitude;
                mPreLocation.setLatitude(lat);
                mPreLocation.setLongitude(log);
                LatLng initPos = new LatLng(lat, log);
                mMap.addMarker(new MarkerOptions().position(initPos));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initPos, 18));
            }

            mCurrentLocation.setLatitude(lat);
            mCurrentLocation.setLongitude(log);
            Log.d(TAG, "onLocationChanged= current mLatitude " + mCurrentLocation.getLatitude() + "  mLongitude  " + mCurrentLocation.getLongitude());

            if (mCurrentLocation != null && mInitialLocation != null) {

                addMarker("blue", mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

                if((mCrosswalkPosition.get(0).distanceTo(mCurrentLocation) < THRESHOLD_DISTANCE_TO_ALERT)||(mCrosswalkPosition.get(1).distanceTo(mCurrentLocation) < THRESHOLD_DISTANCE_TO_ALERT)) {
                    //if user is close to crosswalk, show popup
                    if(!popup.isShowing() && !popup_timing) { // if popup window is not showing and on time
                        // show popup window ~ after hands up, popup must be dismissed
                        popup_timing = true; // later, when user is far enough from both side of crosswalk, pop_timing turn to false.
                        LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                        View v = inflater.inflate(R.layout.popup_window, null);
                        popup.setContentView(v);
                        popup.setWindowLayoutMode(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
                        popup.setTouchable(true);
                        popup.setFocusable(true);
                        popup.showAtLocation(v, Gravity.CENTER, 0, 0);
                        popup.setOutsideTouchable(true);
                        popup.setBackgroundDrawable(new ColorDrawable());
                        popup.showAsDropDown(v);
                    }
                }

                if(preStepCount != stepCount) {
                    isMoving = true;
                    Log.d(TAG, "Moving  preStepCount  " + preStepCount + "  stepCount  " + stepCount);
                } else {
                    isMoving = false;
                }
                preStepCount = stepCount;

                if (isMoving) { //Send BLE packet when the pedestrian is walking
                    Location temp = new Location("");
                    for (int i=0; i < mCrosswalkPosition.size(); i++) {
                        temp = mCrosswalkPosition.get(i);
                        float bearingMove =  mPreLocation.bearingTo(mCurrentLocation);
                        if (bearingMove < 0)
                            bearingMove += 360;
                        Log.d(TAG, "distanceTo = " + temp.distanceTo(mCurrentLocation) + "  bearingTo = " + bearingMove);

                        //Send BLE packet when the distance between Pedestrian and crosswalk is lower than 5 meters
                        if (temp.distanceTo(mCurrentLocation) < THRESHOLD_DISTANCE_TO_ALERT) {
                            if (bearingMove > bearingOfCrossWalk - 30 && bearingMove < bearingOfCrossWalk + 30) {//check direction
                                //mBeaconManager.startBeaconAdvertise(1, 0.0f, 0.0f, 0.0f, 0.0f);//test
                                addMarker("yellow", mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                            }
                        } else {
                            //mBeaconManager.stopAdvertise();
                        }

                    }
                    mPreLocation.setLatitude(mCurrentLocation.getLatitude());
                    mPreLocation.setLongitude(mCurrentLocation.getLongitude());
                }

            }
        }

    }

    public void addMarker(String color, double lat, double log )
    {
        BitmapDescriptor bitmapMarker;
        if (color.equals("blue"))
            bitmapMarker = BitmapDescriptorFactory.fromResource(R.drawable.blue_dot);
        else if (color.equals("green"))
            bitmapMarker = BitmapDescriptorFactory.fromResource(R.drawable.green_dot);
        else if (color.equals("yellow"))
            bitmapMarker = BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot);
        else
            bitmapMarker = BitmapDescriptorFactory.fromResource(R.drawable.blue_dot);
        LatLng curPos = new LatLng(lat, log);
        mMap.addMarker(new MarkerOptions().position(curPos).icon(bitmapMarker));
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed ");

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float alpha = 0.8f;
            double sqrt;
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            sqrt = Math.sqrt(gravity[0]*gravity[0] + gravity[1]*gravity[1] + gravity[2] * gravity[2]);

            calculateSigma((float) sqrt); // check step count
        }
        else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector( rMat, event.values );
            radianAzimuth = SensorManager.getOrientation( rMat, orientation)[0];
            azimuth = (int) (Math.toDegrees(radianAzimuth)+ 360 ) % 360; // Rotation of Z-axis
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged ");
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume ");
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            Log.d(TAG, "mGoogleApiClient Connected ");
            startLocationUpdates();
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart ");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop ");
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    public void calculateSigma(float a) {
        float sum = 0.0f;
        float localMean = 0.0f;
        int i = 0;
        for (i = 1 ; i < FILTER_BUF_NUM; i++) {
            magnitudeAcc[i - 1] = magnitudeAcc[i];
        }
        magnitudeAcc[FILTER_BUF_NUM-1] = a;

        for (i = FILTER_BUF_NUM - 1; i >= 5; i--) {
            sum += magnitudeAcc[i];
        }
        localMean = sum / FILTER_NUM;

        sum = 0;
        for (i = FILTER_BUF_NUM - 1; i > FILTER_BUF_NUM - 6; i--) {
            sum += Math.pow((magnitudeAcc[i - 6] - localMean), 2);
            sum += Math.pow((magnitudeAcc[i] - localMean), 2);
        }
        sum += Math.pow((magnitudeAcc[10] - localMean), 2);

        sigma = Math.sqrt((double) (sum / FILTER_NUM));

        if (checkPoint >= 1.0 && sigma < 0.2) {
            stepCount++;
            checkPoint = 0;
        }

        if(sigma>1.0)
            checkPoint = 1.0;

        //Log.d(TAG, "sigma\t" + sigma + "       " + stepCount);
    }

}
