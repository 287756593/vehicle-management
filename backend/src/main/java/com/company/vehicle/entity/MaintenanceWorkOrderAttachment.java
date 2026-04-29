package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("maintenance_work_order_attachment")
public class MaintenanceWorkOrderAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private String category; // ISSUE, QUOTE, APPROVAL, REPAIR, INVOICE, ACCEPTANCE
    private String filePath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
