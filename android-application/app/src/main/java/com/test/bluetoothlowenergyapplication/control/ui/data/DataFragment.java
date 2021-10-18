package com.test.bluetoothlowenergyapplication.control.ui.data;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.ControlActivity;
import com.test.bluetoothlowenergyapplication.control.model.Data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DataFragment extends Fragment {
    public final static String OTHER = "Others";

    private final static String TEMPERATURE_UNIT = "\u2103";
    private final static String HUMIDITY_UNIT = "%";
    private final static String PRESSURE_UNIT = " atm";

    DataViewModel dataViewModel;

    private Data dataGas;
    private Data dataTemperature;
    private Data dataHumidity;
    private Data dataPressure;
    private ArrayList<Data> dataOtherList;
    private DataAdapter dataOtherAdapter;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.control_fragment_data_layout,
                container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView gasTextValue = (TextView) view.findViewById(R.id.dataGasValue);
        TextView temperatureTextValue = (TextView) view.findViewById(R.id.dataTemperatureValue);
        TextView humidityTextValue = (TextView) view.findViewById(R.id.dataHumidityValue);
        TextView pressureTextValue = (TextView) view.findViewById(R.id.dataPressureValue);

        /* Get the DataViewModel and retrieve saved values */
        dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        if (!retrieveData())
            return;

        /* Initialize the RecyclerView for the additional fields */
        RecyclerView dataRecyclerView = view.findViewById(R.id.dataOtherRecycler);
        dataRecyclerView.setAdapter(dataOtherAdapter);
        dataRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        /* Set the change listener for the DataViewModel intent */
        dataViewModel.getSelectedIntent().removeObservers(getViewLifecycleOwner());
        dataViewModel.getSelectedIntent().observe(getViewLifecycleOwner(), new Observer<Intent>() {
            @Override
            public void onChanged(Intent intent) {
                Data receivedData =
                        (Data) intent.getParcelableExtra(ControlActivity.CHARACTERISTIC);
                if (receivedData == null)
                    return;

                if (receivedData.getDataName().equals(Data.GAS)) {
                    dataGas = receivedData;
                    gasTextValue.setText(dataGas.getDataValue());
                } else if (receivedData.getDataName().equals(Data.TEMPERATURE)) {
                    dataTemperature = receivedData;
                    temperatureTextValue.setText(dataTemperature.getDataValue() + TEMPERATURE_UNIT);
                } else if (receivedData.getDataName().equals(Data.HUMIDITY)) {
                    dataHumidity = receivedData;
                    humidityTextValue.setText(dataHumidity.getDataValue() + HUMIDITY_UNIT);
                } else if (receivedData.getDataName().equals(Data.PRESSURE)) {
                    dataPressure = receivedData;
                    pressureTextValue.setText(dataPressure.getDataValue() + PRESSURE_UNIT);
                } else {
                    modifyOtherData(receivedData);
                }
            }
        });

        /* Get the data refresh button and define the click action */
        ImageButton dataRefreshButton = (ImageButton) view.findViewById(R.id.dataRefreshButton);
        dataRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestData();
            }
        });

        /* Update the fragment's arguments accordingly */
        if (!isDataAvailable()) {
            requestData();
        } else {
            gasTextValue.setText(dataGas.getDataValue());
            temperatureTextValue.setText(dataTemperature.getDataValue() + TEMPERATURE_UNIT);
            humidityTextValue.setText(dataHumidity.getDataValue() + HUMIDITY_UNIT);
            pressureTextValue.setText(dataPressure.getDataValue() + PRESSURE_UNIT);
            dataOtherAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveData();
    }

    /* Modify the additional data fields accordingly */
    private void modifyOtherData(Data receivedData) {
        int indexData = -1;
        for (Data data : dataOtherList) {
            if (data.getDataName().equals(receivedData.getDataName()))
                indexData = dataOtherList.indexOf(data);
        }
        if (indexData >= 0) {
            dataOtherList.remove(indexData);
            dataOtherList.add(indexData, receivedData);
            dataOtherAdapter.notifyItemChanged(indexData);
        } else {
            dataOtherList.add(receivedData);
            dataOtherAdapter.notifyItemInserted(dataOtherList.size() - 1);
        }
    }

    /* Save data using DataViewModel */
    public void saveData() {
        if (dataViewModel == null)
            return;

        dataViewModel.setPersistentField(Data.GAS, dataGas);
        dataViewModel.setPersistentField(Data.TEMPERATURE, dataTemperature);
        dataViewModel.setPersistentField(Data.HUMIDITY, dataHumidity);
        dataViewModel.setPersistentField(Data.PRESSURE, dataPressure);
        dataViewModel.setPersistentArrayField(OTHER, dataOtherList);
    }

    /* Retrieve data from the DataViewModel */
    public boolean retrieveData() {
        if (dataViewModel == null)
            return false;

        dataGas = dataViewModel.getPersistentField(Data.GAS);
        dataTemperature = dataViewModel.getPersistentField(Data.TEMPERATURE);
        dataHumidity = dataViewModel.getPersistentField(Data.HUMIDITY);
        dataPressure = dataViewModel.getPersistentField(Data.PRESSURE);
        dataOtherList = dataViewModel.getPersistentArrayField(OTHER);
        if (dataOtherList == null)
            dataOtherList = new ArrayList<Data>();
        dataOtherAdapter = new DataAdapter(dataOtherList);

        return true;
    }

    /* Check if data is available */
    private boolean isDataAvailable() {
        return dataGas != null && dataTemperature != null &&
                dataHumidity != null && dataPressure != null;
    }

    /* Request data from the Bluetooth LE device */
    private void requestData() {
        ((ControlActivity) getActivity()).requestCharacteristicsData();
    }
}
