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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.SpeechRecognizer;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.alyj.smartconfort.flowerAPI.FlowerPowerConstants;
import com.alyj.smartconfort.flowerAPI.ValueMapper;
import com.alyj.smartconfort.adapter.CharacteristicsAdapter;
import com.alyj.smartconfort.model.Characteristiques;
import com.alyj.smartconfort.model.EnglishNumberToText;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends Activity implements
        RecognitionListener,SwipeRefreshLayout.OnRefreshListener{
    private static final long SCAN_PERIOD = 10000;
    private static final String KWS_SEARCH = "wake";
    private static final String KEYPHRASE = "wake up";
    private static final String MENU = "principal";
    private static final String MODIFICATION = "modification";
    private static final String TEMPERATURE = "temperature";
    private static final String LUMINOSITE = "brightness";
    private static final String HUMIDITE = "humidity";
    private String enCours = "";
    public static boolean INUSE = false;
    public static int temperature;
    public static double luminosite;
    public static double humidite;
    ValueMapper valueMapper;
    View viewHeader;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothManager bluetoothManager;
    public static List<Characteristiques> propertiesToDisplay ;
    private TextView returnedText;
    private ToggleButton toggleButton;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private edu.cmu.pocketsphinx.SpeechRecognizer recognizer;
    private Context context;
    private BluetoothGattService service;
    private ListView listCharacteristicsView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public static CharacteristicsAdapter adapter;
    private UpdateDeviceData updateDeviceData;
    private TextView currentCharacteristicValue;
    /****
     * Image Button
     */
    private ImageButton dropValue;
    private ImageButton addValue;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    System.err.println("Connected ");
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
                    System.err.println("Disconnected");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            System.err.println("service  finish");
            service = gatt.getService(UUID.fromString(FlowerPowerConstants.SERVICE_UUID_FLOWER_POWER));
            if (service != null) {
//             onRefresh();
                System.err.println("service found" + service.getUuid().toString());

            }

           /* BluetoothGattService service=gatt.getService(UUID.fromString(FlowerPowerConstants.SERVICE_UUID_BATTERY_LEVEL));
            BluetoothGattCharacteristic characteristic=service.getCharacteristic(UUID.fromString(FlowerPowerConstants.CHARACTERISTIC_UUID_BATTERY_LEVEL));
            gatt.readCharacteristic(characteristic);*/
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {

            Log.i("onCharacteristicRead", Arrays.toString(characteristic.getValue()));

        }
    };
    private void initializeCharacteristicsToDisplay(){
       propertiesToDisplay =new ArrayList<>();
        propertiesToDisplay.add(new Characteristiques("Température",FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE,"0"));
        propertiesToDisplay.add(new Characteristiques("Luminosité", FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT,"0"));
        propertiesToDisplay.add( new Characteristiques("Humidité",FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE,"0"));
    }
    private BluetoothAdapter.LeScanCallback LeOldScanner =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (device.getName() != null && device.getName().equalsIgnoreCase("Flower power AAB2")) {
                                System.err.println("found " + device.getName());
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
        viewHeader = (View) getLayoutInflater().inflate(R.layout.activity_main, null);
        initializeCharacteristicsToDisplay();
        listCharacteristicsView=(ListView)findViewById(R.id.listCharacteristicView);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.activity_main_swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        dropValue=(ImageButton)findViewById(R.id.dropValue);
        addValue=(ImageButton)findViewById(R.id.addValue);
        currentCharacteristicValue=(TextView)findViewById(R.id.currentCharacteristicValue);
        dropValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                int line=listCharacteristicsView.getSelectedItemPosition();
                if(line!=-1){
                    HttpPost httppost = new HttpPost(" http://192.168.140.191/core/appliances/cb4a5ac3b4f7455f8ee05802d9a18de8/relay/");
                    List<NameValuePair>params=new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("state","on"));
                    try {
                        httppost.setEntity(new UrlEncodedFormEntity(params));
                        HttpClient client=new DefaultHttpClient();
                        client.execute(httppost);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        addValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        adapter=new CharacteristicsAdapter(this,propertiesToDisplay);
        listCharacteristicsView.setAdapter(adapter);
        listCharacteristicsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Characteristiques characteristique=propertiesToDisplay.get(position);
                currentCharacteristicValue.setText(characteristique.getValue());
            }
        });
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
        context = this.getApplicationContext();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }
