package com.company.vehicle.controller;

import com.company.vehicle.entity.SysDept;
import com.company.vehicle.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depts")
public class DeptController {

    @Autowired
    private SysDeptService sysDeptService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN', 'DEPT_APPROVER')")
    public ResponseEntity<List<SysDept>> getAll() {
        return ResponseEntity.ok(sysDeptService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SysDept> create(@RequestBody SysDept dept) {
        return ResponseEntity.ok(sysDeptService.createDeptSafely(dept));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OFFICE_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sysDeptService.deleteDeptSafely(id);
        return ResponseEntity.ok().build();
    }
}
