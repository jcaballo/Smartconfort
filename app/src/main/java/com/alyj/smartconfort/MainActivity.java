package com.alyj.smartconfort;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.alyj.smartconfort.flowerAPI.FlowerPowerConstants;
import com.alyj.smartconfort.flowerAPI.ValueMapper;
import com.alyj.smartconfort.model.EnglishNumberToText;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends Activity implements
        RecognitionListener {

    private static final long SCAN_PERIOD = 10000;
    private static final String KWS_SEARCH = "wakeup";
    private static final String KEYPHRASE = "wake up";
    private static final String MENU_SEARCH = "menu";
    private static final String TEMPERATURE = "temperature";
    private static final String BRIGHTNESS = "brightness";
    private static final String HUMIDITY = "humidity";
    public static boolean INUSE = false;
    ValueMapper valueMapper;
    private String inUse = "";
    private long number = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothGatt mGatt;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothManager bluetoothManager;
    private TextView txtTemperature;
    private TextView txtLuminosite;
    private TextView txtHumidite;
    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private BluetoothGattService service;
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
                    if (INUSE) {
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
            service = gatt.getService(UUID.fromString(FlowerPowerConstants.SERVICE_UUID_FLOWER_POWER));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /** Pour une mis à jour automatique,  on tourne en boucle **/
                    while (true) {
                        try {
                            Thread.sleep(2000);
                         System.err.println("service "+service.getUuid());
                            /*for (String property : propertiesToDisplay) {
                                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(property));
                                if (characteristic != null){
                                    mGatt.readCharacteristic(characteristic);
                                    *//** Impossible de lire deux fois successivement, donc un sleep s'impose **//*
                                    Thread.sleep(200);
                                }

                            }*/
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            switch (characteristic.getUuid().toString()) {
                case FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE:
                    txtTemperature.setText("" + valueMapper.mapTemperature(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
                    break;
                case FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT:
                    txtLuminosite.setText("" + valueMapper.mapSunlight(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
                    break;
                case FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE:
                    txtHumidite.setText("" + valueMapper.mapSoilMoisture(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0)));
                    break;
                default:
                    break;

            }
            Log.i("onCharacteristicRead", Arrays.toString(characteristic.getValue()));

        }
    };
    private HashMap<String, Integer> captions;
    private BluetoothAdapter.LeScanCallback LeOldScanner =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device.getName()!=null && device.getName().equalsIgnoreCase("Flower power AAB2")) {
                                Log.i("FOund ", device.getName());
                                connectToDevice(device);
                            }

                        }
                    });
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.keyphrase);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(TEMPERATURE, R.string.temperature);

        ((TextView) findViewById(R.id.textView1))
                .setText("Préparation de la reconaissance vocale");
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.textView1))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();


        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        txtTemperature = (TextView) findViewById(R.id.txtTemperature);
        txtLuminosite = (TextView) findViewById(R.id.txtLuminosite);
        txtHumidite = (TextView) findViewById(R.id.txtHumidite);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                        mBluetoothAdapter.stopLeScan(LeOldScanner);
                }
            }, SCAN_PERIOD);
                mBluetoothAdapter.startLeScan(LeOldScanner);
        } else {
                mBluetoothAdapter.stopLeScan(LeOldScanner);
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                        // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)

                        // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-32f)

                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(TEMPERATURE, digitsGrammar);

        recognizer.addGrammarSearch(HUMIDITY, digitsGrammar);

        recognizer.addGrammarSearch(BRIGHTNESS, digitsGrammar);

    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.textView1)).setText(caption);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(TEMPERATURE)) {
            inUse = TEMPERATURE;
            switchSearch(TEMPERATURE);
        } else if (text.equals(BRIGHTNESS)) {
            inUse = BRIGHTNESS;
            switchSearch(BRIGHTNESS);
        } else if (text.equals(HUMIDITY)) {
            inUse = HUMIDITY;
            switchSearch(HUMIDITY);
        } else {
            ((TextView) findViewById(R.id.resultText)).setText(text);
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.textView1)).setText("");
        if (hypothesis == null) {
            return;
        }
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(TEMPERATURE)) {
            inUse = TEMPERATURE;
            switchSearch(TEMPERATURE);
        } else if (text.equals(BRIGHTNESS)) {
            inUse = BRIGHTNESS;
            switchSearch(BRIGHTNESS);
        } else if (text.equals(HUMIDITY)) {
            inUse = HUMIDITY;
            switchSearch(HUMIDITY);
        } else {
            number = EnglishNumberToText.numberToText(text);
            ((TextView) findViewById(R.id.resultText)).setText((int) number);
        }
    }

    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.textView1))
                .setText(e.toString());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    @Override
    protected void onResume() {
        super.onResume();
        valueMapper = ValueMapper.getInstance(this);
        INUSE = true;
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        INUSE = false;
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
        recognizer.cancel();
        recognizer.shutdown();
    }

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);
        }
    }


}








