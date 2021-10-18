package com.test.bluetoothlowenergyapplication.scan;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.test.bluetoothlowenergyapplication.R;

import java.util.List;

public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {

    private final List<ScanResult> localDataSet;
    private final OnItemClickListener onItemClickListener;

    public ScanResultAdapter(List<ScanResult> localDataSet,
                             OnItemClickListener onItemClickListener) {
        this.localDataSet = localDataSet;
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.scan_item_layout,
                parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ScanResultAdapter.ViewHolder holder, int position) {
        holder.bind(localDataSet.get(position), onItemClickListener);
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public interface OnItemClickListener {
        void onItemClick(ScanResult scanResult);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView deviceImageView;
        private TextView deviceNameView;
        private TextView deviceMACView;
        private TextView deviceRssiView;

        public ViewHolder(View view) {
            super(view);
            deviceImageView = (ImageView) view.findViewById(R.id.scanDeviceImage);
            deviceNameView = (TextView) view.findViewById(R.id.scanDeviceName);
            deviceMACView = (TextView) view.findViewById(R.id.scanDeviceMAC);
            deviceRssiView = (TextView) view.findViewById(R.id.scanDeviceRSSI);
        }

        public void bind(ScanResult scanResult, OnItemClickListener onItemClickListener) {
            deviceImageView.setImageDrawable(itemView.getResources()
                    .getDrawable(selectDeviceImage(scanResult.getDevice()), null));
            String deviceName = (scanResult.getDevice().getName() == null) ?
                    "Unnamed" : scanResult.getDevice().getName();
            deviceNameView.setText(deviceName);
            deviceMACView.setText(scanResult.getDevice().getAddress());
            deviceRssiView.setText(String.valueOf(scanResult.getRssi()) + " dBm");

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(scanResult);
                }
            });
        }

        private int selectDeviceImage(BluetoothDevice bluetoothDevice) {
            int drawableIconId;

            if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                drawableIconId = R.drawable.ic_bluetooth_classic_48dp;
            } else if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                drawableIconId = R.drawable.ic_bluetooth_le_48dp;
            } else if (bluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                drawableIconId = R.drawable.ic_bluetooth_dual_48dp;
            } else {
                drawableIconId = R.drawable.ic_ac_unit_24dp;
            }

            return drawableIconId;
        }

    }
}
