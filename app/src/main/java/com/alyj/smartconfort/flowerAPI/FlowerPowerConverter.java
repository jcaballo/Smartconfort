package com.alyj.smartconfort.flowerAPI;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.util.Log;

import com.alyj.smartconfort.model.FlowerPower;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;


/**
 * Created by francois on 15/12/2015.
 */
public class FlowerPowerConverter {

    public static String getValueFromSensor(String parameter) {
        String uuid = null;
        if (parameter.equals(FlowerPowerConstants.NAME_AIR_TEMP)) {
            return FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE;
        } else if (parameter.equals(FlowerPowerConstants.NAME_SUNLIGHT_UUID)) {
            return FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT;
        } else if (parameter.equals(FlowerPowerConstants.NAME_SOIL_EC)) {
            return FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_EC;
        } else if (parameter.equals(FlowerPowerConstants.NAME_SOIL_TEMP)) {
            return FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_TEMP;
        } else if (parameter.equals(FlowerPowerConstants.NAME_SOIL_WC)) {
            return FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE;
        } else if (parameter.equals(FlowerPowerConstants.NAME_BATTERY_CHARAC)) {
            return FlowerPowerConstants.CHARACTERISTIC_UUID_BATTERY_LEVEL;
        }
        return uuid;
    }



    public static FlowerPower sendSensorValueRequested(BluetoothGattCharacteristic characteristic, Activity activity, Socket mSocket, String pseudo)
    {

        String toSend = "";
        FlowerPower flowerPower = new FlowerPower();

        final byte[] data = characteristic.getValue();
        System.out.println("characteristic UUID : " + characteristic.getUuid().toString());
        if (characteristic.getUuid().toString().equals(FlowerPowerConstants.CHARACTERISTIC_UUID_SUNLIGHT))
        {
            int i = data[0] + ((data[1] & 0xFF) * 256);
            double sunlight = ValueMapper.getInstance(activity).mapSunlight(i);
            System.out.println("sunlight : " + sunlight);
            toSend = "Je reçois " + i + " lux dans ma zone.";
            flowerPower.setSunlight(sunlight);
        }
        else if (characteristic.getUuid().toString().equals(FlowerPowerConstants.CHARACTERISTIC_UUID_TEMPERATURE))
        {
            int i = data[1] * 256 + (data[0] & 0xFF);
            int temperature = ValueMapper.getInstance(activity).mapTemperature(i);
            System.out.println("Température de l'air : " + temperature);
            toSend = "Il fait " + temperature + "°C dans la zone où je me trouve.";
            flowerPower.setTemperature(temperature);
        }
        else if (characteristic.getUuid().toString().equals(FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_MOISTURE))
        {
            int i = data[1] * 256 + (data[0] & 0xFF);
            double soilMoisture = ValueMapper.getInstance(activity).mapSoilMoisture(i);
            System.out.println("soilMoisture : " + soilMoisture);
            toSend = "Le niveau de moisissure dans mon pot est de " + soilMoisture + ".";
            flowerPower.setSoilMoisture(soilMoisture);
        }
        else if (characteristic.getUuid().toString().equals(FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_TEMP))
        {
            int i = data[1] * 256 + (data[0] & 0xFF);
            int temperature = ValueMapper.getInstance(activity).mapTemperature(i);
            System.out.println("temperature : " + temperature);
            toSend = "La terre contenue dans mon pot est à " + temperature + "°C.";
            flowerPower.setTemperature(temperature);
        }
        else if (characteristic.getUuid().toString().equals(FlowerPowerConstants.CHARACTERISTIC_UUID_SOIL_EC))
        {
            int i = new Byte(data[0]).intValue();
            //double soilMoisture = ValueMapper.getInstance(activity).mapSoilMoisture(i);
            System.out.println("Humidité : " + i);
            toSend = "J'ai dans ma zone un taux d'humidité relative de " + i + " %.";
            flowerPower.setSoilMoisture(i);
        }
        else if (characteristic.getUuid().toString().equals(FlowerPowerConstants.CHARACTERISTIC_UUID_BATTERY_LEVEL))
        {
            int batteryLevel = new Byte(data[0]).intValue();
            System.out.println("batteryLevel : " + batteryLevel);
            toSend = "Je suis chargée à " + batteryLevel + " %.";
            flowerPower.setBatteryLevel(batteryLevel);
        }

        return flowerPower;
    }

}
