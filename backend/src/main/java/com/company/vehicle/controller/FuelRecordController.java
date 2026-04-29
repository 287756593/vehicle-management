package com.company.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.vehicle.entity.FuelRecord;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.service.FuelRecordService;
import com.company.vehicle.service.CurrentUserService;
import com.company.vehicle.service.VehicleBorrowRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fuel-records")
public class FuelRecordController {

    @Autowired
    private FuelRecordService fuelRecordService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private VehicleBorrowRecordService vehicleBorrowRecordService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) Long driverId,
            @RequestParam(required = false) Integer isCash,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            Authentication authentication) {
        if ("DRIVER".equals(currentUserService.getCurrentRole(authentication))) {
            Driver currentDriver = currentUserService.getCurrentDriver(authentication);
            driverId = currentDriver.getId();
        }
        Page<FuelRecord> page = fuelRecordService.getPage(current, size, vehicleId, driverId, isCash, year, status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        Map<String, Long> totalSummary = fuelRecordService.getSummary(vehicleId, driverId, isCash, year);
        result.put("summary", totalSummary);
        result.put("filteredSummary", (status == null || status.isBlank()) ? totalSummary
                : fuelRecordService.getSummary(vehicleId, driverId, isCash, year, status));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FuelRecord> getById(@PathVariable Long id, Authentication authentication) {
        FuelRecord record = fuelRecordService.getById(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        if ("DRIVER".equals(currentUserService.getCurrentRole(authentication))) {
            Driver currentDriver = currentUserService.getCurrentDriver(authentication);
            if (!currentDriver.getId().equals(record.getDriverId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(record);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FuelRecord> create(@RequestBody FuelRecord fuelRecord, Authentication authentication) {
        if ("DRIVER".equals(currentUserService.getCurrentRole(authentication))) {
            Driver currentDriver = requireDriverFuelBorrowContext(authentication, fuelRecord.getVehicleId(), fuelRecord.getFuelDate());
            fuelRecord.setDriverId(currentDriver.getId());
        }
        return ResponseEntity.ok(fuelRecordService.createRecord(fuelRecord));
    }

    @PostMapping("/with-photos")
    @PreAuthorize("hasAnyRole('DRIVER', 'OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FuelRecord> createWithPhotos(
            @RequestParam Long vehicleId,
            @RequestParam Long driverId,
            @RequestParam String fuelType,
            @RequestParam(required = false) String fuelAmount,
            @RequestParam(required = false) String fuelPrice,
            @RequestParam(required = false) String totalAmount,
            @RequestParam(required = false) String fuelMileage,
            @RequestParam String fuelDate,
            @RequestParam(required = false) String fuelLocation,
            @RequestParam(required = false) Integer isCash,
            @RequestParam(required = false) String cashReason,
            @RequestParam(required = false) Integer isFuelEnoughAfterFuel,
            @RequestParam(required = false) MultipartFile[] fuelPhotos,
            @RequestParam(required = false) MultipartFile[] fuelGaugePhotos,
            @RequestParam(required = false) MultipartFile[] cashPhotos,
            @RequestParam(required = false) MultipartFile[] leaderApprovalPhotos,
            Authentication authentication) {
        LocalDateTime parsedFuelDate = parseDateTime(fuelDate, "加油时间");
        if ("DRIVER".equals(currentUserService.getCurrentRole(authentication))) {
            Driver currentDriver = requireDriverFuelBorrowContext(authentication, vehicleId, parsedFuelDate);
            driverId = currentDriver.getId();
        }
        return ResponseEntity.ok(fuelRecordService.createRecordWithPhotos(
            vehicleId, driverId, fuelType, fuelAmount, fuelPrice, totalAmount, fuelMileage,
            fuelDate, fuelLocation, isCash, cashReason, isFuelEnoughAfterFuel,
            fuelPhotos, fuelGaugePhotos, cashPhotos, leaderApprovalPhotos));
    }

    @PutMapping("/{id}/approve-cash-report")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Boolean> approveCashReport(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        Long approverId = currentUserService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(fuelRecordService.approveCashReport(id, approverId, approved, comment));
    }

    @PutMapping("/{id}/reimbursement-status")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN', 'FINANCE')")
    public ResponseEntity<Boolean> updateReimbursementStatus(
            @PathVariable Long id,
            @RequestParam boolean reimbursed) {
        return ResponseEntity.ok(fuelRecordService.updateReimbursementStatus(id, reimbursed));
    }

    @PutMapping("/{id}/fuel-amount")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<FuelRecord> updateFuelAmount(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(fuelRecordService.updateFuelAmount(id, payload == null ? null : payload.get("fuelAmount")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Boolean> softDelete(@PathVariable Long id) {
        return ResponseEntity.ok(fuelRecordService.softDelete(id));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Boolean> restore(@PathVariable Long id) {
        return ResponseEntity.ok(fuelRecordService.restore(id));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Boolean> permanentDelete(@PathVariable Long id) {
        return ResponseEntity.ok(fuelRecordService.permanentDelete(id));
    }

    @GetMapping("/recycle-bin")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecycleBin(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<FuelRecord> page = fuelRecordService.getRecycleBin(current, size);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/recycle-bin/clear")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Boolean> clearRecycleBin() {
        return ResponseEntity.ok(fuelRecordService.clearRecycleBin());
    }

    @GetMapping("/statistics/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN', 'FINANCE')")
    public ResponseEntity<Map<String, Object>> getVehicleStatistics(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(fuelRecordService.getVehicleStatistics(vehicleId, year));
    }

    @GetMapping("/statistics/vehicles")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN', 'FINANCE')")
    public ResponseEntity<Object> getVehicleYearlySummary(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(fuelRecordService.getVehicleYearlySummary(year));
    }

    private Driver requireDriverFuelBorrowContext(Authentication authentication, Long vehicleId, LocalDateTime fuelDate) {
        Driver currentDriver = currentUserService.getCurrentDriver(authentication);
        vehicleBorrowRecordService.requireFuelRecordBorrowContext(currentDriver.getId(), vehicleId, fuelDate);
        return currentDriver;
    }

    private LocalDateTime parseDateTime(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + "不能为空");
        }
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (Exception exception) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确");
        }
    }
}
