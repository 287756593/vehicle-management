package com.company.vehicle.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DriverVehicleActivityItem {
    private Long vehicleId;
    private String plateNumber;
    private String vehicleType;
    private String brand;
    private String model;
    private String parkingLocation;
    private String status;
    private String statusLabel;
    private String activityType;
    private String activityTitle;
    private String activitySubtitle;
    private Integer priority;
    private Boolean availableForBorrow;

    private Long borrowRecordId;
    private String currentDriverName;
    private String currentDestination;
    private String usageReason;
    private LocalDateTime borrowTime;
    private LocalDateTime expectedReturnTime;
    private String followUpStatus;
    private String actionRequired;

    private Long maintenanceOrderId;
    private String maintenanceStatus;
    private String maintenanceStatusLabel;
    private String maintenanceIssueDescription;
    private String repairVendor;
    private LocalDateTime expectedFinishTime;
    private LocalDateTime repairStartTime;
    private LocalDateTime repairFinishTime;

    private String fuelReminderStatus;
    private String fuelReminderNote;
    private LocalDateTime fuelReminderTime;
    private Integer trafficRestrictedToday;
    private Integer trafficRestrictionReleasedToday;
    private String trafficRestrictionMessage;
    private LocalDateTime updateTime;
}
