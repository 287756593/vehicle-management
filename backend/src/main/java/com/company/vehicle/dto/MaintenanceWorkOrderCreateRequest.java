package com.company.vehicle.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaintenanceWorkOrderCreateRequest {
    private Long vehicleId;
    private String plateNumber;
    private String workType;
    private String issueDescription;
    private String faultDescription;
    private BigDecimal estimatedCost;
    private String expectedFinishTime;
    private String repairVendor;
    private String repairFactory;
    private String repairContact;
    private String remark;
}
