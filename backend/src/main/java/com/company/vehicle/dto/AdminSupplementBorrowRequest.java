package com.company.vehicle.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSupplementBorrowRequest {
    private Long vehicleId;
    private Long driverId;
    private String usageReason;
    private String destination;
    private String takeTime;
    private String expectedReturnTime;
    private BigDecimal takeMileage;
}
