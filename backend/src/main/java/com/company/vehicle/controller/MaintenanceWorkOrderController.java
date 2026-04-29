package com.company.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.vehicle.dto.MaintenanceWorkOrderAcceptRequest;
import com.company.vehicle.dto.MaintenanceWorkOrderCancelRequest;
import com.company.vehicle.dto.MaintenanceWorkOrderCreateRequest;
import com.company.vehicle.dto.MaintenanceWorkOrderFinishRequest;
import com.company.vehicle.dto.MaintenanceWorkOrderFromBorrowRequest;
import com.company.vehicle.dto.MaintenanceWorkOrderStartRepairRequest;
import com.company.vehicle.entity.MaintenanceWorkOrder;
import com.company.vehicle.service.CurrentUserService;
import com.company.vehicle.service.MaintenanceWorkOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance/work-orders")
public class MaintenanceWorkOrderController {

    private final MaintenanceWorkOrderService maintenanceWorkOrderService;
    private final CurrentUserService currentUserService;

    public MaintenanceWorkOrderController(MaintenanceWorkOrderService maintenanceWorkOrderService,
                                          CurrentUserService currentUserService) {
        this.maintenanceWorkOrderService = maintenanceWorkOrderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN','FINANCE')")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String keyword) {
        Page<MaintenanceWorkOrder> page = maintenanceWorkOrderService.getPage(
                current,
                size,
                status,
                vehicleId,
                plateNumber,
                keyword
        );
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN','FINANCE')")
    public ResponseEntity<MaintenanceWorkOrder> detail(@PathVariable Long id) {
        MaintenanceWorkOrder order = maintenanceWorkOrderService.getDetail(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<MaintenanceWorkOrder> createManual(
            @RequestBody MaintenanceWorkOrderCreateRequest request,
            Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        Long userId = currentUserService.getCurrentUser(authentication).getId();
        MaintenanceWorkOrder order = maintenanceWorkOrderService.createManual(
                request.getVehicleId(),
                request.getPlateNumber(),
                normalizeBlank(request.getWorkType(), "REPAIR"),
                firstNonBlank(request.getIssueDescription(), request.getFaultDescription()),
                request.getEstimatedCost(),
                parseDateTime(request.getExpectedFinishTime()),
                firstNonBlank(request.getRepairVendor(), request.getRepairFactory()),
                request.getRepairContact(),
                request.getRemark(),
                userId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/from-borrow-record/{recordId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<MaintenanceWorkOrder> createFromBorrowRecord(
            @PathVariable Long recordId,
            @RequestBody(required = false) MaintenanceWorkOrderFromBorrowRequest request,
            Authentication authentication) {
        Long userId = currentUserService.getCurrentUser(authentication).getId();
        MaintenanceWorkOrder order = maintenanceWorkOrderService.createFromBorrowRecord(
                recordId,
                userId,
                request == null ? null : request.getIssueDescription(),
                request == null ? null : parseDateTime(request.getExpectedFinishTime()),
                request == null ? null : request.getRemark()
        );
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> submit(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceWorkOrderService.submit(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        Long approverId = currentUserService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(maintenanceWorkOrderService.approve(id, approverId, comment));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        Long approverId = currentUserService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(maintenanceWorkOrderService.reject(id, approverId, comment));
    }

    @PutMapping("/{id}/start-repair")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> startRepair(
            @PathVariable Long id,
            @RequestBody(required = false) MaintenanceWorkOrderStartRepairRequest request) {
        String vendor = request == null ? null : firstNonBlank(request.getRepairVendor(), request.getRepairFactory());
        String contact = request == null ? null : request.getRepairContact();
        String startTime = request == null ? null : request.getStartTime();
        return ResponseEntity.ok(maintenanceWorkOrderService.startRepair(
                id,
                parseDateTime(startTime),
                vendor,
                contact));
    }

    @PutMapping("/{id}/finish-repair")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> finishRepair(
            @PathVariable Long id,
            @RequestBody(required = false) MaintenanceWorkOrderFinishRequest request) {
        String finishTime = request == null ? null : request.getFinishTime();
        BigDecimal actualCost = request == null ? null : request.getActualCost();
        String remark = request == null ? null : request.getRemark();
        return ResponseEntity.ok(maintenanceWorkOrderService.finishRepair(
                id,
                parseDateTime(finishTime),
                actualCost,
                remark));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> accept(
            @PathVariable Long id,
            @RequestBody(required = false) MaintenanceWorkOrderAcceptRequest request,
            Authentication authentication) {
        Long userId = currentUserService.getCurrentUser(authentication).getId();
        String acceptanceResult = request == null ? null : request.getAcceptanceResult();
        String closeResultStatus = request == null ? null : request.getCloseResultStatus();
        String remark = request == null ? null : request.getRemark();
        return ResponseEntity.ok(maintenanceWorkOrderService.accept(
                id,
                userId,
                acceptanceResult,
                closeResultStatus,
                remark));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','OFFICE_ADMIN')")
    public ResponseEntity<Boolean> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) MaintenanceWorkOrderCancelRequest request) {
        String finalReason = reason;
        if ((finalReason == null || finalReason.isBlank()) && request != null) {
            finalReason = request.getReason();
        }
        return ResponseEntity.ok(maintenanceWorkOrderService.cancel(id, finalReason));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (Exception exception) {
            throw new IllegalArgumentException("时间格式不正确");
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private String normalizeBlank(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