private void refreshData(){
//    updateDeviceData=new UpdateDeviceData(context,service,gatt,listCharacteristicsView);
//    mHandler.postDelayed(new UpdateDeviceData(context, service, mGatt, listCharacteristicsView));
//    mSwipeRefreshLayout.setRefreshing(false);
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
                .setKeywordThreshold(1e-30f)

                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU, menuGrammar);

       // Create grammar-based search for numbers
        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addGrammarSearch(MODIFICATION, digitsGrammar);


    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).

        if (searchName.equals(KWS_SEARCH)) {
            ((TextView) findViewById(R.id.textView1)).setText(R.string.speech_prompt);
            recognizer.startListening(searchName);
        }
        else
            recognizer.startListening(searchName, 10000);

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH)) {
            ((TextView) findViewById(R.id.textView1)).setText(R.string.speech_prompt);
            switchSearch(KWS_SEARCH);
        }
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return;
        }
        String text = hypothesis.getHypstr();

        if (text.contains(KEYPHRASE)) {
            ((TextView) findViewById(R.id.textView1))
                    .setText("What ?");
            switchSearch(MENU);
        }
        else if(text.contains(TEMPERATURE) || text.contains(LUMINOSITE) || text.contains(HUMIDITE))
        {
            enCours = text;
            ((TextView) findViewById(R.id.textView1))
                    .setText(text);
            switchSearch(MODIFICATION);
        }


       /* else {
            if (enCours.equals(TEMPERATURE)) {
                temperature = EnglishNumberToText.numberToText(text);
                Toast toast = Toast.makeText(context, Integer.toString(temperature), Toast.LENGTH_LONG);
                toast.show();

            } else if (enCours.equals(LUMINOSITE)) {
                luminosite = EnglishNumberToText.numberToText(text);
                Toast toast = Toast.makeText(context, Double.toString(luminosite), Toast.LENGTH_LONG);
                toast.show();


            } else if (enCours.equals(HUMIDITE)) {
                humidite = EnglishNumberToText.numberToText(text);
                Toast toast = Toast.makeText(context, Double.toString(humidite), Toast.LENGTH_LONG);
                toast.show();

            }
            enCours = "";
            switchSearch(KWS_SEARCH);

        }*/
    }

    @Override
    public void onResult(Hypothesis hypothesis) {

        if (hypothesis == null) {
            return;
        }
        String text = hypothesis.getHypstr();
        if(recognizer.getSearchName() == MODIFICATION) {
            if (enCours.equals(TEMPERATURE)) {
                temperature = EnglishNumberToText.numberToText(text);
                Toast toast = Toast.makeText(context, Integer.toString(temperature), Toast.LENGTH_LONG);
                toast.show();

            } else if (enCours.equals(LUMINOSITE)) {
                luminosite = EnglishNumberToText.numberToText(text);
                Toast toast = Toast.makeText(context, Double.toString(luminosite), Toast.LENGTH_LONG);
                toast.show();


            } else if (enCours.equals(HUMIDITE)) {
                humidite = EnglishNumberToText.numberToText(text);
                Toast toast = Toast.makeText(context, Double.toString(humidite), Toast.LENGTH_LONG);
                toast.show();

            }
            enCours = "";
            switchSearch(KWS_SEARCH);
        }



    }

    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.textView1))
                .setText(e.toString());
    }

    @Override
    public void onTimeout() {

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
//mBluetoothAdapter.startDiscovery();
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
            mGatt = device.connectGatt(this, true, gattCallback);
            scanLeDevice(false);
        }
    }


    @Override
    public void onRefresh() {
        System.err.println("swipe ");
        for (Characteristiques ch : MainActivity.propertiesToDisplay) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(ch.getCharacteristic()));
            if (characteristic != null) {
                mGatt.readCharacteristic(characteristic);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                displayData(characteristic,ch);
            }

        }
    }
    private void displayData(final BluetoothGattCharacteristic characteristic ,Characteristiques ch) {
        switch (characteristic.getUuid().toString()) {
            case FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE:
                int temperature = valueMapper.mapTemperature(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                ch.setValue(temperature+"");
                System.err.println("Temp " +ch.getValue());
                break;
            case FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT:
                double luminosite = valueMapper.mapSunlight(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                ch.setValue(luminosite+"");
                System.err.println("Display Sunlight" + ch.getValue());
                break;
            case FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE:

                double humidite = valueMapper.mapSoilMoisture(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0));
                ch.setValue(humidite+"");
                System.err.println("Display Soil Moisture " + ch.getValue());
                break;
            default:
                break;

        }
        MainActivity.adapter.notifyDataSetChanged();
        listCharacteristicsView.invalidate();
        mSwipeRefreshLayout.setRefreshing(false);
    }
}








