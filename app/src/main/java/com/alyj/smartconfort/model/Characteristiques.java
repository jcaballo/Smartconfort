package com.alyj.smartconfort.model;

/**
 * Created by yirou on 15/02/16.
 */
public class Characteristiques {
    private String name;
    private String characteristic;
    private String value;

    public Characteristiques(String name,String characteristic, String value) {
        this.characteristic = characteristic;
        this.value = value;
        this.name=name;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(String characteristic) {
        this.characteristic = characteristic;
    }
}
