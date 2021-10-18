package com.example.gattbackendapplication.controller;

import com.example.gattbackendapplication.model.Measurement;
import com.example.gattbackendapplication.service.MeasurementService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/measurements")
@AllArgsConstructor
@CrossOrigin
public class MeasurementController {

    private final MeasurementService measurementService;

    @GetMapping("/")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello wanderer. This is just the beginning.");
    }

    @GetMapping("/get-measurements")
    public ResponseEntity<List<Measurement>> fetchAllMeasurements() {
        return ResponseEntity.ok(measurementService.getAllMeasurements());
    }

    @GetMapping("/get-area-measurements/{location}:{radius}")
    public ResponseEntity<List<Measurement>> fetchAreaMeasurements(@PathVariable String location,
                                                                    @PathVariable String radius) {
        return ResponseEntity.ok(measurementService.getAreaMeasurements(location, radius));
    }

    @GetMapping("/get-measurement/{location}")
    public ResponseEntity<Measurement> fetchMeasurement(@PathVariable String location) {
        return ResponseEntity.ok(measurementService.getMeasurementByLocation(location));
    }

    @PostMapping("/register-measurement")
    public ResponseEntity<Measurement> registerMeasurement(@RequestBody Measurement measurement) {
        return ResponseEntity.ok(measurementService.addMeasurement(measurement));
    }

    @PutMapping("/update-measurement")
    public ResponseEntity<Measurement> updateMeasurement(@RequestBody Measurement measurement) {
        return ResponseEntity.ok(measurementService.updateMeasurement(measurement));
    }

    @DeleteMapping("/delete-measurement/{location}")
    public ResponseEntity<Void> deleteMeasurement(@PathVariable String location) {
        measurementService.deleteMeasurementByLocation(location);
        return ResponseEntity.ok().build();
    }
}
