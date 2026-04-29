package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.SystemVersionInfo;
import com.company.vehicle.mapper.SystemVersionInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemVersionInfoService extends ServiceImpl<SystemVersionInfoMapper, SystemVersionInfo> {

    public List<SystemVersionInfo> listVersions() {
        return this.list(new LambdaQueryWrapper<SystemVersionInfo>()
                .orderByDesc(SystemVersionInfo::getIsCurrent)
                .orderByDesc(SystemVersionInfo::getReleaseTime)
                .orderByDesc(SystemVersionInfo::getId));
    }

    public SystemVersionInfo getCurrentVersion() {
        SystemVersionInfo current = this.lambdaQuery()
                .eq(SystemVersionInfo::getIsCurrent, 1)
                .orderByDesc(SystemVersionInfo::getReleaseTime)
                .orderByDesc(SystemVersionInfo::getId)
                .last("limit 1")
                .one();
        if (current != null) {
            return current;
        }
        return this.lambdaQuery()
                .orderByDesc(SystemVersionInfo::getReleaseTime)
                .orderByDesc(SystemVersionInfo::getId)
                .last("limit 1")
                .one();
    }

    @Transactional
    public SystemVersionInfo createVersion(SystemVersionInfo request, String operatorName) {
        SystemVersionInfo version = new SystemVersionInfo();
        version.setVersionNo(normalizeRequiredText(request.getVersionNo(), "版本号不能为空"));
        version.setVersionTitle(normalizeNullableText(request.getVersionTitle()));
        version.setChangeLog(normalizeRequiredText(request.getChangeLog(), "更新日志不能为空"));
        version.setReleaseTime(request.getReleaseTime() != null ? request.getReleaseTime() : LocalDateTime.now());
        version.setCreatedBy(normalizeNullableText(operatorName));
        version.setIsCurrent(resolveCurrentFlag(request.getIsCurrent(), true, false));
        if (Integer.valueOf(1).equals(version.getIsCurrent())) {
            clearCurrentVersion(null);
        }
        if (!this.save(version)) {
            throw new IllegalStateException("保存版本信息失败");
        }
        return this.getById(version.getId());
    }

    @Transactional
    public SystemVersionInfo updateVersion(Long id, SystemVersionInfo request) {
        SystemVersionInfo existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("版本记录不存在");
        }
        existing.setVersionNo(normalizeRequiredText(request.getVersionNo(), "版本号不能为空"));
        existing.setVersionTitle(normalizeNullableText(request.getVersionTitle()));
        existing.setChangeLog(normalizeRequiredText(request.getChangeLog(), "更新日志不能为空"));
        existing.setReleaseTime(request.getReleaseTime() != null ? request.getReleaseTime() : existing.getReleaseTime());
        Integer nextCurrent = resolveCurrentFlag(request.getIsCurrent(), false, Integer.valueOf(1).equals(existing.getIsCurrent()));
        existing.setIsCurrent(nextCurrent);
        if (Integer.valueOf(1).equals(nextCurrent)) {
            clearCurrentVersion(id);
        }
        if (!this.updateById(existing)) {
            throw new IllegalStateException("更新版本信息失败");
        }
        return this.getById(id);
    }

    @Transactional
    public SystemVersionInfo setCurrentVersion(Long id) {
        SystemVersionInfo existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("版本记录不存在");
        }
        clearCurrentVersion(id);
        existing.setIsCurrent(1);
        if (!this.updateById(existing)) {
            throw new IllegalStateException("设置当前版本失败");
        }
        return this.getById(id);
    }

    private void clearCurrentVersion(Long excludeId) {
        this.lambdaUpdate()
                .ne(excludeId != null, SystemVersionInfo::getId, excludeId)
                .eq(SystemVersionInfo::getIsCurrent, 1)
                .set(SystemVersionInfo::getIsCurrent, 0)
                .update();
    }

    private Integer resolveCurrentFlag(Integer requestedCurrent, boolean creating, boolean keepCurrentWhenUnchecked) {
        if (Integer.valueOf(1).equals(requestedCurrent)) {
            return 1;
        }
        if (keepCurrentWhenUnchecked) {
            return 1;
        }
        if (creating && this.lambdaQuery().eq(SystemVersionInfo::getIsCurrent, 1).count() == 0) {
            return 1;
        }
        return 0;
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
