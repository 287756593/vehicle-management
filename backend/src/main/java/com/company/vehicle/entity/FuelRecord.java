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
@TableName("fuel_record")
public class FuelRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vehicleId;
    private Long driverId;
    private String fuelType;
    private BigDecimal fuelAmount;
    private BigDecimal fuelPrice;
    private BigDecimal totalAmount;
    private BigDecimal fuelMileage;
    private LocalDateTime fuelDate;
    private String fuelLocation;
    private Integer isCash;
    private String cashPhoto;
    private String cashReason;
    private String leaderApprovalPhoto;
    private String fuelGaugePhoto;
    private Integer isFuelEnoughAfterFuel;
    private Integer budgetYear;
    private String cashReportStatus;
    private Long cashReportApproveBy;
    private LocalDateTime cashReportApproveTime;
    private String reimbursementStatus;
    private LocalDateTime reimbursedTime;
    private String invoicePhoto;
    private String remark;
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String plateNumber;

    @TableField(exist = false)
    private String vehicleModel;

    @TableField(exist = false)
    private String driverName;
}
