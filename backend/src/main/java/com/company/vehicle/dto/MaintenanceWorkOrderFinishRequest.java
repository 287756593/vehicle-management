package com.company.vehicle.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MaintenanceWorkOrderFinishRequest {
    private String finishTime;
    private BigDecimal actualCost;
    private String remark;
}
