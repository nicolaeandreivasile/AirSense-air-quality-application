package com.test.bluetoothlowenergyapplication.scan;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.ControlActivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ScanActivity extends Activity {
    private final static String SCAN_TAG = ScanActivity.class.getSimpleName();

    private final static int REQUEST_BLUETOOTH_ENABLE = 0;
    private final static int REQUEST_LOCATION_PERMISSION = 1;

    private final static int BLUETOOTH_SCAN_PERIOD = 3500;
    private final static int BLUETOOTH_SCAN_RSSI_UPDATE = 1000;

    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bluetoothLeScanner = null;

    private boolean bluetoothLeScanning = false;
    private boolean locationPermissionGranted = false;
    private ArrayList<Boolean> bluetoothLeScanRssiUpdatePending = new ArrayList<>();

    private Handler handler = new Handler(Looper.myLooper());

    private LinkedList<ScanResult> bluetoothLeScanResultList = new LinkedList<>();

    private ScanSettings bluetoothLeScanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build();
    private ScanCallback bluetoothLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            int indexScanResult = -1;
            for (ScanResult scanResult : bluetoothLeScanResultList) {
                if (scanResult.getDevice().getAddress().equals(result.getDevice().getAddress()))
                    indexScanResult = bluetoothLeScanResultList.indexOf(scanResult);
            }
            if (indexScanResult < 0) {
                bluetoothLeScanRssiUpdatePending.add(false);
                bluetoothLeScanResultList.addLast(result);
                bluetoothLeScanResultAdapter.notifyItemInserted(
                        bluetoothLeScanResultList.size() - 1);
            } else {
                if (!bluetoothLeScanRssiUpdatePending.get(indexScanResult)) {
                    int finalIndexScanResult = indexScanResult;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothLeScanResultList.remove(finalIndexScanResult);
                            bluetoothLeScanResultList.add(finalIndexScanResult, result);
                            bluetoothLeScanResultAdapter.notifyItemChanged(finalIndexScanResult);

                            bluetoothLeScanRssiUpdatePending.remove(finalIndexScanResult);
                            bluetoothLeScanRssiUpdatePending.add(finalIndexScanResult,
                                    false);
                        }
                    }, BLUETOOTH_SCAN_RSSI_UPDATE);

                    bluetoothLeScanRssiUpdatePending.remove(finalIndexScanResult);
                    bluetoothLeScanRssiUpdatePending.add(finalIndexScanResult, true);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(SCAN_TAG, "Bluetooth LE scan failed");
        }
    };
    private ScanResultAdapter bluetoothLeScanResultAdapter =
            new ScanResultAdapter(bluetoothLeScanResultList,
                    new ScanResultAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(ScanResult scanResult) {
                            if (bluetoothLeScanning)
                                stopBluetoothLeScan();

                            Intent controlActivityIntent = new Intent(getBaseContext(),
                                    ControlActivity.class);
                            controlActivityIntent.putExtra(ControlActivity.DEVICE,
                                    scanResult.getDevice());
                            startActivity(controlActivityIntent);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_activity_layout);

        /* If the device does not support Bluetooth LE, print a Toast message and exit */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bluetooth_le_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        initializeBluetoothControllers();

        RecyclerView bluetoothScanResultsRecyclerView =
                (RecyclerView) findViewById(R.id.scanRecyclerView);
        bluetoothScanResultsRecyclerView.setAdapter(bluetoothLeScanResultAdapter);
        bluetoothScanResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ImageButton scanButton = (ImageButton) findViewById(R.id.scanSearchButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothLeScanning) {
                    startBluetoothLeScan();
                } else {
                    stopBluetoothLeScan();
                }
            }
        });
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

        /* On resume, enable Bluetooth if it is not already */
        if (!bluetoothAdapter.isEnabled())
            enableBluetoothActivity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BLUETOOTH_ENABLE) {
            if (resultCode != Activity.RESULT_OK)
                enableBluetoothActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanBluetoothLeDevice();
                locationPermissionGranted = true;
            } else {
                Toast.makeText(this, R.string.location_permission_denied,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeBluetoothControllers() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(SCAN_TAG, "Bluetooth manager initialization failed");
            finish();
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(SCAN_TAG, "Bluetooth adapter initialization failed");
            finish();
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e(SCAN_TAG, "Bluetooth LE scanner initialization failed");
            finish();
        }
    }

    private void enableBluetoothActivity() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_BLUETOOTH_ENABLE);
        }
    }

    private void startBluetoothLeScan() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                locationPermissionGranted) {
            bluetoothLeScanRssiUpdatePending.clear();
            bluetoothLeScanResultList.clear();
            bluetoothLeScanResultAdapter.notifyDataSetChanged();
            scanBluetoothLeDevice();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void scanBluetoothLeDevice() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(bluetoothLeScanCallback);
                bluetoothLeScanning = false;
            }
        }, BLUETOOTH_SCAN_PERIOD);
        bluetoothLeScanner.startScan(null, bluetoothLeScanSettings, bluetoothLeScanCallback);
        bluetoothLeScanning = true;
    }

    private void stopBluetoothLeScan() {
        bluetoothLeScanner.stopScan(bluetoothLeScanCallback);
        bluetoothLeScanning = false;
    }
}