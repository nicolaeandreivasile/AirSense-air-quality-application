package com.test.bluetoothlowenergyapplication.control.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Date;

public class Measurement implements Parcelable {

    public final static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final int FIELDS = 6;
    private static final int LOCATION_IDX = 0;
    private static final int GAS_IDX = 1;
    private static final int TEMPERATURE_IDX = 2;
    private static final int HUMIDITY_IDX = 3;
    private static final int PRESSURE_IDX = 4;
    private static final int LIGHT_IDX = 5;

    private String location;
    private String gasValue;
    private String temperatureValue;
    private String humidityValue;
    private String pressureValue;
    private String lightValue;
    private Date createdAt;

    public Measurement() {
    }

    public Measurement(String gasValue, String temperatureValue, String humidityValue,
                       String pressureValue, String lightValue) {
        this.gasValue = gasValue;
        this.temperatureValue = temperatureValue;
        this.humidityValue = humidityValue;
        this.pressureValue = pressureValue;
        this.lightValue = lightValue;
    }

    public Measurement(String location, String gasValue, String temperatureValue,
                       String humidityValue, String pressureValue, String lightValue,
                       Date createdAt) {
        this.location = location;
        this.gasValue = gasValue;
        this.temperatureValue = temperatureValue;
        this.humidityValue = humidityValue;
        this.pressureValue = pressureValue;
        this.lightValue = lightValue;
        this.createdAt = createdAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGasValue() {
        return gasValue;
    }

    public void setGasValue(String gasValue) {
        this.gasValue = gasValue;
    }

    public String getTemperatureValue() {
        return temperatureValue;
    }

    public void setTemperatureValue(String temperatureValue) {
        this.temperatureValue = temperatureValue;
    }

    public String getHumidityValue() {
        return humidityValue;
    }

    public void setHumidityValue(String humidityValue) {
        this.humidityValue = humidityValue;
    }

    public String getPressureValue() {
        return pressureValue;
    }

    public void setPressureValue(String pressureValue) {
        this.pressureValue = pressureValue;
    }

    public String getLightValue() {
        return lightValue;
    }

    public void setLightValue(String lightValue) {
        this.lightValue = lightValue;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void populateField(Data data) {
        if (data.getDataName().equals(Data.GAS)) {
            gasValue = data.getDataValue();
        } else if (data.getDataName().equals(Data.TEMPERATURE)) {
            temperatureValue = data.getDataValue();
        } else if (data.getDataName().equals(Data.HUMIDITY)) {
            humidityValue = data.getDataValue();
        } else if (data.getDataName().equals(Data.PRESSURE)) {
            pressureValue = data.getDataValue();
        } else if (data.getDataName().equals(Data.LIGHT)) {
            lightValue = data.getDataValue();
        }
    }

    public boolean isPopulated() {
        return gasValue != null && temperatureValue != null && humidityValue != null &&
                pressureValue != null && lightValue != null;
    }

    public static Creator getCREATOR() {
        return CREATOR;
    }

    public Measurement(Parcel in) {
        String[] measurement = new String[FIELDS];
        in.readStringArray(measurement);

        this.location = measurement[LOCATION_IDX];
        this.gasValue = measurement[GAS_IDX];
        this.temperatureValue = measurement[TEMPERATURE_IDX];
        this.humidityValue = measurement[HUMIDITY_IDX];
        this.pressureValue = measurement[PRESSURE_IDX];
        this.lightValue = measurement[LIGHT_IDX];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[]{this.location, this.gasValue, this.temperatureValue,
                this.humidityValue, this.pressureValue, this.lightValue});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new Measurement(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Measurement[size];
        }
    };

    @NonNull
    @Override
    public String toString() {
        return String.format(location + " " + gasValue + " " + temperatureValue + " " +
                humidityValue + " " + pressureValue + " " + lightValue + createdAt.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return location.equals(((Measurement) obj).getLocation());
    }
}
