package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothGatt;
import android.content.Context;

import java.util.Date;

public class BluetoothGattClose extends BluetoothGattOperation {

    public BluetoothGattClose(Context context, BluetoothGatt bluetoothGatt, Date date) {
        super(context, bluetoothGatt, date);
    }

    @Override
    public void execute(Context context) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (getBluetoothGatt() == null)
                    return;
                
                getBluetoothGatt().close();
                setBluetoothGatt(null);
            }
        });
    }
}
