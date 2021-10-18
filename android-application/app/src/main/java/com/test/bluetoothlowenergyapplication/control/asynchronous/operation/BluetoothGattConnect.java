package com.test.bluetoothlowenergyapplication.control.asynchronous.operation;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import java.util.Date;

public class BluetoothGattConnect extends BluetoothGattOperation {
    BluetoothDevice bluetoothDevice;
    BluetoothGattCallback bluetoothGattCallback;

    public BluetoothGattConnect(Context context, BluetoothGattCallback bluetoothGattCallback,
                                BluetoothDevice bluetoothDevice, Date date) {
        super(context, null, date);
        this.bluetoothDevice = bluetoothDevice;
        this.bluetoothGattCallback = bluetoothGattCallback;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @Override
    public void execute(Context context) {
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                if (getBluetoothGatt() != null)
                    return;

                BluetoothGatt bluetoothGatt =
                        bluetoothDevice.connectGatt(context, false,
                                bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE);
                setBluetoothGatt(bluetoothGatt);
            }
        });
    }
}
