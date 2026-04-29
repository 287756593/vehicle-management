package com.company.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String status) {
        Page<Vehicle> page = vehicleService.getPage(current, size, plateNumber, status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(vehicleService.getOverview());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailable() {
        return ResponseEntity.ok(vehicleService.getAvailableVehicles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getVehicleDetail(id);
        if (vehicle == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(vehicle);
    }

    @PostMapping
    @PreAuthorize("hasRole('OFFICE_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Vehicle> create(@RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.createVehicle(vehicle));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Vehicle> update(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        Vehicle saved = vehicleService.updateVehicle(id, vehicle);
        return saved == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/traffic-restriction-release")
    @PreAuthorize("hasRole('OFFICE_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Vehicle> updateTrafficRestrictionRelease(
            @PathVariable Long id,
            @RequestParam boolean released) {
        Vehicle saved = vehicleService.updateTrafficRestrictionRelease(id, released);
        return saved == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehicleService.deleteVehicleSafely(id);
        return ResponseEntity.ok().build();
    }
}
