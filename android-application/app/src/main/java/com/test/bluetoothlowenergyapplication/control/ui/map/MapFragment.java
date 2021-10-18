package com.test.bluetoothlowenergyapplication.control.ui.map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.test.bluetoothlowenergyapplication.R;
import com.test.bluetoothlowenergyapplication.control.ControlActivity;
import com.test.bluetoothlowenergyapplication.control.HttpRequestService;
import com.test.bluetoothlowenergyapplication.control.model.Measurement;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private final static String MARKER_LIST = "MARKER_LIST";
    private final static String MEASUREMENT_LIST = "MEASUREMENT_LIST";

    private MapViewModel mapViewModel;

    private ArrayList<Measurement> measurementList;
    private ArrayList<MarkerOptions> markerList;
    private GoogleMap map;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.control_fragment_map_layout,
                container, false);

        return view;
    }

    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFragment);
        supportMapFragment.getMapAsync(this);

        /* Get MapViewModel and retrieve saved data */
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        if (!retrieveMeasurements())
            return;

        /* Initialize observer for for MapViewModel */
        mapViewModel.getSelectedIntent().observe(getViewLifecycleOwner(), new Observer<Intent>() {
            @Override
            public void onChanged(Intent intent) {
                Measurement measurement = intent.getParcelableExtra(HttpRequestService.MEASUREMENT);
                if (measurement == null)
                    return;

                if (measurementList.contains(measurement))
                    measurementList.remove(measurementList.indexOf(measurement));
                measurementList.add(measurement);

                MarkerOptions markerOptions = createMarkerOptions(measurement);
                markerList.add(markerOptions);
                if (map != null)
                    map.addMarker(markerOptions);
            }
        });

        /* Initialize refresh button */
        ImageButton refreshButton = (ImageButton) view.findViewById(R.id.mapRefreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String location = ((ControlActivity) getActivity()).getCurrentLocation(
                        ControlActivity.DEFAULT_COORDINATE_PRECISION);
                requestMeasurements(location, String.valueOf(0.2));
            }
        });

        /* Request measurements if there aren't any available */
        if (measurementList != null && measurementList.isEmpty()) {
            String location = ((ControlActivity) getActivity()).getCurrentLocation(
                    ControlActivity.DEFAULT_COORDINATE_PRECISION);
            requestMeasurements(location, String.valueOf(0.2));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveMeasurements();
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
        map = googleMap;

        /* Set the camera position of the map according to the current location */
        String location = ((ControlActivity) getActivity()).getCurrentLocation(
                ControlActivity.DEFAULT_COORDINATE_PRECISION);
        String[] locationTokens = location.split(",");
        double latitude = new Double(locationTokens[0]);
        double longitude = new Double(locationTokens[1]);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(
                new CameraPosition(new LatLng(latitude, longitude), 14, 0, 0));
        map.moveCamera(cameraUpdate);

        /* Display retrieved markers, if any */
        for (MarkerOptions marker : markerList)
            map.addMarker(marker);
    }

    /* Create map marker */
    private MarkerOptions createMarkerOptions(Measurement measurement) {
        if (measurement == null)
            return null;

        String[] locationTokens = measurement.getLocation().split(",");
        double latitude = new Double(locationTokens[0]);
        double longitude = new Double(locationTokens[1]);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Measurement.DATE_PATTERN);
        String date = simpleDateFormat.format(measurement.getCreatedAt());
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("IAQ: " + measurement.getGasValue())
                .snippet(latitude + "," + longitude + " - " + date)
                .icon(BitmapDescriptorFactory.defaultMarker(
                        selectMarkerColor(Integer.valueOf(measurement.getGasValue()))));

        return markerOptions;
    }

    /* Select the color of the map marker according to the IAQ */
    private float selectMarkerColor(int gasValue) {
        float markerColorIndex = BitmapDescriptorFactory.HUE_BLUE;

        if (gasValue < 100) {
            markerColorIndex = BitmapDescriptorFactory.HUE_GREEN;
        } else if (gasValue >= 100 && gasValue < 150) {
            markerColorIndex = BitmapDescriptorFactory.HUE_YELLOW;
        } else if (gasValue >= 150 && gasValue < 200) {
            markerColorIndex = BitmapDescriptorFactory.HUE_ORANGE;
        } else if (gasValue >= 200 && gasValue < 250) {
            markerColorIndex = BitmapDescriptorFactory.HUE_RED;
        } else if (gasValue >= 250 && gasValue < 350) {
            markerColorIndex = BitmapDescriptorFactory.HUE_MAGENTA;
        } else if (gasValue >= 350) {
            markerColorIndex = BitmapDescriptorFactory.HUE_VIOLET;
        }

        return markerColorIndex;
    }

    /* Save measurements and map markers using MapViewModel */
    private void saveMeasurements() {
        if (mapViewModel == null)
            return;

        mapViewModel.setPersistentArrayMarkerOptions(MARKER_LIST, markerList);
        mapViewModel.setPersistentArrayMeasurements(MEASUREMENT_LIST, measurementList);
    }

    /* Retrieve measurements and markers from MapViewModel */
    private boolean retrieveMeasurements() {
        if (mapViewModel == null)
            return false;

        markerList = mapViewModel.getPersistentArrayMarkerOptions(MARKER_LIST);
        if (markerList == null)
            markerList = new ArrayList<MarkerOptions>();
        measurementList = mapViewModel.getPersistentArrayMeasurements(MEASUREMENT_LIST);
        if (measurementList == null)
            measurementList = new ArrayList<Measurement>();

        return true;
    }

    /* Request measurements from cloud */
    private void requestMeasurements(String location, String radius) {
        if (location == null)
            return;

        ((ControlActivity) getActivity()).requestMeasurementsFromCloud(location, radius);
    }
}
