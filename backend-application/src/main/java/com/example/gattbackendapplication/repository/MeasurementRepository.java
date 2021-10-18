package com.example.gattbackendapplication.repository;

import com.example.gattbackendapplication.model.Measurement;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MeasurementRepository extends MongoRepository<Measurement, String> {

    Measurement findMeasurementByLocation(String location);
    void deleteMeasurementsByLocation(String location);
}
