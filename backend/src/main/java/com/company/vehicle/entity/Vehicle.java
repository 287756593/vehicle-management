package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("vehicle")
public class Vehicle {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String plateNumber;
    private String vehicleType;
    private String brand;
    private String model;
    private String color;
    private String vin;
    private String engineNumber;
    private LocalDate registerDate;
    private BigDecimal currentMileage;
    private BigDecimal annualFuelBudget;
    private BigDecimal annualFuelUsed;
    private String fuelReminderStatus;
    private String fuelReminderNote;
    private LocalDateTime fuelReminderTime;
    private String status;
    private String parkingLocation;
    private String insuranceCompany;
    private LocalDate insuranceExpireDate;
    private LocalDate inspectionExpireDate;
    private String drivingLicensePhoto;
    private String insurancePhoto;
    private LocalDate trafficRestrictionReleaseDate;

    private Long currentDriverId;
    private String currentDriverName;
    private String currentDestination;
    private LocalDateTime borrowTime;
    private BigDecimal borrowMileage;
    private String pickupPhotos;
    private String pickupFuelPhoto;
    private LocalDateTime returnTime;
    private String returnPhotos;
    private String returnFuelPhoto;
    private String returnMileagePhoto;
    private Integer isClean;
    private String cleanReason;

    @TableField(exist = false)
    private Integer trafficRestrictedToday;

    @TableField(exist = false)
    private Integer trafficRestrictionReleasedToday;

    @TableField(exist = false)
    private String trafficRestrictionMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
