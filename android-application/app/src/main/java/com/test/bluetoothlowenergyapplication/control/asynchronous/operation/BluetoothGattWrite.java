package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.Date;

public class BluetoothGattWrite extends BluetoothGattOperation {

    BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public BluetoothGattWrite(Context context, BluetoothGatt bluetoothGatt,
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

                getBluetoothGatt().writeCharacteristic(bluetoothGattCharacteristic);
            }
        });
    }
}
