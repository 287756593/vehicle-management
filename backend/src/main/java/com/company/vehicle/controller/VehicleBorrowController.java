package com.company.vehicle.controller;

import com.company.vehicle.dto.AdminSupplementBorrowRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.vehicle.dto.BorrowRecordUpdateRequest;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.SysUser;
import com.company.vehicle.entity.VehicleBorrowRecord;
import com.company.vehicle.entity.VehicleBorrowRecordEditLog;
import com.company.vehicle.service.CurrentUserService;
import com.company.vehicle.service.VehicleBorrowRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle-borrow")
public class VehicleBorrowController {

    private final VehicleBorrowRecordService vehicleBorrowRecordService;
    private final CurrentUserService currentUserService;

    public VehicleBorrowController(
            VehicleBorrowRecordService vehicleBorrowRecordService,
            CurrentUserService currentUserService) {
        this.vehicleBorrowRecordService = vehicleBorrowRecordService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<VehicleBorrowRecord> getCurrentRecord(Authentication authentication) {
        Driver currentDriver = currentUserService.getCurrentDriver(authentication);
        return ResponseEntity.ok(vehicleBorrowRecordService.getCurrentRecord(currentDriver.getId()));
    }

    @GetMapping("/my-records")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> getMyRecords(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Driver currentDriver = currentUserService.getCurrentDriver(authentication);
        Page<VehicleBorrowRecord> page = vehicleBorrowRecordService.getDriverRecordPage(currentDriver.getId(), current, size);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN', 'DRIVER')")
    public ResponseEntity<Map<String, Object>> getRecords(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String driverName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String followUpStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Authentication authentication) {
        String role = currentUserService.getCurrentRole(authentication);
        Long driverId = null;
        if ("DRIVER".equals(role)) {
            driverId = currentUserService.getCurrentDriver(authentication).getId();
        }
        Page<VehicleBorrowRecord> page = vehicleBorrowRecordService.getPage(
                current,
                size,
                plateNumber,
                driverName,
                status,
                followUpStatus,
                parseDateTime(startDate, "开始时间"),
                parseDateTime(endDate, "结束时间"),
                driverId);
        Map<String, Long> summary = vehicleBorrowRecordService.getSummary(
                plateNumber,
                driverName,
                status,
                followUpStatus,
                parseDateTime(startDate, "开始时间"),
                parseDateTime(endDate, "结束时间"),
                driverId);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("summary", summary);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics/mileage")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> getMileageStatistics(
            @RequestParam(defaultValue = "30") int days,
            Authentication authentication) {
        Driver currentDriver = currentUserService.getCurrentDriver(authentication);
        return ResponseEntity.ok(vehicleBorrowRecordService.getMileageStatistics(currentDriver.getId(), days));
    }

    @PostMapping("/take")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> takeVehicle(
            @RequestParam Long vehicleId,
            @RequestParam String initialMileage,
            @RequestParam(required = false) String usageReason,
            @RequestParam String destination,
            @RequestParam(required = false) String expectedReturnTime,
            @RequestParam(required = false) MultipartFile[] vehiclePhotos,
            @RequestParam(required = false) MultipartFile[] mileagePhotos,
            Authentication authentication) {
        Driver currentDriver = currentUserService.getCurrentDriver(authentication);
        VehicleBorrowRecord record = vehicleBorrowRecordService.takeVehicle(
                vehicleId,
                currentDriver.getId(),
                new BigDecimal(initialMileage),
                usageReason,
                destination,
                parseDateTime(expectedReturnTime, "预计还车时间"),
                vehiclePhotos,
                mileagePhotos);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "取车成功");
        result.put("record", record);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/return")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, Object>> returnVehicle(
            @RequestParam Long recordId,
            @RequestParam String returnMileage,
            @RequestParam Integer isClean,
            @RequestParam Integer isFuelEnough,
            @RequestParam(required = false) String issueDescription,
            @RequestParam(required = false) MultipartFile[] vehiclePhotos,
            @RequestParam(required = false) MultipartFile[] fuelPhotos,
            @RequestParam(required = false) MultipartFile[] mileagePhotos,
            @RequestParam(required = false) MultipartFile[] issuePhotos,
            Authentication authentication) {
        Driver currentDriver = currentUserService.getCurrentDriver(authentication);
        VehicleBorrowRecord record = vehicleBorrowRecordService.returnVehicle(
                recordId,
                currentDriver.getId(),
                new BigDecimal(returnMileage),
                isClean,
                isFuelEnough,
                issueDescription,
                vehiclePhotos,
                mileagePhotos,
                fuelPhotos,
                issuePhotos);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "还车成功");
        result.put("record", record);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/records/{id}/complete-follow-up")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<VehicleBorrowRecord> completeFollowUp(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            @RequestParam(defaultValue = "NORMAL") String nextVehicleStatus,
            Authentication authentication) {
        Long handlerId = currentUserService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(vehicleBorrowRecordService.completeFollowUp(id, handlerId, remark, nextVehicleStatus));
    }

