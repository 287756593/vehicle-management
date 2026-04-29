package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("driver")
public class Driver {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String driverName;
    private String licenseNumber;
    private String licenseType;
    private Integer driveAge;
    private String licensePhoto;
    private String status;
    private Long deptId;
    private String phone;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
