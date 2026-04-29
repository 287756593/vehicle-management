package com.company.vehicle.dto;

import lombok.Data;

@Data
public class MaintenanceWorkOrderAcceptRequest {
    private String acceptanceResult;
    private String closeResultStatus;
    private String remark;
}
