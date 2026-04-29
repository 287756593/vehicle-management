package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vehicle_borrow_record_edit_log")
public class VehicleBorrowRecordEditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private String recordNo;
    private Long operatorId;
    private String operatorName;
    private String changeSummary;
    private String beforeSnapshot;
    private String afterSnapshot;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
