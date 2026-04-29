package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("maintenance_work_order")
public class MaintenanceWorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private Long vehicleId;
    private String plateNumberSnapshot;
    private String sourceType;
    private Long sourceRecordId;
    private String workType;
    private String status;
    private String issueDescription;
    private String issuePhotos;
    private Long reportedBy;
    private LocalDateTime reportedTime;
    private String repairVendor;
    private String repairContact;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private LocalDateTime plannedStartTime;
    private LocalDateTime expectedFinishTime;
    private LocalDateTime repairStartTime;
    private LocalDateTime repairFinishTime;
    private Long acceptedBy;
    private LocalDateTime acceptedTime;
    private String acceptanceResult;
    private String closeResultStatus;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private List<MaintenanceWorkOrderAttachment> attachments;
}
