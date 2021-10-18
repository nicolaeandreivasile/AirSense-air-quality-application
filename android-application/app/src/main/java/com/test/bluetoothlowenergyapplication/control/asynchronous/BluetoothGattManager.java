package com.test.bluetoothlowenergyapplication.control.asynchronous;

import android.content.Context;

import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattOperation;
import com.test.bluetoothlowenergyapplication.control.asynchronous.structure.BluetoothGattOperationQueue;
import com.test.bluetoothlowenergyapplication.control.asynchronous.structure.OnBluetoothGattOperationOfferListener;

import java.util.concurrent.PriorityBlockingQueue;

public class BluetoothGattManager {

    private Context context;
    private boolean bluetoothGattTasksRunning = false;
    private BluetoothGattOperationQueue<BluetoothGattOperation> bluetoothGattOperationQueue;

    public BluetoothGattManager(Context context) {
        this.context = context;
        this.bluetoothGattOperationQueue = new BluetoothGattOperationQueue<>(
                new PriorityBlockingQueue<BluetoothGattOperation>(),
                new OnBluetoothGattOperationOfferListener<BluetoothGattOperation>() {
                    @Override
                    public void onBluetoothGattOperationAdd(BluetoothGattOperation operation) {
                        if (bluetoothGattTasksRunning == false) {
                            executeTask(context);
                        }
                    }
                });
    }

    public void scheduleTask(BluetoothGattOperation bluetoothGattOperation) {
        bluetoothGattOperationQueue.offer(bluetoothGattOperation);
    }

    public void notifyTaskCompleted() {
        if (bluetoothGattOperationQueue.isEmpty()) {
            bluetoothGattTasksRunning = false;
        } else {
            executeTask(context);
        }
    }

    public void executeTask(Context context) {
        bluetoothGattTasksRunning = true;

        BluetoothGattOperation bluetoothGattOperation = bluetoothGattOperationQueue.poll();
        bluetoothGattOperation.execute(context);
    }

    public int getCurrentLoad() {
        return bluetoothGattOperationQueue.size();
    }
}
