package com.company.vehicle.dto;

import lombok.Data;

@Data
public class MaintenanceWorkOrderStartRepairRequest {
    private String startTime;
    private String repairVendor;
    private String repairFactory;
    private String repairContact;
}
