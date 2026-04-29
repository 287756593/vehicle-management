package com.company.vehicle.controller;

import com.company.vehicle.service.CurrentUserService;
import com.company.vehicle.service.TodoCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/todo-center")
public class TodoCenterController {

    private final TodoCenterService todoCenterService;
    private final CurrentUserService currentUserService;

    public TodoCenterController(TodoCenterService todoCenterService, CurrentUserService currentUserService) {
        this.todoCenterService = todoCenterService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE')")
    public ResponseEntity<Map<String, Object>> getSummary(Authentication authentication) {
        String role = currentUserService.getCurrentRole(authentication);
        return ResponseEntity.ok(todoCenterService.getSummary(role));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OFFICE_ADMIN', 'FINANCE')")
    public ResponseEntity<Map<String, Object>> getItems(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long vehicleId,
            Authentication authentication) {
        String role = currentUserService.getCurrentRole(authentication);
        return ResponseEntity.ok(todoCenterService.getItems(role, current, size, type, priority, keyword, vehicleId));
    }
}
