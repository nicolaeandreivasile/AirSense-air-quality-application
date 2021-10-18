package com.test.bluetoothlowenergyapplication.control.ui.device;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.ControlActivity;

import org.jetbrains.annotations.NotNull;

public class DeviceFragment extends Fragment {
    private final static String NAME = "Name";
    private final static String ADDRESS = "Address";

    DeviceViewModel deviceViewModel;

    String deviceName;
    String deviceAddress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.control_fragment_device_layout,
                container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView deviceNameView = (TextView) view.findViewById(R.id.deviceName);
        TextView deviceAddressView = (TextView) view.findViewById(R.id.deviceAddress);

        /* Get the DeviceViewModel and retrieve saved values */
        deviceViewModel = new ViewModelProvider(requireActivity()).get(DeviceViewModel.class);
        if (!retrieveDevice())
            return;

        /* Set the change listener for the DeviceViewModel intent */
        deviceViewModel.getSelectedItem().removeObservers(getViewLifecycleOwner());
        deviceViewModel.getSelectedItem().observe(getViewLifecycleOwner(), new Observer<Intent>() {
            @Override
            public void onChanged(Intent intent) {
                BluetoothDevice bluetoothDevice =
                        intent.getParcelableExtra(ControlActivity.FRAGMENT_DEVICE);
                deviceNameView.setText(bluetoothDevice.getName());
                deviceAddressView.setText(bluetoothDevice.getAddress());
            }
        });

        /* Get the device disconnect button and define the click action */
        Button disconnectButton = (Button) view.findViewById(R.id.deviceDisconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ControlActivity) getActivity()).requestDisconnect();
            }
        });

        /* Get the device restart button and define the click action */
        Button restartButton = (Button) view.findViewById(R.id.deviceRestartButton);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ControlActivity) getActivity()).requestRestart();
            }
        });

        /* Update the fragment's arguments accordingly */
        if (!isDeviceAvailable()) {
            ((ControlActivity) getActivity()).requestDevice();
        } else {
            deviceNameView.setText(deviceName);
            deviceAddressView.setText(deviceAddress);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveDevice();
    }

    /* Save device fields using DeviceViewModel */
    public void saveDevice() {
        if (deviceViewModel == null)
            return;

        deviceViewModel.setPersistentField(NAME, deviceName);
        deviceViewModel.setPersistentField(ADDRESS, deviceAddress);
    }

    /* Retrieve device fields from the DeviceViewModel */
    public boolean retrieveDevice() {
        if (deviceViewModel == null)
            return false;

        deviceName = deviceViewModel.getPersistentField(NAME);
        deviceAddress = deviceViewModel.getPersistentField(ADDRESS);

        return true;
    }

    /* Check if device fields are available */
    private boolean isDeviceAvailable() {
        return deviceName != null && deviceAddress != null;
    }
}
