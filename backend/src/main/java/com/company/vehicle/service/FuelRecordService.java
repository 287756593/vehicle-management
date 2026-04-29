package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.FuelRecord;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.mapper.FuelRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FuelRecordService extends ServiceImpl<FuelRecordMapper, FuelRecord> {

    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final UploadStorageService uploadStorageService;

    public FuelRecordService(
            VehicleService vehicleService,
            DriverService driverService,
            UploadStorageService uploadStorageService) {
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        this.uploadStorageService = uploadStorageService;
    }

    @Transactional(rollbackFor = Exception.class)
    public FuelRecord createRecord(FuelRecord record) {
        requireVehicleForUpdate(record.getVehicleId());
        normalizeRecord(record);
        if (!this.save(record)) {
            throw new IllegalStateException("保存加油记录失败");
        }
        afterRecordChanged(record, false);
        enrichRecord(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public FuelRecord createRecordWithPhotos(
            Long vehicleId,
            Long driverId,
            String fuelType,
            String fuelAmount,
            String fuelPrice,
            String totalAmount,
            String fuelMileage,
            String fuelDate,
            String fuelLocation,
            Integer isCash,
            String cashReason,
            Integer isFuelEnoughAfterFuel,
            MultipartFile[] fuelPhotos,
            MultipartFile[] fuelGaugePhotos,
            MultipartFile[] cashPhotos,
            MultipartFile[] leaderApprovalPhotos) {

        requireVehicleForUpdate(vehicleId);
        Driver driver = requireDriver(driverId);

        String voucherPhotos = uploadStorageService.saveImageFiles(fuelPhotos, "加油凭证照片");
        String fuelGaugePhoto = uploadStorageService.saveImageFiles(fuelGaugePhotos, "加油后油表照片");
        String cashReceiptPhotos = uploadStorageService.saveImageFiles(cashPhotos, "现金加油小票");
        String approvalPhotos = uploadStorageService.saveImageFiles(leaderApprovalPhotos, "领导同意截图");

        FuelRecord record = new FuelRecord();
        record.setVehicleId(vehicleId);
        record.setDriverId(driver.getId());
        record.setFuelType(normalizeRequiredText(fuelType, "请选择油品类型"));
        record.setFuelAmount(parseOptionalDecimal(fuelAmount, "加油量"));
        record.setFuelPrice(parseOptionalDecimal(fuelPrice, "油价"));
        record.setTotalAmount(parseOptionalDecimal(totalAmount, "总金额"));
        record.setFuelMileage(parseOptionalDecimal(fuelMileage, "加油时公里数"));
        record.setFuelDate(parseDateTime(fuelDate, "加油时间"));
        record.setFuelLocation(normalizeNullableText(fuelLocation));
        record.setIsCash(isCash);
        record.setCashReason(normalizeNullableText(cashReason));
        record.setIsFuelEnoughAfterFuel(isFuelEnoughAfterFuel);
        record.setInvoicePhoto(voucherPhotos);
        record.setFuelGaugePhoto(fuelGaugePhoto);
        record.setCashPhoto(cashReceiptPhotos);
        record.setLeaderApprovalPhoto(approvalPhotos);

        normalizeRecord(record);
        if (!this.save(record)) {
            throw new IllegalStateException("保存加油记录失败");
        }
        afterRecordChanged(record, false);
        enrichRecord(record);
        return record;
    }

    public Page<FuelRecord> getPage(int current, int size, Long vehicleId, Long driverId, Integer isCash, Integer year, String status) {
        Page<FuelRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<FuelRecord> wrapper = applyStatusFilter(buildBaseQueryWrapper(vehicleId, driverId, isCash, year), status);
        wrapper.orderByDesc(FuelRecord::getFuelDate);
        Page<FuelRecord> result = this.page(page, wrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    public Page<FuelRecord> getPage(int current, int size, Long vehicleId, Integer isCash, Integer year, String status) {
        return getPage(current, size, vehicleId, null, isCash, year, status);
    }

    public Map<String, Long> getSummary(Long vehicleId, Long driverId, Integer isCash, Integer year) {
        return getSummary(vehicleId, driverId, isCash, year, null);
    }

    public Map<String, Long> getSummary(Long vehicleId, Long driverId, Integer isCash, Integer year, String status) {
        LocalDateTime yearStart = null;
        LocalDateTime yearEnd = null;
        if (year != null) {
            yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
            yearEnd = yearStart.plusYears(1);
        }
        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim() : null;
        Map<String, Object> raw = baseMapper.querySummaryStats(vehicleId, driverId, isCash, yearStart, yearEnd, normalizedStatus);
        Map<String, Long> summary = new HashMap<>();
        summary.put("total",           toLong(raw.get("total")));
        summary.put("noApproval",      toLong(raw.get("noApproval")));
        summary.put("pendingApproval", toLong(raw.get("pendingApproval")));
        summary.put("approved",        toLong(raw.get("approved")));
        summary.put("rejected",        toLong(raw.get("rejected")));
        summary.put("unreimbursed",    toLong(raw.get("unreimbursed")));
        summary.put("reimbursed",      toLong(raw.get("reimbursed")));
        return summary;
    }

    public BigDecimal getTotalFuelCost(Long vehicleId, Integer year) {
        return calculateApprovedFuelCost(vehicleId, year);
    }

    public Map<String, Object> getVehicleStatistics(Long vehicleId, Integer year) {
        Vehicle vehicle = requireVehicle(vehicleId);
        int budgetYear = year == null ? LocalDate.now().getYear() : year;
        BigDecimal budget = safeDecimal(vehicle.getAnnualFuelBudget());
        BigDecimal used = calculateApprovedFuelCost(vehicleId, budgetYear);
        BigDecimal remaining = budget.subtract(used);
        BigDecimal usageRate = budget.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO
                : used.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("vehicleId", vehicleId);
        result.put("year", budgetYear);
        result.put("budget", budget);
        result.put("used", used);
        result.put("remaining", remaining);
        result.put("usageRate", usageRate);
        result.put("fuelReminderStatus", vehicle.getFuelReminderStatus());
        result.put("fuelReminderNote", vehicle.getFuelReminderNote());
        result.put("fuelReminderTime", vehicle.getFuelReminderTime());
        return result;
    }

    public List<Map<String, Object>> getVehicleYearlySummary(Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LocalDateTime start = LocalDate.of(targetYear, 1, 1).atStartOfDay();
        LocalDateTime end = start.plusYears(1);

        List<Vehicle> vehicles = vehicleService.list(new LambdaQueryWrapper<Vehicle>()
                .orderByAsc(Vehicle::getPlateNumber));
        List<FuelRecord> records = this.list(new LambdaQueryWrapper<FuelRecord>()
                .ge(FuelRecord::getFuelDate, start)
                .lt(FuelRecord::getFuelDate, end)
                .and(w -> w.isNull(FuelRecord::getDeleted).or().eq(FuelRecord::getDeleted, 0)));

        Map<Long, BigDecimal> usedByVehicle = new HashMap<>();
        Map<Long, Integer> countByVehicle = new HashMap<>();
        for (FuelRecord record : records) {
            if (record.getVehicleId() == null || !isApprovedForBudget(record)) {
                continue;
            }
            countByVehicle.merge(record.getVehicleId(), 1, Integer::sum);
            usedByVehicle.merge(record.getVehicleId(), safeDecimal(record.getTotalAmount()), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            BigDecimal used = safeDecimal(usedByVehicle.get(vehicle.getId())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal budget = safeDecimal(vehicle.getAnnualFuelBudget());
            BigDecimal remaining = budget.subtract(used);
            BigDecimal usageRate = budget.compareTo(BigDecimal.ZERO) <= 0
                    ? BigDecimal.ZERO
                    : used.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP);

            Map<String, Object> item = new HashMap<>();
            item.put("vehicleId", vehicle.getId());
            item.put("plateNumber", vehicle.getPlateNumber());
            item.put("year", targetYear);
            item.put("usedAmount", used);
            item.put("recordCount", countByVehicle.getOrDefault(vehicle.getId(), 0));
            item.put("budget", budget);
            item.put("remaining", remaining);
            item.put("usageRate", usageRate);
            result.add(item);
        }

        result.sort((left, right) -> {
            BigDecimal rightAmount = (BigDecimal) right.get("usedAmount");
            BigDecimal leftAmount = (BigDecimal) left.get("usedAmount");
            int amountCompare = rightAmount.compareTo(leftAmount);
            if (amountCompare != 0) {
                return amountCompare;
            }
            return String.valueOf(left.get("plateNumber")).compareTo(String.valueOf(right.get("plateNumber")));
        });
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean approveCashReport(Long recordId, Long approverId, boolean approved, String comment) {
        FuelRecord record = this.getById(recordId);
        if (record == null) {
            return false;
        }
        if (!Integer.valueOf(1).equals(record.getIsCash())) {
            throw new IllegalArgumentException("该记录不是现金加油记录");
        }
        if (!"PENDING".equals(record.getCashReportStatus())) {
            throw new IllegalArgumentException("当前记录无需重复审批");
        }

        record.setCashReportApproveBy(approverId);
        record.setCashReportApproveTime(LocalDateTime.now());
        record.setCashReportStatus(approved ? "APPROVED" : "REJECTED");
        if (comment != null && !comment.isBlank()) {
            record.setRemark(comment.trim());
        }
        boolean updated = this.updateById(record);
        if (updated) {
            afterRecordChanged(record, false);
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateReimbursementStatus(Long recordId, boolean reimbursed) {
        FuelRecord record = this.getById(recordId);
        if (record == null) {
            return false;
        }
        if (!canUpdateReimbursementStatus(record)) {
            throw new IllegalArgumentException("当前记录暂不能更新报销状态");
        }

        record.setReimbursementStatus(reimbursed ? "REIMBURSED" : "UNREIMBURSED");
        record.setReimbursedTime(reimbursed ? LocalDateTime.now() : null);
        return this.updateById(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public FuelRecord updateFuelAmount(Long recordId, Object fuelAmountValue) {
        FuelRecord record = this.getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("加油记录不存在");
        }
        if (Integer.valueOf(1).equals(record.getDeleted())) {
            throw new IllegalArgumentException("回收站记录不支持补录加油量");
        }

        record.setFuelAmount(parseRequiredPositiveDecimal(fuelAmountValue, "加油量"));
        if (!this.updateById(record)) {
            throw new IllegalStateException("补录加油量失败");
        }
        enrichRecord(record);
        return record;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean softDelete(Long id) {
        FuelRecord record = this.getById(id);
        if (record == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(record.getDeleted())) {
            return true;
        }
        record.setDeleted(1);
        boolean updated = this.updateById(record);
        if (updated) {
            afterRecordChanged(record, false);
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean restore(Long id) {
        FuelRecord record = this.getById(id);
        if (record == null) {
            return false;
        }
        if (!Integer.valueOf(1).equals(record.getDeleted())) {
            return true;
        }
        record.setDeleted(0);
        boolean updated = this.updateById(record);
        if (updated) {
            afterRecordChanged(record, false);
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean permanentDelete(Long id) {
        FuelRecord record = this.getById(id);
        if (record == null) {
            return false;
        }
        boolean removed = this.removeById(id);
        if (removed) {
            afterRecordChanged(record, true);
            uploadStorageService.deleteCsvPaths(
                    record.getInvoicePhoto(),
                    record.getFuelGaugePhoto(),
                    record.getCashPhoto(),
                    record.getLeaderApprovalPhoto()
            );
        }
        return removed;
    }

    public Page<FuelRecord> getRecycleBin(int current, int size) {
        Page<FuelRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<FuelRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FuelRecord::getDeleted, 1);
        wrapper.orderByDesc(FuelRecord::getUpdateTime);
        Page<FuelRecord> result = this.page(page, wrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean clearRecycleBin() {
        LambdaQueryWrapper<FuelRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FuelRecord::getDeleted, 1);
        List<FuelRecord> list = this.list(wrapper);
        if (list == null || list.isEmpty()) {
            return true;
        }
        boolean removed = this.remove(wrapper);
        if (removed) {
            for (FuelRecord record : list) {
                afterRecordChanged(record, true);
                uploadStorageService.deleteCsvPaths(
                        record.getInvoicePhoto(),
                        record.getFuelGaugePhoto(),
                        record.getCashPhoto(),
                        record.getLeaderApprovalPhoto()
                );
            }
        }
        return removed;
    }

    private void normalizeRecord(FuelRecord record) {
        if (record.getVehicleId() == null) {
            throw new IllegalArgumentException("请选择车辆");
        }
        requireVehicle(record.getVehicleId());
        if (record.getDriverId() == null) {
            throw new IllegalArgumentException("驾驶员信息不能为空");
        }
        requireDriver(record.getDriverId());
        record.setFuelType(normalizeRequiredText(record.getFuelType(), "请选择油品类型"));
        record.setFuelLocation(normalizeNullableText(record.getFuelLocation()));
        record.setRemark(normalizeNullableText(record.getRemark()));
        if (record.getFuelAmount() != null) {
            if (record.getFuelAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("加油量必须大于0");
            }
            record.setFuelAmount(record.getFuelAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (record.getFuelPrice() != null) {
            if (record.getFuelPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("油价必须大于0");
            }
            record.setFuelPrice(record.getFuelPrice().setScale(2, RoundingMode.HALF_UP));
        }
        if (record.getFuelMileage() != null) {
            if (record.getFuelMileage().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("加油时公里数必须大于0");
            }
            record.setFuelMileage(record.getFuelMileage().setScale(2, RoundingMode.HALF_UP));
        }
        if (record.getFuelDate() == null) {
            throw new IllegalArgumentException("加油时间不能为空");
        }
        BigDecimal normalizedTotalAmount = record.getTotalAmount();
        if ((normalizedTotalAmount == null || normalizedTotalAmount.compareTo(BigDecimal.ZERO) <= 0)
                && record.getFuelAmount() != null
                && record.getFuelPrice() != null) {
            normalizedTotalAmount = record.getFuelAmount().multiply(record.getFuelPrice());
        }
        if (normalizedTotalAmount == null || normalizedTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("加油金额必须大于0");
        }
        record.setTotalAmount(normalizedTotalAmount.setScale(2, RoundingMode.HALF_UP));
        Integer normalizedIsCash = record.getIsCash() == null ? 1 : record.getIsCash();
        validateBinaryValue(normalizedIsCash, "是否现金加油");
        record.setIsCash(normalizedIsCash);
        record.setIsFuelEnoughAfterFuel(record.getIsFuelEnoughAfterFuel() == null ? 1 : record.getIsFuelEnoughAfterFuel());
        validateBinaryValue(record.getIsFuelEnoughAfterFuel(), "加油后是否不少于半箱");
        record.setBudgetYear(record.getFuelDate().getYear());
        record.setDeleted(record.getDeleted() == null ? 0 : record.getDeleted());
        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }

        record.setCashReason(normalizeNullableText(record.getCashReason()));
        record.setCashPhoto(normalizeNullableText(record.getCashPhoto()));
        record.setLeaderApprovalPhoto(normalizeNullableText(record.getLeaderApprovalPhoto()));
        record.setCashReportStatus("NONE");
        record.setCashReportApproveBy(null);
        record.setCashReportApproveTime(null);
        record.setReimbursementStatus(normalizeReimbursementStatus(record.getReimbursementStatus()));
        if (record.getReimbursementStatus() == null) {
            record.setReimbursementStatus("UNREIMBURSED");
        }
        if (!"REIMBURSED".equals(record.getReimbursementStatus())) {
            record.setReimbursedTime(null);
        }

        if (record.getInvoicePhoto() == null || record.getInvoicePhoto().isBlank()) {
            throw new IllegalArgumentException("请上传加油凭证照片");
        }
    }

    private void afterRecordChanged(FuelRecord record, boolean removed) {
        if (record.getVehicleId() == null) {
            return;
        }
        if (record.getBudgetYear() != null && record.getBudgetYear().equals(LocalDate.now().getYear())) {
            refreshVehicleAnnualFuelUsedSnapshot(record.getVehicleId());
        }
        if (removed) {
            return;
        }

        Vehicle vehicle = vehicleService.getById(record.getVehicleId());
        if (shouldResolveFuelReminder(record, vehicle)) {
            clearVehicleFuelReminder(vehicle.getId(), vehicle.getFuelReminderTime());
        }
    }

    private boolean shouldResolveFuelReminder(FuelRecord record, Vehicle vehicle) {
        return vehicle != null
                && "PENDING".equals(vehicle.getFuelReminderStatus())
                && Integer.valueOf(0).equals(record.getDeleted())
                && isApprovedForBudget(record)
                && Integer.valueOf(1).equals(record.getIsFuelEnoughAfterFuel())
                && canResolveCurrentFuelReminder(record, vehicle);
    }

    private boolean canResolveCurrentFuelReminder(FuelRecord record, Vehicle vehicle) {
        LocalDateTime reminderTime = vehicle.getFuelReminderTime();
        if (reminderTime == null || record.getFuelDate() == null) {
            return true;
        }
        return !record.getFuelDate().isBefore(reminderTime);
    }

    private boolean isApprovedForBudget(FuelRecord record) {
        if (Integer.valueOf(1).equals(record.getDeleted())) {
            return false;
        }
        if (!Integer.valueOf(1).equals(record.getIsCash())) {
            return true;
        }
        String reportStatus = record.getCashReportStatus();
        return reportStatus == null
                || reportStatus.isBlank()
                || "NONE".equals(reportStatus)
                || "APPROVED".equals(reportStatus);
    }

    private boolean canUpdateReimbursementStatus(FuelRecord record) {
        if (Integer.valueOf(1).equals(record.getDeleted())) {
            return false;
        }
        if (!Integer.valueOf(1).equals(record.getIsCash())) {
            return true;
        }
        String reportStatus = record.getCashReportStatus();
        return "APPROVED".equals(reportStatus) || "NONE".equals(reportStatus) || reportStatus == null || reportStatus.isBlank();
    }

    private void refreshVehicleAnnualFuelUsedSnapshot(Long vehicleId) {
        BigDecimal currentYearUsed = calculateApprovedFuelCost(vehicleId, LocalDate.now().getYear());
        vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, vehicleId)
                .set(Vehicle::getAnnualFuelUsed, currentYearUsed)
                .update();
    }

    private void clearVehicleFuelReminder(Long vehicleId, LocalDateTime reminderTime) {
        var update = vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, vehicleId)
                .eq(Vehicle::getFuelReminderStatus, "PENDING");
        if (reminderTime != null) {
            update.eq(Vehicle::getFuelReminderTime, reminderTime);
        }
        update.set(Vehicle::getFuelReminderStatus, "COMPLETED")
                .set(Vehicle::getFuelReminderNote, "已完成补油")
                .set(Vehicle::getFuelReminderTime, LocalDateTime.now())
                .update();
    }

    private BigDecimal calculateApprovedFuelCost(Long vehicleId, Integer year) {
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        LocalDateTime start = LocalDate.of(targetYear, 1, 1).atStartOfDay();
        LocalDateTime end = start.plusYears(1);
        List<FuelRecord> records = this.list(new LambdaQueryWrapper<FuelRecord>()
                .eq(FuelRecord::getVehicleId, vehicleId)
                .ge(FuelRecord::getFuelDate, start)
                .lt(FuelRecord::getFuelDate, end)
                .and(w -> w.isNull(FuelRecord::getDeleted).or().eq(FuelRecord::getDeleted, 0)));

        BigDecimal total = BigDecimal.ZERO;
        for (FuelRecord record : records) {
            if (isApprovedForBudget(record)) {
                total = total.add(safeDecimal(record.getTotalAmount()));
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Vehicle requireVehicle(Long vehicleId) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("请选择车辆");
        }
        Vehicle vehicle = vehicleService.getById(vehicleId);
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        return vehicle;
    }

    private Vehicle requireVehicleForUpdate(Long vehicleId) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("请选择车辆");
        }
        Vehicle vehicle = vehicleService.getByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        return vehicle;
    }

    private Driver requireDriver(Long driverId) {
        if (driverId == null) {
            throw new IllegalArgumentException("驾驶员信息不能为空");
        }
        Driver driver = driverService.getById(driverId);
        if (driver == null || !"ACTIVE".equals(driver.getStatus())) {
            throw new IllegalArgumentException("驾驶员账号不可用");
        }
        return driver;
    }

    private void validateBinaryValue(Integer value, String fieldLabel) {
        if (value == null || (!Integer.valueOf(0).equals(value) && !Integer.valueOf(1).equals(value))) {
            throw new IllegalArgumentException(fieldLabel + "只能选择是或否");
        }
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

    private BigDecimal parseDecimal(String value, String fieldLabel) {
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(fieldLabel + "必须大于0");
            }
            return decimal.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确");
        }
    }

    private BigDecimal parseRequiredPositiveDecimal(Object value, String fieldLabel) {
        if (value == null) {
            throw new IllegalArgumentException(fieldLabel + "不能为空");
        }
        String normalized = String.valueOf(value).trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + "不能为空");
        }
        return parseDecimal(normalized, fieldLabel);
    }

    private BigDecimal parseOptionalDecimal(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确");
        }
    }

    private LocalDateTime parseDateTime(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + "不能为空");
        }
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (Exception exception) {
            throw new IllegalArgumentException(fieldLabel + "格式不正确");
        }
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LambdaQueryWrapper<FuelRecord> buildBaseQueryWrapper(Long vehicleId, Long driverId, Integer isCash, Integer year) {
        LambdaQueryWrapper<FuelRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(FuelRecord::getDeleted).or().eq(FuelRecord::getDeleted, 0));
        if (vehicleId != null) {
            wrapper.eq(FuelRecord::getVehicleId, vehicleId);
        }
        if (driverId != null) {
            wrapper.eq(FuelRecord::getDriverId, driverId);
        }
        if (isCash != null) {
            wrapper.eq(FuelRecord::getIsCash, isCash);
        }
        if (year != null) {
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime end = start.plusYears(1);
            wrapper.ge(FuelRecord::getFuelDate, start).lt(FuelRecord::getFuelDate, end);
        }
        return wrapper;
    }

    private LambdaQueryWrapper<FuelRecord> applyStatusFilter(LambdaQueryWrapper<FuelRecord> wrapper, String status) {
        if (status != null && !status.isBlank()) {
            wrapper.eq(FuelRecord::getCashReportStatus, status.trim());
        }
        return wrapper;
    }

    private void enrichRecords(List<FuelRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> vehicleIds = records.stream().map(FuelRecord::getVehicleId).filter(id -> id != null).distinct().toList();
        List<Long> driverIds = records.stream().map(FuelRecord::getDriverId).filter(id -> id != null).distinct().toList();

        Map<Long, Vehicle> vehicleMap = new HashMap<>();
        if (!vehicleIds.isEmpty()) {
            List<Vehicle> vehicles = vehicleService.list(new LambdaQueryWrapper<Vehicle>().in(Vehicle::getId, vehicleIds));
            for (Vehicle vehicle : vehicles) {
                vehicleMap.put(vehicle.getId(), vehicle);
            }
        }

        Map<Long, Driver> driverMap = new HashMap<>();
        if (!driverIds.isEmpty()) {
            List<Driver> drivers = driverService.list(new LambdaQueryWrapper<Driver>().in(Driver::getId, driverIds));
            for (Driver driver : drivers) {
                driverMap.put(driver.getId(), driver);
            }
        }

        for (FuelRecord record : records) {
            applyDefaultReimbursementState(record);
            Vehicle vehicle = vehicleMap.get(record.getVehicleId());
            if (vehicle != null) {
                record.setPlateNumber(vehicle.getPlateNumber());
                record.setVehicleModel(vehicle.getModel());
            }
            Driver driver = driverMap.get(record.getDriverId());
            if (driver != null) {
                record.setDriverName(driver.getDriverName());
            }
        }
    }

    private void enrichRecord(FuelRecord record) {
        if (record == null) {
            return;
        }
        enrichRecords(List.of(record));
    }

    private void applyDefaultReimbursementState(FuelRecord record) {
        String normalizedStatus = normalizeReimbursementStatus(record.getReimbursementStatus());
        if (normalizedStatus == null) {
            normalizedStatus = record.getReimbursedTime() != null ? "REIMBURSED" : "UNREIMBURSED";
        }
        record.setReimbursementStatus(normalizedStatus);
        if (!"REIMBURSED".equals(normalizedStatus)) {
            record.setReimbursedTime(null);
        }
    }

    private String normalizeReimbursementStatus(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if ("REIMBURSED".equals(normalized) || "UNREIMBURSED".equals(normalized)) {
            return normalized;
        }
        return null;
    }
    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

}
