package com.company.vehicle.controller;

import com.company.vehicle.service.CurrentUserService;
import com.company.vehicle.service.DriverVehicleActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/vehicle-activity")
public class DriverVehicleActivityController {

    private final DriverVehicleActivityService driverVehicleActivityService;
    private final CurrentUserService currentUserService;

    public DriverVehicleActivityController(
            DriverVehicleActivityService driverVehicleActivityService,
            CurrentUserService currentUserService) {
        this.driverVehicleActivityService = driverVehicleActivityService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> getOverview(
            @RequestParam(required = false) String keyword,
            Authentication authentication) {
        currentUserService.getCurrentDriver(authentication);
        return ResponseEntity.ok(driverVehicleActivityService.getOverview(keyword));
    }
}
