package com.example.gattbackendapplication.service;

import com.example.gattbackendapplication.model.Measurement;
import com.example.gattbackendapplication.repository.MeasurementRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;

    public List<Measurement> getAllMeasurements() {
        return measurementRepository.findAll();
    }

    public List<Measurement> getAreaMeasurements(String location, String radius) {
        List<Measurement> areaMeasurements = new ArrayList<>();
        List<Measurement> allMeasurements = measurementRepository.findAll();


        String[] locationTokens = location.split(",");
        double latitude = Double.parseDouble(locationTokens[0]);
        double longitude = Double.parseDouble(locationTokens[1]);

        for (Measurement measurement : allMeasurements) {
            String[] measurementLocationTokens = measurement.getLocation().split(",");
            double measurementLatitude = Double.parseDouble(measurementLocationTokens[0]);
            double measurementLongitude = Double.parseDouble(measurementLocationTokens[1]);

            if (Math.sqrt(Math.pow((measurementLatitude - latitude), 2) +
                    Math.pow((measurementLongitude - longitude), 2)) <= Double.parseDouble(radius))
                areaMeasurements.add(measurement);
        }

        return areaMeasurements;
    }

    public Measurement addMeasurement(Measurement measurement) {
        return measurementRepository.save(processMeasurement(measurement));
    }

    public Measurement getMeasurementByLocation(String location) {
        return measurementRepository.findMeasurementByLocation(location);
    }

    public Measurement updateMeasurement(Measurement measurement) {
        return measurementRepository.save(processMeasurement(measurement));
    }

    public void deleteMeasurementByLocation(String location) {
        measurementRepository.deleteMeasurementsByLocation(location);
    }

    private Measurement processMeasurement(Measurement measurement) {
        Measurement databaseMeasurement = measurementRepository.findMeasurementByLocation(measurement.getLocation());
        if (databaseMeasurement == null) {
            databaseMeasurement = measurement;
        } else {
            databaseMeasurement.setGas(measurement.getGas());
            databaseMeasurement.setTemperature(measurement.getTemperature());
            databaseMeasurement.setHumidity(measurement.getHumidity());
            databaseMeasurement.setPressure(measurement.getPressure());
            databaseMeasurement.setLight(measurement.getLight());
            databaseMeasurement.setCreatedAt(measurement.getCreatedAt());
        }

        return databaseMeasurement;
    }
}
