package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.Date;

public class BluetoothGattRead extends BluetoothGattOperation {

    BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public BluetoothGattRead(Context context, BluetoothGatt bluetoothGatt,
                             BluetoothGattCharacteristic bluetoothGattCharacteristic, Date date) {
        super(context, bluetoothGatt, date);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
    }
    
    @Override
    public void execute(Context context) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (getBluetoothGatt() == null)
                    return;

                getBluetoothGatt().readCharacteristic(bluetoothGattCharacteristic);
            }
        });
    }
}
