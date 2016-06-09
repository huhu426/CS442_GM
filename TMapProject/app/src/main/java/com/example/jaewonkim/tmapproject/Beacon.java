package com.example.jaewonkim.tmapproject;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Hyunoo on 2016-05-26.
 */
public class Beacon {
    private BluetoothAdapter mBTAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private BluetoothLeScanner mBTScanner;
    private BluetoothGattServer mGattServer;
    private View view;
    private int id;
    private boolean is_popup = false;
    private PopupWindow popup;

    public Beacon(View v){
        view = v;
        Context context = v.getContext();
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if ((mBTAdapter == null) || (!mBTAdapter.isEnabled())) {
            Toast.makeText(context, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    public void setup_popup() {
        Context context = view.getContext();
        popup = new PopupWindow(view);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.popup_window, null);
        popup.setContentView(view);
        popup.setWindowLayoutMode(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        popup.setTouchable(true);
        popup.setFocusable(true);
        popup.showAtLocation(view, Gravity.CENTER, 0, 0);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new ColorDrawable());
        popup.showAsDropDown(view);

        new CountDownTimer(3000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFinish() {
                // TODO Auto-generated method stub
                if(popup.isShowing())
                    popup.dismiss();
                is_popup = false;
            }
        }.start();
    }

    private static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        builder.setConnectable(connectable);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return builder.build();
    }

    private static AdvertiseData createBeaconAdvertiseData(byte[] mbyte) {
        byte[] mData = new byte[mbyte.length];
        ByteBuffer bb = ByteBuffer.wrap(mData);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(mbyte);

        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.addServiceData(new ParcelUuid(UUID.fromString("0000640a-0000-1000-8000-00805f9b34fb")), mData);
        AdvertiseData adv = builder.build();
        return adv;
    }
    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
            if (settingsInEffect != null) {
                Log.d("TAG", "onStartSuccess TxPowerLv="
                        + settingsInEffect.getTxPowerLevel()
                        + " mode=" + settingsInEffect.getMode()
                        + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.d("TAG", "onStartSuccess, settingInEffect is null");
            }
        }

        public void onStartFailure(int errorCode) {
            Log.d("TAG", "onStartFailure errorCode=" + errorCode);
        }
    };

    private static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }
    private static byte [] int2ByteArray (int value)
    {
        return ByteBuffer.allocate(4).putInt(value).array();
    }
    public void startBeaconAdvertise(int index,float lattitude,float longitude,float degree,float velocity) {
        if (mBTAdapter == null) {
            return;
        }
        if (mBTAdvertiser == null) {
            mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
        }
        if (mBTAdvertiser != null) {
            byte[] mData = new byte[20];

            System.arraycopy(int2ByteArray(index),0,mData,0,4);
            System.arraycopy(float2ByteArray(lattitude),0,mData,4,4);
            System.arraycopy(float2ByteArray(longitude), 0, mData, 8, 4);
            System.arraycopy(float2ByteArray(degree),0,mData,12,4);
            System.arraycopy(float2ByteArray(velocity), 0, mData, 16, 4);

            mBTAdvertiser.startAdvertising(
                    createAdvSettings(false, 0),
                    createBeaconAdvertiseData(mData),
                    mAdvCallback);
        }
    }

    public void stopAdvertise() {
        if (mGattServer != null) {
            mGattServer.clearServices();
            mGattServer.close();
            mGattServer = null;
        }
        if (mBTAdvertiser != null) {
            mBTAdvertiser.stopAdvertising(mAdvCallback);
            mBTAdvertiser = null;
        }
    }

    public float bytesToFloat(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
        //return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    public long bytesTolong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    public int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
        //return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {

            super.onScanResult(callbackType, result);
            byte[] mbyte = result.getScanRecord().getBytes();
            int index = bytesToInt(Arrays.copyOfRange(mbyte, 4, 8));
            long advTime = bytesTolong(Arrays.copyOfRange(mbyte, 8, 16));

//            Log.d("Scan", "Scan Success" + mbyte[2] + mbyte[3]);
//            Toast.makeText(context, "Scan Success" + mbyte[2] + mbyte[3], Toast.LENGTH_SHORT);

            if(mbyte[2]==0x0a && mbyte[3]==0x64) {
                if((id == index)&&(!is_popup)) {
                    long current_time = System.currentTimeMillis();
                    long diff = current_time - advTime;
                    Log.d("Scan", "Snd Time: "+advTime + "Rcv Time: "+current_time);
                    Log.d("Scan", "Difference: " + diff);
//                    Toast.makeText(context, "WATCH OUT!", Toast.LENGTH_SHORT).show();
                    Log.d("Scan", "PopUP");
                    setup_popup();
                    is_popup = true;
                }
//                else
//                    Log.d("Scan", "Ignore");
            }
            else if(mbyte[2]==0x0a && mbyte[3]==0xe7) {
                /* 주위에 네비가 있는 정보를 수신한 경우 */
                /*******************************************************
                 beacon 호출 간격을 줄이는 코드
                 ******************************************************/
            }

        }

        @Override
        public void onScanFailed(int errorCode)
        {
            super.onScanFailed(errorCode);
        }
    };

    public void scanAdvertise(int id){
        this.id = id;
        if (mBTAdapter == null) {
            Log.d("Scan", "mBTAdapter == null");
            return;
        }
        if (mBTScanner == null) {
            Log.d("Scan", "mBTAdapter == null");
            mBTScanner = mBTAdapter.getBluetoothLeScanner();
        }
        if (mBTScanner != null) {
            ScanSettings.Builder msetbuilder = new ScanSettings.Builder();
            msetbuilder.setReportDelay(0);
            msetbuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

            mBTScanner.startScan(null, msetbuilder.build(), mScanCallback);
            Log.d("Scan", "Scan Start");
        }
    }

    public void stopScan() {
        if (mGattServer != null) {
            mGattServer.clearServices();
            mGattServer.close();
            mGattServer = null;
        }
        if (mBTScanner != null) {
            mBTScanner.stopScan(mScanCallback);
            mBTScanner = null;
        }
    }
}