    @PutMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<VehicleBorrowRecord> updateRecord(
            @PathVariable Long id,
            @ModelAttribute BorrowRecordUpdateRequest request,
            Authentication authentication) {
        SysUser currentUser = currentUserService.getCurrentUser(authentication);
        String operatorName = currentUser.getRealName() != null && !currentUser.getRealName().isBlank()
                ? currentUser.getRealName()
                : currentUser.getUsername();
        return ResponseEntity.ok(vehicleBorrowRecordService.updateRecordByAdmin(
                id,
                currentUser.getId(),
                operatorName,
                request.getUsageReason(),
                request.getDestination(),
                parseDateTime(request.getTakeTime(), "取车时间"),
                parseDateTime(request.getExpectedReturnTime(), "预计还车时间"),
                request.getTakeMileage(),
                parseDateTime(request.getReturnTime(), "还车时间"),
                request.getReturnMileage(),
                request.getIsClean(),
                request.getIsFuelEnough(),
                request.getIssueDescription(),
                request.getTakeVehiclePhotos(),
                request.getTakeMileagePhotos(),
                request.getReturnVehiclePhotos(),
                request.getReturnMileagePhotos(),
                request.getReturnFuelPhotos(),
                request.getIssuePhotos()
        ));
    }

    @PostMapping("/records/admin-supplement")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<VehicleBorrowRecord> createAdminSupplementRecord(
            @RequestBody AdminSupplementBorrowRequest request) {
        return ResponseEntity.ok(vehicleBorrowRecordService.createSupplementRecordByAdmin(
                request.getVehicleId(),
                request.getDriverId(),
                request.getUsageReason(),
                request.getDestination(),
                parseDateTime(request.getTakeTime(), "取车时间"),
                parseDateTime(request.getExpectedReturnTime(), "预计还车时间"),
                request.getTakeMileage()
        ));
    }

    @DeleteMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<Boolean> deleteRecord(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        Long operatorId = currentUserService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(vehicleBorrowRecordService.softDeleteByAdmin(id, operatorId, reason));
    }

    @PutMapping("/records/{id}/restore")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<Boolean> restoreRecord(@PathVariable Long id, Authentication authentication) {
        Long operatorId = currentUserService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(vehicleBorrowRecordService.restoreByAdmin(id, operatorId));
    }

    @DeleteMapping("/records/{id}/permanent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> permanentDeleteRecord(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleBorrowRecordService.permanentDeleteByAdmin(id));
    }

    @GetMapping("/records/recycle-bin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getRecycleBin(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        Page<VehicleBorrowRecord> page = vehicleBorrowRecordService.getRecycleBin(current, size);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/records/recycle-bin/clear")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Boolean> clearRecycleBin() {
        return ResponseEntity.ok(vehicleBorrowRecordService.clearRecycleBin());
    }

    @GetMapping("/records/{id}/edit-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<List<VehicleBorrowRecordEditLog>> getEditLogs(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleBorrowRecordService.getEditLogs(id));
    }

    private LocalDateTime parseDateTime(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.trim().replace(" ", "T");
            return LocalDateTime.parse(normalized);
        } catch (Exception exception) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确");
        }
    }
}
