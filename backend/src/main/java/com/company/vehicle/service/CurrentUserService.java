package com.company.vehicle.service;

import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.SysUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final SysUserService sysUserService;
    private final DriverService driverService;

    public CurrentUserService(SysUserService sysUserService, DriverService driverService) {
        this.sysUserService = sysUserService;
        this.driverService = driverService;
    }

    public SysUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("未登录");
        }

        SysUser user = sysUserService.getByUsername(authentication.getName());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new AccessDeniedException("当前用户不可用");
        }
        return user;
    }

    public Driver getCurrentDriver(Authentication authentication) {
        SysUser user = getCurrentUser(authentication);
        Driver driver = driverService.getByUserId(user.getId());
        if (driver == null || !"ACTIVE".equals(driver.getStatus())) {
            throw new AccessDeniedException("驾驶员信息不可用");
        }
        return driver;
    }

    public String getCurrentRole(Authentication authentication) {
        return getCurrentUser(authentication).getRole();
    }
}
