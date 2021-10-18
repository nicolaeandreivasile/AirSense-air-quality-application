package com.test.bluetoothlowenergyapplication.control;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.github.mikephil.charting.data.Entry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.model.Data;
import com.test.bluetoothlowenergyapplication.control.model.Measurement;
import com.test.bluetoothlowenergyapplication.control.ui.data.DataViewModel;
import com.test.bluetoothlowenergyapplication.control.ui.device.DeviceViewModel;
import com.test.bluetoothlowenergyapplication.control.ui.map.MapViewModel;
import com.test.bluetoothlowenergyapplication.control.ui.statistics.StatisticsFragment;
import com.test.bluetoothlowenergyapplication.control.ui.statistics.StatisticsViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class ControlActivity extends FragmentActivity {
    private final static String CONTROL_TAG = ControlActivity.class.getSimpleName();

    public final static String DEVICE = "DEVICE";
    public final static String FRAGMENT_DEVICE = "FRAGMENT_DEVICE";
    public final static String CHARACTERISTIC = "CHARACTERISTIC";

    private final static int REQUEST_LOCATION_PERMISSION = 1;
    public final static int DEFAULT_COORDINATE_PRECISION = 3;

    private final static byte BLUETOOTH_DEVICE_RESTART_ACTION = 0x01;

    private BluetoothLeService bluetoothLeService;
    private BluetoothDevice bluetoothLeDevice;

    DataViewModel bluetoothLeDataViewModel;
    DeviceViewModel bluetoothLeDeviceViewModel;
    MapViewModel bluetoothLeMapViewModel;
    StatisticsViewModel bluetoothLeStatisticsViewModel;

    private BluetoothGattService bluetoothLeGattEnvironmentalSensingService;
    private LinkedHashMap<BluetoothGattService,
            LinkedList<BluetoothGattCharacteristic>> bluetoothLeDeviceData =
            new LinkedHashMap<BluetoothGattService, LinkedList<BluetoothGattCharacteristic>>();

    private boolean bluetoothLeDeviceConnected = false;
    private final ServiceConnection bluetoothLeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (bluetoothLeService != null) {
                if (!bluetoothLeService.initialize()) {
                    Log.e(CONTROL_TAG, "Error occurred while initializing " +
                            "Bluetooth LE service");
                    finish();
                }

                bluetoothLeService.connect(bluetoothLeDevice);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothLeService = null;
        }
    };

    private final BroadcastReceiver bluetoothLeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.GATT_CONNECTED.equals(action)) {
                bluetoothLeDeviceConnected = true;

                Toast.makeText(context, bluetoothLeDevice.getName() +
                        " connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothLeService.GATT_DISCONNECTED.equals(action)) {
                bluetoothLeDeviceConnected = false;

                Toast.makeText(context, bluetoothLeDevice.getName() +
                        " disconnected", Toast.LENGTH_SHORT).show();

                finish();
            } else if (BluetoothLeService.GATT_CHARACTERISTIC_RESTART_OPERATION.equals(action)) {
                bluetoothLeService.disconnect();
            } else if (BluetoothLeService.GATT_ERROR.equals(action)) {
                bluetoothLeDeviceConnected = false;

                Toast.makeText(context, bluetoothLeDevice.getName() +
                        " has disconnected unexpectedly", Toast.LENGTH_SHORT).show();

                finish();
            } else if (BluetoothLeService.GATT_SERVICES_DISCOVERED.equals(action)) {
                collectBluetoothLeDiscoveredServices(
                        bluetoothLeService.getBluetoothLeGattServices());
            } else if (BluetoothLeService.GATT_CHARACTERISTIC_DATA_AVAILABLE.equals(action)) {
                String dataString =
                        intent.getStringExtra(BluetoothLeService.GATT_CHARACTERISTIC_EXTRA_DATA);
                String[] dataStringTokens = dataString.split("\n");
                String dataName = dataStringTokens[Data.NAME_IDX];
                String dataValue = dataStringTokens[Data.VALUE_IDX];
                Data data = new Data(dataName, dataValue);
                if (measurement != null) {
                    measurement.populateField(data);
                    if (measurement.isPopulated()) {
                        measurement.setLocation(getCurrentLocation(DEFAULT_COORDINATE_PRECISION));
                        measurement.setCreatedAt(Calendar.getInstance().getTime());
                        updateStatisticsList(measurement);
                        uploadMeasurementToCloud(measurement);

                        Toast.makeText(context, "Data updated", Toast.LENGTH_SHORT).show();
                    }
                }

                Intent dataIntent = new Intent();
                dataIntent.putExtra(CHARACTERISTIC, data);
                bluetoothLeDataViewModel.selectIntent(dataIntent);
            }
        }
    };

    private Measurement measurement = null;
    private List<Measurement> measurementList = new ArrayList<>();
    private LocationManager locationManager = null;
    private ConnectivityManager connectivityManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.control_activity_layout);

        Intent intent = getIntent();
        bluetoothLeDevice = (BluetoothDevice) intent.getParcelableExtra(DEVICE);

        BottomNavigationView bottomNavigationView =
                (BottomNavigationView) findViewById(R.id.controlBottomNavigation);
        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.controlFragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        bluetoothLeDataViewModel = new ViewModelProvider(this).get(DataViewModel.class);
        bluetoothLeDeviceViewModel = new ViewModelProvider(this).get(DeviceViewModel.class);
        bluetoothLeMapViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        bluetoothLeStatisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        Intent bluetoothLeServiceIntent = new Intent(ControlActivity.this,
                BluetoothLeService.class);
        bindService(bluetoothLeServiceIntent, bluetoothLeServiceConnection,
                Context.BIND_AUTO_CREATE);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager.getActiveNetwork() == null)
            Toast.makeText(this, R.string.control_no_network, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bluetoothLeUpdateReceiver, buildBluetoothLeIntentFilter());
        if (bluetoothLeService != null)
            bluetoothLeService.connect(bluetoothLeDevice);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                Toast.makeText(this, R.string.location_permission_denied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothLeUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(bluetoothLeServiceConnection);
        bluetoothLeDevice = null;
        bluetoothLeService = null;
    }

    /* Intent filter for broadcast receiver */
    @NotNull
    private static IntentFilter buildBluetoothLeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.GATT_CHARACTERISTIC_RESTART_OPERATION);
        intentFilter.addAction(BluetoothLeService.GATT_ERROR);
        intentFilter.addAction(BluetoothLeService.GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.GATT_CHARACTERISTIC_DATA_AVAILABLE);

        return intentFilter;
    }

    /* Collect discovered services */
    private void collectBluetoothLeDiscoveredServices(
            List<BluetoothGattService> bluetoothGattServices) {
        if (bluetoothGattServices == null)
            return;

        for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
            if (bluetoothGattService.getUuid().toString().equals(
                    BluetoothLeService.EnvironmentalSensingService.ENVIRONMENTAL_SENSING_UUID)) {
                bluetoothLeGattEnvironmentalSensingService = bluetoothGattService;
            } else {
                continue;
            }

            LinkedList<BluetoothGattCharacteristic> bluetoothGattCharacteristicList =
                    new LinkedList<BluetoothGattCharacteristic>();
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                    bluetoothGattService.getCharacteristics())
                bluetoothGattCharacteristicList.add(bluetoothGattCharacteristic);

            bluetoothLeDeviceData.put(bluetoothGattService, bluetoothGattCharacteristicList);
        }
    }

    /* Get current location using GPS services */
    public String getCurrentLocation(int decimals) {
        String currentLocation = null;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        Location gpsLocation = null;
        try {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (gpsLocation != null) {
            double latitude = approximateCoordinate(gpsLocation.getLatitude(), decimals);
            double longitude = approximateCoordinate(gpsLocation.getLongitude(), decimals);
            currentLocation = latitude + "," + longitude;
        }

        return currentLocation;
    }

    /* Approximate position */
    public double approximateCoordinate(double coordinate, int decimals) {
        double gradient = Math.pow(10, decimals);

        return Math.round(coordinate * gradient) / gradient;
    }

    /* Request device information */
    public void requestDevice() {
        Intent deviceIntent = new Intent();
        deviceIntent.putExtra(FRAGMENT_DEVICE, bluetoothLeDevice);
        bluetoothLeDeviceViewModel.selectItem(deviceIntent);
    }

    /* Request new measurement from the connected device */
    public void requestCharacteristicsData() {
        LinkedList<BluetoothGattCharacteristic> bluetoothGattCharacteristicList =
                bluetoothLeDeviceData.get(bluetoothLeGattEnvironmentalSensingService);
        measurement = new Measurement();

        for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                bluetoothGattCharacteristicList) {
            String bluetoothGattCharacteristicName = BluetoothLeService.EnvironmentalSensingService
                    .lookup(bluetoothGattCharacteristic.getUuid().toString());
            if (bluetoothGattCharacteristicName == null || bluetoothGattCharacteristicName.equals(
                    BluetoothLeService.EnvironmentalSensingService.CONTROL_CHARACTERISTIC))
                continue;

            bluetoothLeService.readCharacteristic(bluetoothGattCharacteristic);
        }
    }

    /* Request disconnect from the connected device */
    public void requestDisconnect() {
        bluetoothLeService.disconnect();
    }

    /* Request restart of the connected device */
    public void requestRestart() {
        LinkedList<BluetoothGattCharacteristic> bluetoothGattCharacteristicList =
                bluetoothLeDeviceData.get(bluetoothLeGattEnvironmentalSensingService);

        for (BluetoothGattCharacteristic bluetoothGattCharacteristic :
                bluetoothGattCharacteristicList) {
            String bluetoothGattCharacteristicName = BluetoothLeService.EnvironmentalSensingService
                    .lookup(bluetoothGattCharacteristic.getUuid().toString());
            if (bluetoothGattCharacteristicName == null)
                continue;

            if (bluetoothGattCharacteristicName.equals(
                    BluetoothLeService.EnvironmentalSensingService.CONTROL_CHARACTERISTIC)) {
                byte[] byteArray = new byte[1];
                byteArray[0] = BLUETOOTH_DEVICE_RESTART_ACTION;
                bluetoothGattCharacteristic.setValue(byteArray);
                bluetoothGattCharacteristic.setWriteType(
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                bluetoothLeService.writeCharacteristic(bluetoothGattCharacteristic);
                return;
            }
        }
    }

    /* Update statistics */
    private void updateStatisticsList(Measurement measurement) {
        if (measurement == null || bluetoothLeStatisticsViewModel == null)
            return;

        ArrayList<Entry> statisticsEntryList =
                bluetoothLeStatisticsViewModel.getPersistentArrayField(StatisticsFragment.ENTRIES);
        if (statisticsEntryList == null)
            statisticsEntryList = new ArrayList<Entry>();

        if (statisticsEntryList.size() == StatisticsFragment.DEFAULT_ENTRY_LIMIT)
            statisticsEntryList.remove(0);
        statisticsEntryList.add(new Entry(statisticsEntryList.size(),
                new Float(measurement.getGasValue())));

        bluetoothLeStatisticsViewModel.setPersistentArrayField(
                StatisticsFragment.ENTRIES, statisticsEntryList);
    }

    /* Update / Insert current measurement */
    private void uploadMeasurementToCloud(Measurement measurement) {
        if (measurement == null || connectivityManager.getActiveNetwork() == null)
            return;

        HttpRequestService.getInstance(this).registerMeasurementHttpRequest(measurement);
    }

    /* Request area measurements from the backend service */
    public void requestMeasurementsFromCloud(String location, String radius) {
        if (location == null || radius == null)
            return;

        HttpRequestService.getInstance(this).getAreaMeasurementsHttpRequest(
                bluetoothLeMapViewModel, location, radius);
    }

}
