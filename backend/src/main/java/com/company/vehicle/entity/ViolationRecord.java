package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@TableName("violation_record")
public class ViolationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long vehicleId;
    private Long driverId;
    private Long applyId;
    private LocalDateTime violationTime;
    private String violationLocation;
    private String violationType;
    private String violationContent;
    private BigDecimal penaltyAmount;
    private Integer penaltyPoints;
    private String status;
    private LocalDate handleDeadline;
    private LocalDateTime handleTime;
    private String handlePhoto;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
