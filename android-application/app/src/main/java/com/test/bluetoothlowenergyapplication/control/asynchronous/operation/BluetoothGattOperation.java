package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Handler;

import java.util.Calendar;
import java.util.Date;

public abstract class BluetoothGattOperation implements Comparable<BluetoothGattOperation> {
    private Handler handler;
    private BluetoothGatt bluetoothGatt;
    private Date currentDate;

    public BluetoothGattOperation(Context context, BluetoothGatt bluetoothGatt, Date currentDate) {
        this.handler = new Handler(context.getMainLooper());
        this.bluetoothGatt = bluetoothGatt;
        this.currentDate = currentDate;
    }

    protected Handler getHandler() {
        return handler;
    }

    protected BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    protected Date getCurrentDate() { return currentDate; }

    public void setBluetoothGatt(BluetoothGatt bluetoothGatt) {
        this.bluetoothGatt = bluetoothGatt;
    }

    public abstract void execute(Context context);

    @Override
    public int compareTo(BluetoothGattOperation o) {
        return (int) (o.getCurrentDate().getTime() - currentDate.getTime());
    }
}
