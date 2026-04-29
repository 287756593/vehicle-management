package com.company.vehicle.dto;

import lombok.Data;

@Data
public class MaintenanceWorkOrderFromBorrowRequest {
    private String issueDescription;
    private String expectedFinishTime;
    private String remark;
}
