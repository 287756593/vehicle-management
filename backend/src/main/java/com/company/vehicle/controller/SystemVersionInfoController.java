package com.company.vehicle.controller;

import com.company.vehicle.entity.SysUser;
import com.company.vehicle.entity.SystemVersionInfo;
import com.company.vehicle.service.CurrentUserService;
import com.company.vehicle.service.SystemVersionInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system-versions")
public class SystemVersionInfoController {

    private final SystemVersionInfoService systemVersionInfoService;
    private final CurrentUserService currentUserService;

    public SystemVersionInfoController(
            SystemVersionInfoService systemVersionInfoService,
            CurrentUserService currentUserService) {
        this.systemVersionInfoService = systemVersionInfoService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<List<SystemVersionInfo>> listVersions() {
        return ResponseEntity.ok(systemVersionInfoService.listVersions());
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<SystemVersionInfo> getCurrentVersion() {
        return ResponseEntity.ok(systemVersionInfoService.getCurrentVersion());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<SystemVersionInfo> createVersion(
            @RequestBody SystemVersionInfo request,
            Authentication authentication) {
        return ResponseEntity.ok(systemVersionInfoService.createVersion(request, resolveOperatorName(authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<SystemVersionInfo> updateVersion(
            @PathVariable Long id,
            @RequestBody SystemVersionInfo request) {
        return ResponseEntity.ok(systemVersionInfoService.updateVersion(id, request));
    }

    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN')")
    public ResponseEntity<SystemVersionInfo> setCurrentVersion(@PathVariable Long id) {
        return ResponseEntity.ok(systemVersionInfoService.setCurrentVersion(id));
    }

    private String resolveOperatorName(Authentication authentication) {
        SysUser currentUser = currentUserService.getCurrentUser(authentication);
        if (currentUser == null) {
            return null;
        }
        return currentUser.getRealName() != null && !currentUser.getRealName().isBlank()
                ? currentUser.getRealName()
                : currentUser.getUsername();
    }
}
