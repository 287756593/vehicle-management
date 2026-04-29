package com.company.vehicle.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private Long driverId;
    private String username;
    private String realName;
    private String role;
    private String deptName;
}
