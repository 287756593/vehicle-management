package com.company.vehicle.controller;

import com.company.vehicle.service.DriverService;
import com.company.vehicle.service.HalfYearReportService;
import com.company.vehicle.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final HalfYearReportService halfYearReportService;

    public StatisticsController(
            VehicleService vehicleService,
            DriverService driverService,
            HalfYearReportService halfYearReportService) {
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        this.halfYearReportService = halfYearReportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics() {
        long vehicleCount = vehicleService.count();
        long availableCount = vehicleService.countAvailable();
        long driverCount = driverService.count();

        Map<String, Object> data = new HashMap<>();
        data.put("vehicleCount", vehicleCount);
        data.put("availableCount", availableCount);
        data.put("driverCount", driverCount);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/half-year-report")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE')")
    public ResponseEntity<Map<String, Object>> getHalfYearReport(
            @RequestParam(required = false) String endMonth) {
        return ResponseEntity.ok(halfYearReportService.buildHalfYearReport(endMonth));
    }
}
