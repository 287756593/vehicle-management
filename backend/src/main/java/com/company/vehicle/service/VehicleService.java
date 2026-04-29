package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.FuelRecord;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.entity.VehicleBorrowRecord;
import com.company.vehicle.mapper.FuelRecordMapper;
import com.company.vehicle.mapper.VehicleMapper;
import com.company.vehicle.mapper.VehicleBorrowRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleService extends ServiceImpl<VehicleMapper, Vehicle> {

    @Autowired
    private VehicleBorrowRecordMapper vehicleBorrowRecordMapper;

    @Autowired
    private FuelRecordMapper fuelRecordMapper;

    public Page<Vehicle> getPage(int current, int size, String plateNumber, String status) {
        Page<Vehicle> page = new Page<>(current, size);
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        if (plateNumber != null && !plateNumber.isEmpty()) {
            wrapper.like(Vehicle::getPlateNumber, plateNumber);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Vehicle::getStatus, status);
        }
        wrapper.orderByDesc(Vehicle::getCreateTime);
        Page<Vehicle> result = this.page(page, wrapper);
        enrichTrafficRestriction(result.getRecords());
        return result;
    }

    public List<Vehicle> getAvailableVehicles() {
        List<Vehicle> vehicles = this.list(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getStatus, "NORMAL"));
        enrichTrafficRestriction(vehicles);
        return vehicles.stream()
                .filter(vehicle -> !isRestrictedForBorrowToday(vehicle))
                .toList();
    }

    public long countAvailable() {
        List<Vehicle> vehicles = this.list(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getStatus, "NORMAL"));
        enrichTrafficRestriction(vehicles);
        return vehicles.stream()
                .filter(vehicle -> !isRestrictedForBorrowToday(vehicle))
                .count();
    }
    public Map<String, Object> getOverview() {
        List<Vehicle> all = this.list();
        enrichTrafficRestriction(all);
        int total = all.size();
        int available = 0;
        int inUse = 0;
        int restrictedToday = 0;
        int needsAttention = 0;
        int pendingFuel = 0;
        int pendingCheck = 0;
        for (Vehicle v : all) {
            boolean restrictedAndNotReleased = Integer.valueOf(1).equals(v.getTrafficRestrictedToday())
                    && !Integer.valueOf(1).equals(v.getTrafficRestrictionReleasedToday());
            if ("NORMAL".equals(v.getStatus()) && !restrictedAndNotReleased) {
                available++;
            }
            if ("IN_USE".equals(v.getStatus())) {
                inUse++;
            }
            if (restrictedAndNotReleased) {
                restrictedToday++;
            }
            if ("PENDING".equals(v.getFuelReminderStatus())) {
                pendingFuel++;
            }
            if ("PENDING_CHECK".equals(v.getStatus())) {
                pendingCheck++;
            }
            if (restrictedAndNotReleased
                    || "PENDING_CHECK".equals(v.getStatus())
                    || "MAINTENANCE".equals(v.getStatus())
                    || "PENDING".equals(v.getFuelReminderStatus())) {
                needsAttention++;
            }
        }
        Map<String, Object> overview = new java.util.LinkedHashMap<>();
        overview.put("total", total);
        overview.put("available", available);
        overview.put("inUse", inUse);
        overview.put("restrictedToday", restrictedToday);
        overview.put("needsAttention", needsAttention);
        overview.put("pendingFuel", pendingFuel);
        overview.put("pendingCheck", pendingCheck);
        return overview;
    }


    public Vehicle getByPlateNumber(String plateNumber) {
        return this.getOne(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getPlateNumber, plateNumber));
    }

    public Vehicle getByIdForUpdate(Long vehicleId) {
        return this.baseMapper.selectByIdForUpdate(vehicleId);
    }

    public void markMaintenance(Long vehicleId) {
        this.lambdaUpdate()
                .eq(Vehicle::getId, vehicleId)
                .set(Vehicle::getStatus, "MAINTENANCE")
                .update();
    }

    public void resetStatusIfNoActiveMaintenance(Long vehicleId) {
        this.lambdaUpdate()
                .eq(Vehicle::getId, vehicleId)
                .set(Vehicle::getStatus, "NORMAL")
                .update();
    }

    public Vehicle getVehicleDetail(Long id) {
        Vehicle vehicle = this.getById(id);
        enrichTrafficRestriction(vehicle);
        return vehicle;
    }

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆信息不能为空");
        }
        normalizeVehicle(vehicle, null);
        if (!this.save(vehicle)) {
            throw new IllegalStateException("新增车辆失败");
        }
        return getVehicleDetail(vehicle.getId());
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle vehicle) {
        Vehicle existing = this.getById(id);
        if (existing == null) {
            return null;
        }
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆信息不能为空");
        }
        vehicle.setId(id);
        normalizeVehicle(vehicle, existing);
        if (!this.updateById(vehicle)) {
            throw new IllegalStateException("更新车辆失败");
        }
        return getVehicleDetail(id);
    }

    @Transactional
    public Vehicle updateTrafficRestrictionRelease(Long id, boolean released) {
        Vehicle vehicle = getByIdForUpdate(id);
        if (vehicle == null) {
            return null;
        }
        boolean updated = this.lambdaUpdate()
                .eq(Vehicle::getId, id)
                .set(Vehicle::getTrafficRestrictionReleaseDate, released ? LocalDate.now() : null)
                .update();
        if (!updated) {
            throw new IllegalStateException("更新限行放行状态失败");
        }
        return getVehicleDetail(id);
    }

    public void updateMileage(Long vehicleId, BigDecimal mileage) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle != null) {
            vehicle.setCurrentMileage(mileage);
            this.updateById(vehicle);
        }
    }

    public void deleteVehicleSafely(Long vehicleId) {
        Vehicle vehicle = this.getById(vehicleId);
        if (vehicle == null) {
            return;
        }
        if ("IN_USE".equals(vehicle.getStatus())) {
            throw new IllegalArgumentException("车辆当前正在使用中，不能删除");
        }

        Long activeBorrowCount = vehicleBorrowRecordMapper.selectCount(new LambdaQueryWrapper<VehicleBorrowRecord>()
                .eq(VehicleBorrowRecord::getVehicleId, vehicleId)
                .eq(VehicleBorrowRecord::getStatus, "TAKEN"));
        if (activeBorrowCount != null && activeBorrowCount > 0) {
            throw new IllegalArgumentException("该车辆当前还有未归还记录，不能删除");
        }

        Long borrowHistoryCount = vehicleBorrowRecordMapper.selectCount(new LambdaQueryWrapper<VehicleBorrowRecord>()
                .eq(VehicleBorrowRecord::getVehicleId, vehicleId));
        if (borrowHistoryCount != null && borrowHistoryCount > 0) {
            throw new IllegalArgumentException("该车辆已有借还车记录，不能删除，请保留档案或调整车辆状态");
        }

        Long fuelRecordCount = fuelRecordMapper.selectCount(new LambdaQueryWrapper<FuelRecord>()
                .eq(FuelRecord::getVehicleId, vehicleId));
        if (fuelRecordCount != null && fuelRecordCount > 0) {
            throw new IllegalArgumentException("该车辆已有加油记录，不能删除，请保留档案或调整车辆状态");
        }

        this.removeById(vehicleId);
    }

    private void normalizeVehicle(Vehicle vehicle, Vehicle existing) {
        String plateNumber = normalizeRequiredText(vehicle.getPlateNumber(), "请输入车牌号");
        if (existsPlateNumber(plateNumber, existing == null ? null : existing.getId())) {
            throw new IllegalArgumentException("该车牌号已存在，请勿重复录入");
        }

        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType(normalizeNullableText(vehicle.getVehicleType()));
        vehicle.setBrand(normalizeNullableText(vehicle.getBrand()));
        vehicle.setModel(normalizeNullableText(vehicle.getModel()));
        vehicle.setColor(normalizeNullableText(vehicle.getColor()));
        vehicle.setVin(normalizeNullableText(vehicle.getVin()));
        vehicle.setEngineNumber(normalizeNullableText(vehicle.getEngineNumber()));
        vehicle.setParkingLocation(normalizeNullableText(vehicle.getParkingLocation()));
        vehicle.setInsuranceCompany(normalizeNullableText(vehicle.getInsuranceCompany()));

        if (vehicle.getCurrentMileage() == null) {
            vehicle.setCurrentMileage(existing != null && existing.getCurrentMileage() != null
                    ? existing.getCurrentMileage()
                    : BigDecimal.ZERO);
        }
        if (vehicle.getAnnualFuelBudget() == null && existing != null) {
            vehicle.setAnnualFuelBudget(existing.getAnnualFuelBudget());
        }
        if (vehicle.getAnnualFuelUsed() == null && existing != null) {
            vehicle.setAnnualFuelUsed(existing.getAnnualFuelUsed());
        }
        if (vehicle.getTrafficRestrictionReleaseDate() == null && existing != null) {
            vehicle.setTrafficRestrictionReleaseDate(existing.getTrafficRestrictionReleaseDate());
        }

        String nextStatus = normalizeNullableText(vehicle.getStatus());
        if (nextStatus == null) {
            nextStatus = existing != null && normalizeNullableText(existing.getStatus()) != null
                    ? normalizeNullableText(existing.getStatus())
                    : "NORMAL";
        }
        vehicle.setStatus(nextStatus);
    }

    private boolean existsPlateNumber(String plateNumber, Long excludeId) {
        return this.lambdaQuery()
                .eq(Vehicle::getPlateNumber, plateNumber)
                .ne(excludeId != null, Vehicle::getId, excludeId)
                .count() > 0;
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

    public void validateCanBorrowToday(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        enrichTrafficRestriction(vehicle);
        if (isRestrictedForBorrowToday(vehicle)) {
            throw new IllegalArgumentException(vehicle.getTrafficRestrictionMessage());
        }
    }

    public void enrichTrafficRestriction(Collection<Vehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return;
        }
        vehicles.forEach(this::enrichTrafficRestriction);
    }

    public void enrichTrafficRestriction(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        boolean restrictedToday = isPlateRestrictedToday(vehicle.getPlateNumber(), LocalDate.now());
        boolean releasedToday = isRestrictionReleasedToday(vehicle);
        vehicle.setTrafficRestrictedToday(restrictedToday ? 1 : 0);
        vehicle.setTrafficRestrictionReleasedToday(releasedToday ? 1 : 0);
        vehicle.setTrafficRestrictionMessage(buildTrafficRestrictionMessage(vehicle.getPlateNumber(), restrictedToday, releasedToday));
    }

    private boolean isRestrictedForBorrowToday(Vehicle vehicle) {
        return Integer.valueOf(1).equals(vehicle.getTrafficRestrictedToday())
                && !Integer.valueOf(1).equals(vehicle.getTrafficRestrictionReleasedToday());
    }

    private boolean isRestrictionReleasedToday(Vehicle vehicle) {
        return vehicle != null
                && vehicle.getTrafficRestrictionReleaseDate() != null
                && LocalDate.now().isEqual(vehicle.getTrafficRestrictionReleaseDate());
    }

    private boolean isPlateRestrictedToday(String plateNumber, LocalDate today) {
        if (today == null) {
            return false;
        }
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        Integer lastDigit = resolveLastPlateDigit(plateNumber);
        if (lastDigit == null) {
            return false;
        }

        return switch (dayOfWeek) {
            case MONDAY -> lastDigit == 1 || lastDigit == 6;
            case TUESDAY -> lastDigit == 2 || lastDigit == 7;
            case WEDNESDAY -> lastDigit == 3 || lastDigit == 8;
            case THURSDAY -> lastDigit == 4 || lastDigit == 9;
            case FRIDAY -> lastDigit == 5 || lastDigit == 0;
            default -> false;
        };
    }

    private Integer resolveLastPlateDigit(String plateNumber) {
        if (plateNumber == null || plateNumber.isBlank()) {
            return null;
        }
        for (int index = plateNumber.length() - 1; index >= 0; index--) {
            char current = plateNumber.charAt(index);
            if (Character.isDigit(current)) {
                return current - '0';
            }
        }
        return null;
    }

    private String buildTrafficRestrictionMessage(String plateNumber, boolean restrictedToday, boolean releasedToday) {
        if (!restrictedToday) {
            return null;
        }
        String weekdayRule = getTodayRestrictionRule();
        if (releasedToday) {
            return String.format("%s 今日尾号限行（%s），但已由管理员手动放行", plateNumber, weekdayRule);
        }
        return String.format("%s 今日尾号限行（%s），当前禁止借用；如需出车，请先由管理员手动放行", plateNumber, weekdayRule);
    }

    private String getTodayRestrictionRule() {
        return switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY -> "周一限行尾号 1 和 6";
            case TUESDAY -> "周二限行尾号 2 和 7";
            case WEDNESDAY -> "周三限行尾号 3 和 8";
            case THURSDAY -> "周四限行尾号 4 和 9";
            case FRIDAY -> "周五限行尾号 5 和 0";
            default -> "今日不限行";
        };
    }
}
