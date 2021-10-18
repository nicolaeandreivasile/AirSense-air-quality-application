package com.test.bluetoothlowenergyapplication.control;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.test.bluetoothlowenergyapplication.control.asynchronous.BluetoothGattManager;
import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattClose;
import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattConnect;
import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattDisconnect;
import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattDiscover;
import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattRead;
import com.test.bluetoothlowenergyapplication.control.asynchronous.operation.BluetoothGattWrite;
import com.test.bluetoothlowenergyapplication.control.model.Data;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class BluetoothLeService extends Service {
    private final static String BLE_SERVICE_TAG = BluetoothLeService.class.getSimpleName();

    public final static String GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String GATT_ERROR = "ACTION_GATT_ERROR";
    public final static String GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String GATT_CHARACTERISTIC_DATA_AVAILABLE =
            "ACTION_GATT_CHARACTERISTIC_DATA_AVAILABLE";
    public final static String GATT_CHARACTERISTIC_EXTRA_DATA = "GATT_CHARACTERISTIC_EXTRA_DATA";
    public final static String GATT_CHARACTERISTIC_RESTART_OPERATION =
            "ACTION_GATT_CHARACTERISTIC_RESTART_OPERATION";

    private String bluetoothLeConnectionState = GATT_DISCONNECTED;

    private final Binder binder = new LocalBinder();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGatt bluetoothGatt = null;
    private BluetoothGattManager bluetoothGattManager = new BluetoothGattManager(this);
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    broadcastUpdate(GATT_CONNECTED, null);
                    bluetoothLeConnectionState = GATT_CONNECTED;
                    bluetoothGatt = gatt;

                    BluetoothGattDiscover bluetoothGattDiscover =
                            new BluetoothGattDiscover(BluetoothLeService.this,
                                    bluetoothGatt, Calendar.getInstance().getTime());
                    bluetoothGattManager.scheduleTask(bluetoothGattDiscover);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    BluetoothGattClose bluetoothGattClose =
                            new BluetoothGattClose(BluetoothLeService.this,
                                    null, Calendar.getInstance().getTime());
                    bluetoothGattManager.scheduleTask(bluetoothGattClose);

                    broadcastUpdate(GATT_DISCONNECTED, null);
                    bluetoothLeConnectionState = GATT_DISCONNECTED;
                }
            } else {
                Log.w(BLE_SERVICE_TAG, "onConnectionStateChange received: status " + status);

                BluetoothGattClose bluetoothGattClose =
                        new BluetoothGattClose(BluetoothLeService.this,
                                null, Calendar.getInstance().getTime());
                bluetoothGattManager.scheduleTask(bluetoothGattClose);

                broadcastUpdate(GATT_ERROR, null);
                bluetoothLeConnectionState = GATT_ERROR;
            }
            bluetoothGattManager.notifyTaskCompleted();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(GATT_SERVICES_DISCOVERED, null);
            } else {
                Log.w(BLE_SERVICE_TAG, "onServicesDiscovered received: status " + status);
            }
            bluetoothGattManager.notifyTaskCompleted();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(GATT_CHARACTERISTIC_DATA_AVAILABLE, characteristic);
            } else {
                Log.w(BLE_SERVICE_TAG, "onCharacteristicRead received: status " + status);
            }
            bluetoothGattManager.notifyTaskCompleted();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(GATT_CHARACTERISTIC_RESTART_OPERATION, null);
            } else {
                Log.w(BLE_SERVICE_TAG, "onCharacteristicWrite received: status " + status);
            }
            bluetoothGattManager.notifyTaskCompleted();
        }
    };

    private void broadcastUpdate(final String bluetoothLeAction,
                                 BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        Intent broadcastUpdateIntent = new Intent(bluetoothLeAction);
        if (bluetoothGattCharacteristic != null) {
            final String characteristicName = EnvironmentalSensingService
                    .lookup(bluetoothGattCharacteristic.getUuid().toString());
            if (characteristicName == null)
                return;

            final byte[] characteristicDataByteArray = bluetoothGattCharacteristic.getValue();
            if (characteristicDataByteArray != null && characteristicDataByteArray.length > 0) {
                int value = 0;
                int mask = 0xFFFFFFFF;
                for (int index = 0; index < characteristicDataByteArray.length &&
                        Math.pow(2, index) < Integer.SIZE; index++) {
                    int intByte =
                            mask & (characteristicDataByteArray[index] << (index * Byte.SIZE));
                    value ^= intByte;
                }
                String characteristicData = String.valueOf(value);

                broadcastUpdateIntent.putExtra(GATT_CHARACTERISTIC_EXTRA_DATA,
                        characteristicName + "\n" + characteristicData);
            }
        }
        sendBroadcast(broadcastUpdateIntent);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(BLE_SERVICE_TAG, "Error occurred while initializing Bluetooth manager");
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(BLE_SERVICE_TAG, "Error occurred while initializing Bluetooth adapter");
            return false;
        }

        return true;
    }

    public void connect(final BluetoothDevice bluetoothLeDevice) {
        BluetoothGattConnect bluetoothGattConnect = new BluetoothGattConnect(this,
                bluetoothGattCallback, bluetoothLeDevice, Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattConnect);
    }

    public void disconnect() {
        if (bluetoothGatt == null)
            return;

        BluetoothGattDisconnect bluetoothGattDisconnect = new BluetoothGattDisconnect(this,
                bluetoothGatt, bluetoothGattCallback, Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattDisconnect);

    }

    private void close() {
        if (bluetoothGatt == null)
            return;

        BluetoothGattClose bluetoothGattClose = new BluetoothGattClose(this, bluetoothGatt,
                Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattClose);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public List<BluetoothGattService> getBluetoothLeGattServices() {
        if (bluetoothGatt == null)
            return null;

        return bluetoothGatt.getServices();
    }

    public void readCharacteristic(BluetoothGattCharacteristic bluetoothLeGattCharacteristic) {
        if (bluetoothGatt == null)
            return;

        BluetoothGattRead bluetoothGattRead = new BluetoothGattRead(this, bluetoothGatt,
                bluetoothLeGattCharacteristic, Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattRead);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (bluetoothGatt == null)
            return;

        BluetoothGattWrite bluetoothGattWrite = new BluetoothGattWrite(this, bluetoothGatt,
                bluetoothGattCharacteristic, Calendar.getInstance().getTime());
        bluetoothGattManager.scheduleTask(bluetoothGattWrite);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public static class EnvironmentalSensingService {
        public static final String ENVIRONMENTAL_SENSING_UUID = "0000181a-0000-1000-8000-00805f9b34fb";
        public static final String CONTROL_CHARACTERISTIC = "Control";

        private static HashMap<String, String> attributes = new LinkedHashMap<String, String>();

        static {
            attributes.put("00002a9f-0000-1000-8000-00805f9b34fb", CONTROL_CHARACTERISTIC);
            attributes.put("00002a6e-0000-1000-8000-00805f9b34fb", Data.TEMPERATURE);
            attributes.put("00002a6f-0000-1000-8000-00805f9b34fb", Data.HUMIDITY);
            attributes.put("00002a6d-0000-1000-8000-00805f9b34fb", Data.PRESSURE);
            attributes.put("00002aca-0000-1000-8000-00805f9b34fb", Data.GAS);
            attributes.put("00002b03-0000-1000-8000-00805f9b34fb", Data.LIGHT);

        }

        public static String lookup(String characteristicUUID) {
            String characteristicName = attributes.get(characteristicUUID);

            return characteristicName;
        }
    }
}
