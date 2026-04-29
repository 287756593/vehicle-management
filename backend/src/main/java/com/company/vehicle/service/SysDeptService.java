package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.SysDept;
import com.company.vehicle.entity.SysUser;
import com.company.vehicle.mapper.DriverMapper;
import com.company.vehicle.mapper.SysDeptMapper;
import com.company.vehicle.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysDeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    private final DriverMapper driverMapper;
    private final SysUserMapper sysUserMapper;

    public SysDeptService(DriverMapper driverMapper, SysUserMapper sysUserMapper) {
        this.driverMapper = driverMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Transactional
    public SysDept createDeptSafely(SysDept dept) {
        if (dept == null) {
            throw new IllegalArgumentException("部门信息不能为空");
        }
        String deptName = normalizeRequiredText(dept.getDeptName(), "部门名称不能为空");
        String deptCode = normalizeRequiredText(dept.getDeptCode(), "部门编码不能为空");
        if (this.lambdaQuery().eq(SysDept::getDeptName, deptName).count() > 0) {
            throw new IllegalArgumentException("该部门名称已存在");
        }
        if (this.lambdaQuery().eq(SysDept::getDeptCode, deptCode).count() > 0) {
            throw new IllegalArgumentException("该部门编码已存在");
        }
        dept.setDeptName(deptName);
        dept.setDeptCode(deptCode);
        if (!this.save(dept)) {
            throw new IllegalStateException("新增部门失败");
        }
        return dept;
    }

    @Transactional
    public void deleteDeptSafely(Long id) {
        SysDept dept = this.getById(id);
        if (dept == null) {
            return;
        }

        long driverCount = driverMapper.selectCount(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getDeptId, id));
        if (driverCount > 0) {
            throw new IllegalArgumentException("该部门下仍有关联驾驶员，不能删除，请先调整驾驶员所属部门");
        }

        long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id)
                .ne(SysUser::getRole, "DRIVER"));
        if (userCount > 0) {
            throw new IllegalArgumentException("该部门下仍有关联账号，不能删除，请先调整账号所属部门");
        }

        this.removeById(id);
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
