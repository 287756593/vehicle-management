package com.company.vehicle.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN', 'DEPT_APPROVER')")
    public ResponseEntity<Map<String, Object>> getPage(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String driverName,
            @RequestParam(required = false) String status) {
        Page<Driver> page = driverService.getPage(current, size, driverName, status);
        Map<String, Object> result = new HashMap<>();
        result.put("data", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    public ResponseEntity<List<Driver>> getList() {
        return ResponseEntity.ok(driverService.getActiveDrivers());
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<Driver>> getActive() {
        return ResponseEntity.ok(driverService.getActiveDrivers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN', 'DEPT_APPROVER')")
    public ResponseEntity<Driver> getById(@PathVariable Long id) {
        Driver driver = driverService.getById(id);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(driver);
    }

    @PostMapping
    @PreAuthorize("hasRole('OFFICE_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Driver> create(@RequestBody Driver driver) {
        return ResponseEntity.ok(driverService.createDriverWithAccount(driver));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Driver> update(@PathVariable Long id, @RequestBody Driver driver) {
        return ResponseEntity.ok(driverService.updateDriverWithAccount(id, driver));
    }

    @PutMapping("/{id}/sort-order")
    @PreAuthorize("hasRole('OFFICE_ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<Driver> updateSortOrder(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        return ResponseEntity.ok(driverService.updateSortOrder(id, request.get("sortOrder")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (driverService.hasUnprocessedIssues(id)) {
            throw new IllegalArgumentException("该驾驶员仍有未处理违章，不能删除");
        }
        driverService.deleteDriverWithAccount(id);
        return ResponseEntity.ok().build();
    }
}
