package com.company.vehicle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deptName;
    private Long parentId;
    private String deptCode;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
