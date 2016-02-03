package com.alyj.smartconfort;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.alyj.smartconfort.flowerAPI.FlowerPowerConstants;
import com.alyj.smartconfort.flowerAPI.ValueMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.speech.SpeechRecognizer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;






@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends ActionBarActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothManager bluetoothManager;
    public static boolean INUSE=false;
    private TextView txtTemperature;
    private TextView txtLuminosite;
    private TextView txtHumidite;
    private TextView returnedText;
    private ToggleButton toggleButton;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private List<String> propertiesToDisplay=Arrays.asList(FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE,FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT,FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE);
    private BluetoothGattService service;
    ValueMapper valueMapper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    txtTemperature=(TextView)findViewById(R.id.txtTemperature);
        txtLuminosite=(TextView)findViewById(R.id.txtLuminosite);
        txtHumidite=(TextView)findViewById(R.id.txtHumidite);
            bluetoothManager=    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(LeOldScanner);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(LeOldScanner);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(LeOldScanner);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }



        @Override
        protected void onResume() {
            super.onResume();
            valueMapper=ValueMapper.getInstance(this);
            INUSE=true;
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                if (Build.VERSION.SDK_INT >= 21) {
                    mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build();
                    filters = new ArrayList<>();
                }
                scanLeDevice(true);
            }
        }

        @Override
        protected void onPause() {
            super.onPause();
            INUSE=false;
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                scanLeDevice(false);
            }
        }

        @Override
        protected void onDestroy() {
            if (mGatt == null) {
                return;
            }
            mGatt.close();
            mGatt = null;
            super.onDestroy();
        }



        private ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.i("callbackType, Found", String.valueOf(callbackType));
                BluetoothDevice btDevice = result.getDevice();
                connectToDevice(btDevice);
            }


        };

        private BluetoothAdapter.LeScanCallback LeOldScanner =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi,
                                         byte[] scanRecord) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if(device.getName().equalsIgnoreCase("Flower power AAB2")){
                                    Log.i("FOund ", device.getName());
                                    connectToDevice(device);
                                }

                            }
                        });
                    }
                };

        public void connectToDevice(BluetoothDevice device) {
            if (mGatt == null) {
                mGatt = device.connectGatt(this, false, gattCallback);
                scanLeDevice(false);
            }
        }

        private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Log.i("onConnectionStateChange", "Status: " + status);
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        Log.i("gattCallback", "STATE_CONNECTED");
                        gatt.discoverServices();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        if(INUSE){
                            gatt.connect();
                        }
                        Log.e("gattCallback", "STATE_DISCONNECTED");
                        break;
                    default:
                        Log.e("gattCallback", "STATE_OTHER");
                }

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                List<BluetoothGattService> services = gatt.getServices();
                Log.i("onServicesDiscovered", services.toString());
                service=gatt.getService(UUID.fromString(FlowerPowerConstants.SERVICE_UUID_FLOWER_POWER));


                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Thread.sleep(2000);
                                for (String property : propertiesToDisplay) {
                                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(property));
                                    mGatt.readCharacteristic(characteristic);
                                    Thread.sleep(200);
                                    if (characteristic != null)
                                        displayData(characteristic);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

            }
private void displayData(BluetoothGattCharacteristic characteristic){
    switch (characteristic.getUuid().toString()){
        case FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE:
            txtTemperature.setText(""+valueMapper.mapTemperature(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
            break;
        case FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT:
            txtLuminosite.setText(""+valueMapper.mapSunlight(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
            break;
        case FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE:
            txtHumidite.setText(""+valueMapper.mapSoilMoisture(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16,0)));
            break;
        default:break;

    }
}
            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic
                                                     characteristic, int status) {
                Log.i("onCharacteristicRead", Arrays.toString(characteristic.getValue()));

            }
        };


}








