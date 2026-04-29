package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.FuelRecord;
import com.company.vehicle.entity.SysUser;
import com.company.vehicle.entity.VehicleBorrowRecord;
import com.company.vehicle.entity.ViolationRecord;
import com.company.vehicle.mapper.DriverMapper;
import com.company.vehicle.mapper.FuelRecordMapper;
import com.company.vehicle.mapper.VehicleBorrowRecordMapper;
import com.company.vehicle.mapper.ViolationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Objects;

@Service
public class DriverService extends ServiceImpl<DriverMapper, Driver> {

    @Autowired
    private ViolationRecordMapper violationRecordMapper;

    @Autowired
    private VehicleBorrowRecordMapper vehicleBorrowRecordMapper;

    @Autowired
    private FuelRecordMapper fuelRecordMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<Driver> getPage(int current, int size, String driverName, String status) {
        Page<Driver> page = new Page<>(current, size);
        LambdaQueryWrapper<Driver> wrapper = new LambdaQueryWrapper<>();
        if (driverName != null && !driverName.isEmpty()) {
            wrapper.like(Driver::getDriverName, driverName);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Driver::getStatus, status);
        }
        wrapper.orderByAsc(Driver::getSortOrder).orderByAsc(Driver::getId);
        return this.page(page, wrapper);
    }

    public List<Driver> getActiveDrivers() {
        return this.list(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getStatus, "ACTIVE")
                .orderByAsc(Driver::getSortOrder)
                .orderByAsc(Driver::getId));
    }

    public Driver getByLicenseNumber(String licenseNumber) {
        return this.getOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getLicenseNumber, licenseNumber));
    }

    public Driver getByUserId(Long userId) {
        return this.getOne(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getUserId, userId));
    }

    @Transactional
    public Driver createDriverWithAccount(Driver driver) {
        if (driver.getStatus() == null || driver.getStatus().isBlank()) {
            driver.setStatus("ACTIVE");
        }
        normalizeSortOrder(driver);
        this.save(driver);
        if (driver.getSortOrder() == null) {
            driver.setSortOrder(Math.toIntExact(driver.getId()));
            this.lambdaUpdate()
                    .eq(Driver::getId, driver.getId())
                    .set(Driver::getSortOrder, driver.getSortOrder())
                    .update();
        }
        ensureUserAccount(driver.getId());
        return this.getById(driver.getId());
    }

    @Transactional
    public Driver updateDriverWithAccount(Long id, Driver driver) {
        Driver existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("驾驶员不存在");
        }

        driver.setId(id);
        if (driver.getUserId() == null) {
            driver.setUserId(existing.getUserId());
        }
        if (driver.getSortOrder() == null) {
            driver.setSortOrder(existing.getSortOrder());
        }
        normalizeSortOrder(driver);
        this.updateById(driver);

        Driver saved = this.getById(id);
        syncUserAccount(saved);
        return this.getById(id);
    }

    @Transactional
    public Driver updateSortOrder(Long id, Integer sortOrder) {
        Driver existing = this.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("驾驶员不存在");
        }
        if (sortOrder == null || sortOrder < 1) {
            throw new IllegalArgumentException("登录排序必须大于 0");
        }
        if (Objects.equals(existing.getSortOrder(), sortOrder)) {
            return existing;
        }

        // 使用数据库级别的更新避免竞态条件
        boolean updated = this.lambdaUpdate()
                .eq(Driver::getId, id)
                .eq(Driver::getSortOrder, existing.getSortOrder()) // 乐观锁检查
                .set(Driver::getSortOrder, sortOrder)
                .update();

        if (!updated) {
            throw new IllegalStateException("更新失败，驾驶员排序可能已被其他操作修改，请刷新后重试");
        }

        return this.getById(id);
    }

    @Transactional
    public void deleteDriverWithAccount(Long id) {
        Driver existing = this.getById(id);
        if (existing == null) {
            return;
        }
        Long activeBorrowCount = vehicleBorrowRecordMapper.selectCount(new LambdaQueryWrapper<VehicleBorrowRecord>()
                .eq(VehicleBorrowRecord::getDriverId, id)
                .eq(VehicleBorrowRecord::getStatus, "TAKEN"));
        if (activeBorrowCount != null && activeBorrowCount > 0) {
            throw new IllegalArgumentException("该驾驶员当前还有未归还车辆，不能删除，请先完成还车");
        }

        Long fuelRecordCount = fuelRecordMapper.selectCount(new LambdaQueryWrapper<FuelRecord>()
                .eq(FuelRecord::getDriverId, id));
        if (fuelRecordCount != null && fuelRecordCount > 0) {
            throw new IllegalArgumentException("该驾驶员已有加油记录，不能删除，请改为标记离职");
        }

        if (existing.getUserId() != null) {
            SysUser user = sysUserService.getById(existing.getUserId());
            if (user != null) {
                user.setStatus(0);
                sysUserService.updateById(user);
            }
        }
        this.removeById(id);
    }

    @Transactional
    public Driver ensureUserAccount(Long driverId) {
        Driver driver = this.getById(driverId);
        if (driver == null) {
            return null;
        }
        syncUserAccount(driver);
        return this.getById(driverId);
    }

    @Transactional
    public int ensureAllDriverAccounts() {
        List<Driver> drivers = this.list();
        int fixedCount = 0;
        for (Driver driver : drivers) {
            Long beforeUserId = driver.getUserId();
            syncUserAccount(driver);
            Driver refreshed = this.getById(driver.getId());
            if (beforeUserId == null && refreshed != null && refreshed.getUserId() != null) {
                fixedCount++;
            }
        }
        return fixedCount;
    }

    public boolean hasUnprocessedIssues(Long driverId) {
        long violationCount = violationRecordMapper.selectCount(new LambdaQueryWrapper<ViolationRecord>()
                .eq(ViolationRecord::getDriverId, driverId)
                .ne(ViolationRecord::getStatus, "COMPLETED"));
        return violationCount > 0;
    }

    private void syncUserAccount(Driver driver) {
        if (driver == null) {
            return;
        }

        SysUser user = null;
        if (driver.getUserId() != null) {
            user = sysUserService.getById(driver.getUserId());
        }

        if (user == null) {
            user = new SysUser();
            user.setUsername(generateDriverUsername(driver.getId()));
            // 使用随机密码，首次登录后需要修改
            String randomPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
            user.setPassword(passwordEncoder.encode(randomPassword));
            user.setRole("DRIVER");
            log.debug("为驾驶员创建账户: " + driver.getDriverName() + ", 临时密码: " + randomPassword);
        }

        user.setRealName(driver.getDriverName());
        user.setPhone(driver.getPhone());
        user.setDeptId(driver.getDeptId());
        user.setRole("DRIVER");
        user.setStatus("ACTIVE".equals(driver.getStatus()) ? 1 : 0);

        if (user.getId() == null) {
            sysUserService.save(user);
            this.lambdaUpdate()
                    .eq(Driver::getId, driver.getId())
                    .set(Driver::getUserId, user.getId())
                    .update();
            driver.setUserId(user.getId());
        } else {
            sysUserService.updateById(user);
        }
    }

    private String generateDriverUsername(Long driverId) {
        String base = "driver_" + driverId;
        if (sysUserService.getByUsername(base) == null) {
            return base;
        }

        // 限制最大尝试次数，防止无限循环
        String candidate = base;
        int suffix = 1;
        int maxAttempts = 100;
        while (sysUserService.getByUsername(candidate) != null && suffix <= maxAttempts) {
            candidate = base + "_" + suffix;
            suffix++;
        }

        if (suffix > maxAttempts) {
            // 如果超过最大尝试次数，使用UUID确保唯一性
            candidate = base + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        }

        return candidate;
    }

    private void normalizeSortOrder(Driver driver) {
        if (driver == null || driver.getSortOrder() == null) {
            return;
        }
        if (driver.getSortOrder() < 1) {
            throw new IllegalArgumentException("登录排序必须大于 0");
        }
    }
}
