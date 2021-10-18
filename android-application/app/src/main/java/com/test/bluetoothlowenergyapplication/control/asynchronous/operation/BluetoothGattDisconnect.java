package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import java.util.Date;

public class BluetoothGattDisconnect extends BluetoothGattOperation {

    public BluetoothGattDisconnect(Context context, BluetoothGatt bluetoothGatt,
                                   BluetoothGattCallback bluetoothGattCallback, Date date) {
        super(context, bluetoothGatt, date);
    }

    @Override
    public void execute(Context context) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (getBluetoothGatt() == null)
                    return;

                getBluetoothGatt().disconnect();
            }
        });
    }
}
