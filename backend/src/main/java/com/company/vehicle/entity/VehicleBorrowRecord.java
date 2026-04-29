package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("vehicle_borrow_record")
public class VehicleBorrowRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;
    private Long vehicleId;
    private String plateNumber;
    private Long driverId;
    private String driverName;
    private String status;
    private String usageReason;
    private String destination;
    private LocalDateTime expectedReturnTime;

    private BigDecimal takeMileage;
    private String takeVehiclePhotos;
    private String takeMileagePhoto;
    private LocalDateTime takeTime;

    private BigDecimal returnMileage;
    private String returnVehiclePhotos;
    private String returnMileagePhoto;
    private String returnFuelPhoto;
    private Integer isClean;
    private Integer isFuelEnough;
    private String issueDescription;
    private String issuePhotos;
    private String actionRequired;
    private String followUpStatus;
    private String followUpRemark;
    private Long followUpHandledBy;
    private LocalDateTime followUpHandledTime;
    private String followUpResultStatus;
    private LocalDateTime returnTime;
    private Integer deleted;
    private LocalDateTime deletedTime;
    private Long deletedBy;
    private String deleteReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private BigDecimal tripMileage;

    @TableField(exist = false)
    private String vehicleModel;

    @TableField(exist = false)
    private String followUpHandledByName;
}
