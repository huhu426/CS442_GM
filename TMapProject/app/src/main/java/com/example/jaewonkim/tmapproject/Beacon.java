package com.example.jaewonkim.tmapproject;

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
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/**
 * Created by Hyunoo on 2016-05-26.
 */
public class Beacon {
    private BluetoothAdapter mBTAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    private BluetoothLeScanner mBTScanner;
    private BluetoothGattServer mGattServer;

    private Beacon(Context context){
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
            System.arraycopy(float2ByteArray(velocity),0,mData,16,4);

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

            if(mbyte[2]==0x0a && mbyte[3]==0x18) {
                //네비로부터 위험신호를 수신한 경우.
                /*******************************************************
                 사용자에게 위험신호 발생시키는 코드
                 ******************************************************/
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

    public void scanAdvertise(){
        if (mBTAdapter == null) {
            return;
        }
        if (mBTScanner == null) {
            mBTScanner = mBTAdapter.getBluetoothLeScanner();
        }
        else if (mBTScanner != null) {
            ScanSettings.Builder msetbuilder = new ScanSettings.Builder();
            msetbuilder.setReportDelay(0);
            msetbuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

            mBTScanner.startScan(null, msetbuilder.build(), mScanCallback);
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