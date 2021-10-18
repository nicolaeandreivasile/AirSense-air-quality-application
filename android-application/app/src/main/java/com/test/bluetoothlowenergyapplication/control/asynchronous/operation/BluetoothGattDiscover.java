package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import java.util.Date;

public class BluetoothGattDiscover extends BluetoothGattOperation{

    public BluetoothGattDiscover(Context context, BluetoothGatt bluetoothGatt, Date date) {
        super(context, bluetoothGatt, date);
    }

    @Override
    public void execute(Context context) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                getBluetoothGatt().discoverServices();
            }
        });
    }
}
