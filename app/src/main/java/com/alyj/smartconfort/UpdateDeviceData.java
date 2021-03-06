package com.alyj.smartconfort;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.alyj.smartconfort.flowerAPI.FlowerPowerConstants;
import com.alyj.smartconfort.flowerAPI.ValueMapper;
import com.alyj.smartconfort.model.Characteristiques;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/*

Created by yirou on 15/02/16.
*/


public class UpdateDeviceData implements Runnable {
    private Thread thread;
    private Context context;
    private BluetoothGattService service;
    private BluetoothGatt gatt;
    private ValueMapper valueMapper ;
    private ListView myView;


    public UpdateDeviceData(Context context, BluetoothGattService service, BluetoothGatt gatt,ListView myView) {
        this.context = context;
        this.service = service;
        this.gatt = gatt;
        this.myView=myView;
        this.valueMapper=ValueMapper.getInstance(context);
    }

    @Override
    public void run() {

                for (Characteristiques ch : MainActivity.propertiesToDisplay) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(ch.getCharacteristic()));
                    if (characteristic != null) {
                        gatt.readCharacteristic(characteristic);
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
        myView.invalidate();
    }
}
