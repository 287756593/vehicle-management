package com.company.vehicle.service;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.VehicleBorrowRecordEditLog;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.SysUser;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.entity.VehicleBorrowRecord;
import com.company.vehicle.mapper.VehicleBorrowRecordEditLogMapper;
import com.company.vehicle.mapper.VehicleBorrowRecordMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class VehicleBorrowRecordService extends ServiceImpl<VehicleBorrowRecordMapper, VehicleBorrowRecord> {

    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final SysUserService sysUserService;
    private final UploadStorageService uploadStorageService;
    private final JdbcTemplate jdbcTemplate;
    private final VehicleBorrowRecordEditLogMapper editLogMapper;
    private final ObjectMapper objectMapper;

    public VehicleBorrowRecordService(
            VehicleService vehicleService,
            DriverService driverService,
            SysUserService sysUserService,
            UploadStorageService uploadStorageService,
            JdbcTemplate jdbcTemplate,
            VehicleBorrowRecordEditLogMapper editLogMapper,
            ObjectMapper objectMapper) {
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        this.sysUserService = sysUserService;
        this.uploadStorageService = uploadStorageService;
        this.jdbcTemplate = jdbcTemplate;
        this.editLogMapper = editLogMapper;
        this.objectMapper = objectMapper;
    }

    public Page<VehicleBorrowRecord> getPage(
            int current,
            int size,
            String plateNumber,
            String driverName,
            String status,
            String followUpStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long driverId) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        Page<VehicleBorrowRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = buildQueryWrapper(
                plateNumber,
                driverName,
                status,
                followUpStatus,
                startDate,
                endDate,
                driverId);
        wrapper.orderByDesc(VehicleBorrowRecord::getTakeTime);
        Page<VehicleBorrowRecord> result = this.page(page, wrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    public Map<String, Long> getSummary(
            String plateNumber,
            String driverName,
            String status,
            String followUpStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long driverId) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }

        Map<String, Long> summary = new HashMap<>();
        summary.put("total", this.count(buildQueryWrapper(
                plateNumber,
                driverName,
                status,
                followUpStatus,
                startDate,
                endDate,
                driverId)));
        summary.put("taken", this.count(buildQueryWrapper(
                plateNumber,
                driverName,
                "TAKEN",
                followUpStatus,
                startDate,
                endDate,
                driverId)));
        summary.put("returned", this.count(buildQueryWrapper(
                plateNumber,
                driverName,
                "RETURNED",
                followUpStatus,
                startDate,
                endDate,
                driverId)));
        summary.put("pendingFollowUp", this.count(buildQueryWrapper(
                plateNumber,
                driverName,
                status,
                "PENDING",
                startDate,
                endDate,
                driverId)));
        summary.put("actionRequired", this.count(buildQueryWrapper(
                plateNumber,
                driverName,
                status,
                followUpStatus,
                startDate,
                endDate,
                driverId).isNotNull(VehicleBorrowRecord::getActionRequired)
                        .ne(VehicleBorrowRecord::getActionRequired, "")));
        return summary;
    }

    private LambdaQueryWrapper<VehicleBorrowRecord> buildQueryWrapper(
            String plateNumber,
            String driverName,
            String status,
            String followUpStatus,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long driverId) {
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0));
        if (plateNumber != null && !plateNumber.isBlank()) {
            wrapper.like(VehicleBorrowRecord::getPlateNumber, plateNumber.trim());
        }
        if (driverName != null && !driverName.isBlank()) {
            wrapper.like(VehicleBorrowRecord::getDriverName, driverName.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(VehicleBorrowRecord::getStatus, status.trim());
        }
        if (followUpStatus != null && !followUpStatus.isBlank()) {
            wrapper.eq(VehicleBorrowRecord::getFollowUpStatus, followUpStatus.trim());
        }
        if (startDate != null) {
            wrapper.ge(VehicleBorrowRecord::getTakeTime, startDate);
        }
        if (endDate != null) {
            wrapper.le(VehicleBorrowRecord::getTakeTime, endDate);
        }
        if (driverId != null) {
            wrapper.eq(VehicleBorrowRecord::getDriverId, driverId);
        }
        return wrapper;
    }

    public Page<VehicleBorrowRecord> getDriverRecordPage(Long driverId, int current, int size) {
        Page<VehicleBorrowRecord> page = new Page<>(Math.max(current, 1), Math.max(size, 1));
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleBorrowRecord::getDriverId, driverId)
                .and(w -> w.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .orderByDesc(VehicleBorrowRecord::getTakeTime);
        Page<VehicleBorrowRecord> result = this.page(page, wrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    public VehicleBorrowRecord getCurrentRecord(Long driverId) {
        VehicleBorrowRecord record = findActiveRecord(driverId);
        if (record != null) {
            enrichRecord(record);
        }
        return record;
    }

    public VehicleBorrowRecord requireFuelRecordBorrowContext(Long driverId, Long vehicleId, LocalDateTime fuelDate) {
        if (driverId == null) {
            throw new IllegalArgumentException("驾驶员信息不能为空");
        }
        if (vehicleId == null) {
            throw new IllegalArgumentException("请选择车辆");
        }
        if (fuelDate == null) {
            throw new IllegalArgumentException("加油时间不能为空");
        }

        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleBorrowRecord::getDriverId, driverId)
                .eq(VehicleBorrowRecord::getVehicleId, vehicleId)
                .le(VehicleBorrowRecord::getTakeTime, fuelDate)
                .and(condition -> condition.eq(VehicleBorrowRecord::getStatus, "TAKEN")
                        .or(returned -> returned.eq(VehicleBorrowRecord::getStatus, "RETURNED")
                                .isNotNull(VehicleBorrowRecord::getReturnTime)
                                .ge(VehicleBorrowRecord::getReturnTime, fuelDate)))
                .orderByDesc(VehicleBorrowRecord::getTakeTime);

        VehicleBorrowRecord record = this.list(wrapper).stream().findFirst().orElse(null);
        if (record == null) {
            throw new IllegalArgumentException("补登记请填写本次借车期间的实际加油时间");
        }
        enrichRecord(record);
        return record;
    }

    public List<VehicleBorrowRecordEditLog> getEditLogs(Long recordId) {
        if (recordId == null || this.lambdaQuery().eq(VehicleBorrowRecord::getId, recordId).count() == 0) {
            throw new IllegalArgumentException("借还车记录不存在");
        }
        return editLogMapper.selectList(new LambdaQueryWrapper<VehicleBorrowRecordEditLog>()
                .eq(VehicleBorrowRecordEditLog::getRecordId, recordId)
                .orderByDesc(VehicleBorrowRecordEditLog::getId));
    }

    @Transactional
    public VehicleBorrowRecord takeVehicle(
            Long vehicleId,
            Long driverId,
            BigDecimal takeMileage,
            String usageReason,
            String destination,
            LocalDateTime expectedReturnTime,
            MultipartFile[] takeVehicleFiles,
            MultipartFile[] takeMileageFiles) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("请选择车辆");
        }
        if (takeMileage == null) {
            throw new IllegalArgumentException("请输入取车里程");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("请填写目的地/去向");
        }
        if (expectedReturnTime != null && !expectedReturnTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("预计还车时间必须晚于当前时间");
        }

        return withBorrowLocks(driverId, vehicleId, () -> {
            Driver driver = driverService.getById(driverId);
            if (driver == null || !"ACTIVE".equals(driver.getStatus())) {
                throw new IllegalArgumentException("驾驶员不存在或已停用");
            }
            if (findActiveRecord(driverId) != null) {
                throw new IllegalArgumentException("你当前还有未还车记录");
            }

            Vehicle vehicle = vehicleService.getById(vehicleId);
            if (vehicle == null) {
                throw new IllegalArgumentException("车辆不存在");
            }
            if (!"NORMAL".equals(vehicle.getStatus())) {
                throw new IllegalArgumentException("该车辆当前不可借用");
            }
            vehicleService.validateCanBorrowToday(vehicle);
            BigDecimal currentMileage = vehicle.getCurrentMileage() == null ? BigDecimal.ZERO : vehicle.getCurrentMileage();
            if (takeMileage.compareTo(currentMileage) < 0) {
                throw new IllegalArgumentException("取车里程不能小于车辆当前里程");
            }

            String takeVehiclePhotos = uploadStorageService.saveImageFiles(takeVehicleFiles, "出发前车辆照片");
            String takeMileagePhoto = uploadStorageService.saveImageFiles(takeMileageFiles, "出发前公里数照片");
            if (takeVehiclePhotos.isBlank()) {
                throw new IllegalArgumentException("请上传出发前车辆照片");
            }
            if (takeMileagePhoto.isBlank()) {
                throw new IllegalArgumentException("请上传出发前公里数照片");
            }

            LocalDateTime now = LocalDateTime.now();
            VehicleBorrowRecord record = new VehicleBorrowRecord();
            record.setRecordNo("BR" + IdUtil.getSnowflakeNextIdStr());
            record.setVehicleId(vehicle.getId());
            record.setPlateNumber(vehicle.getPlateNumber());
            record.setDriverId(driver.getId());
            record.setDriverName(driver.getDriverName());
            record.setStatus("TAKEN");
            record.setUsageReason(usageReason == null || usageReason.isBlank() ? null : usageReason.trim());
            record.setDestination(destination.trim());
            record.setExpectedReturnTime(expectedReturnTime);
            record.setTakeMileage(takeMileage);
            record.setTakeVehiclePhotos(takeVehiclePhotos);
            record.setTakeMileagePhoto(takeMileagePhoto);
            record.setTakeTime(now);
            record.setFollowUpStatus("NONE");
            record.setDeleted(0);

            boolean claimed = vehicleService.lambdaUpdate()
                    .eq(Vehicle::getId, vehicle.getId())
                    .eq(Vehicle::getStatus, "NORMAL")
                    .set(Vehicle::getStatus, "IN_USE")
                    .set(Vehicle::getCurrentDriverId, driver.getId())
                    .set(Vehicle::getCurrentDriverName, driver.getDriverName())
                    .set(Vehicle::getCurrentDestination, record.getDestination())
                    .set(Vehicle::getBorrowTime, now)
                    .set(Vehicle::getBorrowMileage, takeMileage)
                    .set(Vehicle::getPickupPhotos, takeVehiclePhotos)
                    .set(Vehicle::getPickupFuelPhoto, takeMileagePhoto)
                    .set(Vehicle::getReturnTime, null)
                    .set(Vehicle::getReturnPhotos, null)
                    .set(Vehicle::getReturnFuelPhoto, null)
                    .set(Vehicle::getReturnMileagePhoto, null)
                    .set(Vehicle::getIsClean, null)
                    .set(Vehicle::getCleanReason, null)
                    .update();
            if (!claimed) {
                throw new IllegalArgumentException("该车辆刚刚已被其他人借走，请刷新后重试");
            }
            if (!this.save(record)) {
                throw new IllegalStateException("保存借车记录失败");
            }

            enrichRecord(record);
            return record;
        });
    }

    @Transactional
    public VehicleBorrowRecord returnVehicle(
            Long recordId,
            Long driverId,
            BigDecimal returnMileage,
            Integer isClean,
            Integer isFuelEnough,
            String issueDescription,
            MultipartFile[] returnVehicleFiles,
            MultipartFile[] returnMileageFiles,
            MultipartFile[] returnFuelFiles,
            MultipartFile[] issuePhotoFiles) {
        if (recordId == null) {
            throw new IllegalArgumentException("未找到当前借车记录");
        }
        if (returnMileage == null) {
            throw new IllegalArgumentException("请输入还车累计里程");
        }
        if (isClean == null) {
            throw new IllegalArgumentException("请选择车辆是否干净");
        }
        if (isFuelEnough == null) {
            throw new IllegalArgumentException("请选择油量是否不少于半箱");
        }

        VehicleBorrowRecord existingRecord = this.getById(recordId);
        if (existingRecord == null) {
            throw new IllegalArgumentException("借车记录不存在");
        }
        if (!driverId.equals(existingRecord.getDriverId())) {
            throw new AccessDeniedException("无权归还该车辆");
        }

        return withBorrowLocks(driverId, existingRecord.getVehicleId(), () -> {
            VehicleBorrowRecord record = this.getById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("借车记录不存在");
            }
            if (!driverId.equals(record.getDriverId())) {
                throw new AccessDeniedException("无权归还该车辆");
            }
            if (!"TAKEN".equals(record.getStatus())) {
                throw new IllegalArgumentException("该借车记录已还车");
            }
            if (record.getTakeMileage() != null && returnMileage.compareTo(record.getTakeMileage()) < 0) {
                throw new IllegalArgumentException("还车累计里程不能小于取车里程");
            }

            Vehicle vehicle = vehicleService.getById(record.getVehicleId());
            if (vehicle == null) {
                throw new IllegalArgumentException("车辆不存在");
            }
            if (vehicle.getCurrentDriverId() != null && !driverId.equals(vehicle.getCurrentDriverId())) {
                throw new AccessDeniedException("当前车辆借用人不匹配");
            }

            String normalizedIssueDescription = issueDescription == null ? "" : issueDescription.trim();
            String returnVehiclePhotos = uploadStorageService.saveImageFiles(returnVehicleFiles, "停车后的车辆照片");
            String returnMileagePhoto = uploadStorageService.saveImageFiles(returnMileageFiles, "还车仪表照片");
            String returnFuelPhoto = uploadStorageService.saveImageFiles(returnFuelFiles, "还车油表照片");
            String issuePhotos = uploadStorageService.saveImageFiles(issuePhotoFiles, "车辆异常照片");
            if (returnVehiclePhotos.isBlank()) {
                throw new IllegalArgumentException("请上传停车后的车辆照片");
            }
            if (returnMileagePhoto.isBlank()) {
                throw new IllegalArgumentException("请上传还车仪表照片");
            }
            returnFuelPhoto = resolveReturnFuelPhoto(returnMileagePhoto, returnFuelPhoto);
            if (!issuePhotos.isBlank() && normalizedIssueDescription.isBlank()) {
                throw new IllegalArgumentException("请填写车辆异常说明");
            }
            if (!normalizedIssueDescription.isBlank() && issuePhotos.isBlank()) {
                throw new IllegalArgumentException("请上传车辆异常照片");
            }

            boolean hasVehicleIssue = !normalizedIssueDescription.isBlank();
            String actionRequired = buildActionRequired(isClean, isFuelEnough, hasVehicleIssue);
            String followUpStatus = actionRequired.isBlank() ? "NONE" : "PENDING";
            String nextVehicleStatus = hasVehicleIssue ? "PENDING_CHECK" : "NORMAL";
            LocalDateTime now = LocalDateTime.now();

            record.setStatus("RETURNED");
            record.setReturnMileage(returnMileage);
            record.setReturnVehiclePhotos(returnVehiclePhotos);
            record.setReturnMileagePhoto(returnMileagePhoto);
            record.setReturnFuelPhoto(returnFuelPhoto);
            record.setIsClean(isClean);
            record.setIsFuelEnough(isFuelEnough);
            record.setIssueDescription(normalizedIssueDescription);
            record.setIssuePhotos(issuePhotos);
            record.setActionRequired(actionRequired);
            record.setFollowUpStatus(followUpStatus);
            record.setReturnTime(now);
            if (!this.updateById(record)) {
                throw new IllegalStateException("更新还车记录失败");
            }

            boolean vehicleUpdated = vehicleService.lambdaUpdate()
                    .eq(Vehicle::getId, vehicle.getId())
                    .set(Vehicle::getStatus, nextVehicleStatus)
                    .set(Vehicle::getCurrentDriverId, null)
                    .set(Vehicle::getCurrentDriverName, null)
                    .set(Vehicle::getCurrentDestination, null)
                    .set(Vehicle::getReturnTime, now)
                    .set(Vehicle::getReturnPhotos, returnVehiclePhotos)
                    .set(Vehicle::getReturnFuelPhoto, returnFuelPhoto)
                    .set(Vehicle::getReturnMileagePhoto, returnMileagePhoto)
                    .set(Vehicle::getIsClean, isClean)
                    .set(Vehicle::getCleanReason, "PENDING_CHECK".equals(nextVehicleStatus) ? actionRequired : null)
                    .set(Vehicle::getFuelReminderStatus, buildFuelReminderStatus(isFuelEnough))
                    .set(Vehicle::getFuelReminderNote, buildFuelReminderNote(isFuelEnough))
                    .set(Vehicle::getFuelReminderTime, buildFuelReminderTime(isFuelEnough, now))
                    .set(Vehicle::getCurrentMileage, returnMileage)
                    .update();
            if (!vehicleUpdated) {
                throw new IllegalStateException("更新车辆状态失败");
            }

            enrichRecord(record);
            return record;
        });
    }

    @Transactional
    public VehicleBorrowRecord completeFollowUp(
            Long recordId,
            Long handlerId,
            String remark,
            String nextVehicleStatus) {
        VehicleBorrowRecord existingRecord = this.getById(recordId);
        if (existingRecord == null) {
            throw new IllegalArgumentException("借还车记录不存在");
        }

        return withBorrowLocks(existingRecord.getDriverId(), existingRecord.getVehicleId(), () -> {
            VehicleBorrowRecord record = this.getById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("借还车记录不存在");
            }
            if (!"PENDING".equals(record.getFollowUpStatus())) {
                throw new IllegalArgumentException("当前记录无需处理闭环");
            }
            String targetStatus = (nextVehicleStatus == null || nextVehicleStatus.isBlank())
                    ? "NORMAL"
                    : nextVehicleStatus.trim();
            if (!List.of("NORMAL", "MAINTENANCE").contains(targetStatus)) {
                throw new IllegalArgumentException("处理后车辆状态不合法");
            }

            Vehicle vehicle = vehicleService.getById(record.getVehicleId());
            if (vehicle == null) {
                throw new IllegalArgumentException("车辆不存在");
            }
            String normalizedRemark = remark == null ? "" : remark.trim();
            boolean latestRecordForVehicle = isLatestRecordForVehicle(record.getVehicleId(), record.getId());
            if (!latestRecordForVehicle) {
                LocalDateTime now = LocalDateTime.now();
                record.setFollowUpStatus("COMPLETED");
                record.setFollowUpRemark(appendRemarkNote(normalizedRemark, "历史待办已处理，未改动车辆当前状态"));
                record.setFollowUpHandledBy(handlerId);
                record.setFollowUpHandledTime(now);
                record.setFollowUpResultStatus(targetStatus);
                if (!this.updateById(record)) {
                    throw new IllegalStateException("更新闭环记录失败");
                }
                enrichRecord(record);
                return record;
            }
            if ("IN_USE".equals(vehicle.getStatus())) {
                if (!"NORMAL".equals(targetStatus)) {
                    throw new IllegalArgumentException("车辆当前正在使用中，不能变更为非正常状态");
                }
                LocalDateTime now = LocalDateTime.now();
                record.setFollowUpStatus("COMPLETED");
                record.setFollowUpRemark(normalizedRemark);
                record.setFollowUpHandledBy(handlerId);
                record.setFollowUpHandledTime(now);
                record.setFollowUpResultStatus(targetStatus);
                if (!this.updateById(record)) {
                    throw new IllegalStateException("更新闭环记录失败");
                }
                enrichRecord(record);
                return record;
            }

            LocalDateTime now = LocalDateTime.now();
            record.setFollowUpStatus("COMPLETED");
            record.setFollowUpRemark(normalizedRemark);
            record.setFollowUpHandledBy(handlerId);
            record.setFollowUpHandledTime(now);
            record.setFollowUpResultStatus(targetStatus);
            if (!this.updateById(record)) {
                throw new IllegalStateException("更新闭环记录失败");
            }
            String nextCleanReason = "NORMAL".equals(targetStatus)
                    ? null
                    : (record.getIssueDescription() != null && !record.getIssueDescription().isBlank()
                    ? record.getIssueDescription()
                    : record.getActionRequired());
            boolean vehicleUpdated = vehicleService.lambdaUpdate()
                    .eq(Vehicle::getId, vehicle.getId())
                    .set(Vehicle::getStatus, targetStatus)
                    .set(Vehicle::getCurrentDriverId, null)
                    .set(Vehicle::getCurrentDriverName, null)
                    .set(Vehicle::getCurrentDestination, null)
                    .set(Vehicle::getIsClean, "NORMAL".equals(targetStatus) ? 1 : vehicle.getIsClean())
                    .set(Vehicle::getCleanReason, nextCleanReason)
                    .update();
            if (!vehicleUpdated) {
                throw new IllegalStateException("更新车辆闭环状态失败");
            }

            enrichRecord(record);
            return record;
        });
    }

    @Transactional
    public VehicleBorrowRecord updateRecordByAdmin(
            Long recordId,
            Long operatorId,
            String operatorName,
            String usageReason,
            String destination,
            LocalDateTime takeTime,
            LocalDateTime expectedReturnTime,
            BigDecimal takeMileage,
            LocalDateTime returnTime,
            BigDecimal returnMileage,
            Integer isClean,
            Integer isFuelEnough,
            String issueDescription,
            MultipartFile[] takeVehicleFiles,
            MultipartFile[] takeMileageFiles,
            MultipartFile[] returnVehicleFiles,
            MultipartFile[] returnMileageFiles,
            MultipartFile[] returnFuelFiles,
            MultipartFile[] issuePhotoFiles) {
        VehicleBorrowRecord existingRecord = this.getById(recordId);
        if (existingRecord == null) {
            throw new IllegalArgumentException("借还车记录不存在");
        }

        return withBorrowLocks(existingRecord.getDriverId(), existingRecord.getVehicleId(), () -> {
            VehicleBorrowRecord record = this.getById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("借还车记录不存在");
            }
            VehicleBorrowRecord beforeSnapshot = cloneRecord(record);
            List<String> oldPhotosToDelete = new ArrayList<>();

            String normalizedUsageReason = normalizeNullableText(
                    usageReason != null ? usageReason : record.getUsageReason()
            );
            String normalizedDestination = normalizeRequiredText(
                    destination != null ? destination : record.getDestination(),
                    "请填写目的地/去向"
            );
            LocalDateTime effectiveTakeTime = takeTime != null ? takeTime : record.getTakeTime();
            if (effectiveTakeTime == null) {
                throw new IllegalArgumentException("取车时间不能为空");
            }
            BigDecimal effectiveTakeMileage = takeMileage != null ? takeMileage : record.getTakeMileage();
            if (effectiveTakeMileage == null) {
                throw new IllegalArgumentException("取车里程不能为空");
            }
            if (effectiveTakeMileage.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("取车里程不能小于0");
            }
            LocalDateTime effectiveExpectedReturnTime = expectedReturnTime != null
                    ? expectedReturnTime
                    : record.getExpectedReturnTime();
            if (effectiveExpectedReturnTime != null && !effectiveExpectedReturnTime.isAfter(effectiveTakeTime)) {
                throw new IllegalArgumentException("预计还车时间必须晚于取车时间");
            }

            record.setUsageReason(normalizedUsageReason);
            record.setDestination(normalizedDestination);
            record.setTakeTime(effectiveTakeTime);
            record.setExpectedReturnTime(effectiveExpectedReturnTime);
            record.setTakeMileage(effectiveTakeMileage);
            record.setTakeVehiclePhotos(replacePhotoField(
                    record.getTakeVehiclePhotos(),
                    takeVehicleFiles,
                    "取车车辆照片",
                    oldPhotosToDelete
            ));
            record.setTakeMileagePhoto(replacePhotoField(
                    record.getTakeMileagePhoto(),
                    takeMileageFiles,
                    "取车公里数照片",
                    oldPhotosToDelete
            ));

            boolean latestRecordForVehicle = isLatestRecordForVehicle(record.getVehicleId(), record.getId());
            if ("RETURNED".equals(record.getStatus())) {
                BigDecimal effectiveReturnMileage = returnMileage != null ? returnMileage : record.getReturnMileage();
                LocalDateTime effectiveReturnTime = returnTime != null ? returnTime : record.getReturnTime();
                Integer effectiveIsClean = isClean != null ? isClean : record.getIsClean();
                Integer effectiveIsFuelEnough = isFuelEnough != null ? isFuelEnough : record.getIsFuelEnough();
                String normalizedIssueDescription = normalizeNullableText(
                        issueDescription != null ? issueDescription : record.getIssueDescription()
                );

                if (effectiveReturnTime == null) {
                    throw new IllegalArgumentException("还车时间不能为空");
                }
                if (effectiveReturnTime.isBefore(effectiveTakeTime)) {
                    throw new IllegalArgumentException("还车时间不能早于取车时间");
                }
                if (effectiveReturnMileage == null) {
                    throw new IllegalArgumentException("还车累计里程不能为空");
                }
                if (effectiveReturnMileage.compareTo(effectiveTakeMileage) < 0) {
                    throw new IllegalArgumentException("还车累计里程不能小于取车里程");
                }
                validateBinaryValue(effectiveIsClean, "车辆是否干净");
                validateBinaryValue(effectiveIsFuelEnough, "油量是否不少于半箱");

                String effectiveReturnVehiclePhotos = replacePhotoField(
                        record.getReturnVehiclePhotos(),
                        returnVehicleFiles,
                        "还车停车照片",
                        oldPhotosToDelete
                );
                String effectiveReturnMileagePhoto = replacePhotoField(
                        record.getReturnMileagePhoto(),
                        returnMileageFiles,
                        "还车公里数照片",
                        oldPhotosToDelete
                );
                String effectiveReturnFuelPhoto = replacePhotoField(
                        record.getReturnFuelPhoto(),
                        returnFuelFiles,
                        "还车油表照片",
                        oldPhotosToDelete
                );
                String effectiveIssuePhotos = replacePhotoField(
                        record.getIssuePhotos(),
                        issuePhotoFiles,
                        "车辆异常照片",
                        oldPhotosToDelete
                );
                if (normalizedIssueDescription == null) {
                    if (effectiveIssuePhotos != null && !effectiveIssuePhotos.isBlank()) {
                        oldPhotosToDelete.add(effectiveIssuePhotos);
                    }
                    effectiveIssuePhotos = null;
                }

                boolean hasVehicleIssue = normalizedIssueDescription != null && !normalizedIssueDescription.isBlank();
                String actionRequired = buildActionRequired(effectiveIsClean, effectiveIsFuelEnough, hasVehicleIssue);
                String pendingVehicleStatus = hasVehicleIssue ? "PENDING_CHECK" : "NORMAL";
                boolean preserveCompletedFollowUp = shouldPreserveCompletedFollowUp(
                        beforeSnapshot,
                        effectiveIsClean,
                        effectiveIsFuelEnough,
                        normalizedIssueDescription,
                        actionRequired
                );

                record.setReturnTime(effectiveReturnTime);
                record.setReturnMileage(effectiveReturnMileage);
                record.setReturnVehiclePhotos(effectiveReturnVehiclePhotos);
                record.setReturnMileagePhoto(effectiveReturnMileagePhoto);
                record.setReturnFuelPhoto(effectiveReturnFuelPhoto);
                record.setIsClean(effectiveIsClean);
                record.setIsFuelEnough(effectiveIsFuelEnough);
                record.setIssueDescription(normalizedIssueDescription);
                record.setIssuePhotos(effectiveIssuePhotos);
                record.setActionRequired(actionRequired);
                if (actionRequired.isBlank()) {
                    record.setFollowUpStatus("NONE");
                    record.setFollowUpRemark(null);
                    record.setFollowUpHandledBy(null);
                    record.setFollowUpHandledTime(null);
                    record.setFollowUpResultStatus(null);
                } else if (preserveCompletedFollowUp) {
                    record.setFollowUpStatus("COMPLETED");
                } else {
                    record.setFollowUpStatus("PENDING");
                    record.setFollowUpRemark(null);
                    record.setFollowUpHandledBy(null);
                    record.setFollowUpHandledTime(null);
                    record.setFollowUpResultStatus(null);
                }

                if (!this.updateById(record)) {
                    throw new IllegalStateException("更新借还车记录失败");
                }
                if (latestRecordForVehicle) {
                    String vehicleStatusForSync = preserveCompletedFollowUp
                            && record.getFollowUpResultStatus() != null
                            && !record.getFollowUpResultStatus().isBlank()
                            ? record.getFollowUpResultStatus()
                            : pendingVehicleStatus;
                    syncVehicleWithReturnedRecord(record, vehicleStatusForSync);
                }
            } else {
                if (!this.updateById(record)) {
                    throw new IllegalStateException("更新借还车记录失败");
                }
                if (latestRecordForVehicle) {
                    syncVehicleWithTakenRecord(record);
                }
            }

            registerCommitCleanup(oldPhotosToDelete);
            createEditLog(beforeSnapshot, record, operatorId, operatorName);
            enrichRecord(record);
            return record;
        });
    }

    @Transactional
    public VehicleBorrowRecord createSupplementRecordByAdmin(
            Long vehicleId,
            Long driverId,
            String usageReason,
            String destination,
            LocalDateTime takeTime,
            LocalDateTime expectedReturnTime,
            BigDecimal takeMileage) {
        if (vehicleId == null) {
            throw new IllegalArgumentException("请选择车辆");
        }
        if (driverId == null) {
            throw new IllegalArgumentException("请选择驾驶员");
        }
        if (takeTime == null) {
            throw new IllegalArgumentException("取车时间不能为空");
        }
        if (takeTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("取车时间不能晚于当前时间");
        }
        if (takeMileage == null) {
            throw new IllegalArgumentException("请输入取车里程");
        }
        if (takeMileage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("取车里程不能小于0");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("请填写目的地/去向");
        }
        if (expectedReturnTime != null && !expectedReturnTime.isAfter(takeTime)) {
            throw new IllegalArgumentException("预计还车时间必须晚于取车时间");
        }

        return withBorrowLocks(driverId, vehicleId, () -> {
            Driver driver = driverService.getById(driverId);
            if (driver == null || !"ACTIVE".equals(driver.getStatus())) {
                throw new IllegalArgumentException("驾驶员不存在或已停用");
            }
            if (findActiveRecord(driverId) != null) {
                throw new IllegalArgumentException("该驾驶员当前还有未归还记录，不能重复补登记");
            }
            if (findActiveVehicleRecord(vehicleId) != null) {
                throw new IllegalArgumentException("该车辆当前已有未归还记录，不能重复补登记");
            }

            Vehicle vehicle = vehicleService.getById(vehicleId);
            if (vehicle == null) {
                throw new IllegalArgumentException("车辆不存在");
            }
            if (!"NORMAL".equals(vehicle.getStatus())) {
                throw new IllegalArgumentException("该车辆当前不是可补登记状态，请先检查车辆状态");
            }
            BigDecimal currentMileage = vehicle.getCurrentMileage() == null ? BigDecimal.ZERO : vehicle.getCurrentMileage();
            if (takeMileage.compareTo(currentMileage) < 0) {
                throw new IllegalArgumentException("取车里程不能小于车辆当前里程");
            }

            VehicleBorrowRecord record = new VehicleBorrowRecord();
            record.setRecordNo("BR" + IdUtil.getSnowflakeNextIdStr());
            record.setVehicleId(vehicle.getId());
            record.setPlateNumber(vehicle.getPlateNumber());
            record.setDriverId(driver.getId());
            record.setDriverName(driver.getDriverName());
            record.setStatus("TAKEN");
            record.setUsageReason(normalizeNullableText(usageReason));
            record.setDestination(normalizeRequiredText(destination, "请填写目的地/去向"));
            record.setExpectedReturnTime(expectedReturnTime);
            record.setTakeMileage(takeMileage);
            record.setTakeVehiclePhotos(null);
            record.setTakeMileagePhoto(null);
            record.setTakeTime(takeTime);
            record.setFollowUpStatus("NONE");
            record.setDeleted(0);

            if (!this.save(record)) {
                throw new IllegalStateException("补登记借车记录失败");
            }

            syncVehicleWithTakenRecord(record);
            enrichRecord(record);
            return record;
        });
    }

    @Transactional
    public boolean softDeleteByAdmin(Long recordId, String reason) {
        return softDeleteByAdmin(recordId, null, reason);
    }

    @Transactional
    public boolean softDeleteByAdmin(Long recordId, Long operatorId, String reason) {
        VehicleBorrowRecord existingRecord = this.getById(recordId);
        if (existingRecord == null) {
            throw new IllegalArgumentException("借还车记录不存在");
        }

        return withBorrowLocks(existingRecord.getDriverId(), existingRecord.getVehicleId(), () -> {
            VehicleBorrowRecord record = this.getById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("借还车记录不存在");
            }
            if (!"RETURNED".equals(record.getStatus())) {
                throw new IllegalArgumentException("仅支持删除已还车的借还车记录");
            }
            if (Integer.valueOf(1).equals(record.getDeleted())) {
                return true;
            }

            Vehicle vehicle = vehicleService.getById(record.getVehicleId());
            if (vehicle == null) {
                throw new IllegalArgumentException("车辆不存在");
            }

            boolean latestRecordForVehicle = isLatestRecordForVehicle(record.getVehicleId(), record.getId());

            VehicleBorrowRecord before = cloneRecord(record);
            record.setDeleted(1);
            record.setDeletedTime(LocalDateTime.now());
            record.setDeletedBy(operatorId);
            record.setDeleteReason(normalizeNullableText(reason));
            boolean updated = this.updateById(record);
            if (!updated) {
                throw new IllegalStateException("删除借还车记录失败");
            }

            if (latestRecordForVehicle) {
                syncVehicleAfterRecordDeletion(vehicle, record);
            }

            createEditLog(before, record, null, "系统");
            return true;
        });
    }

    @Transactional
    public boolean restoreByAdmin(Long recordId, Long operatorId) {
        VehicleBorrowRecord existingRecord = this.getById(recordId);
        if (existingRecord == null) {
            throw new IllegalArgumentException("借还车记录不存在");
        }

        return withBorrowLocks(existingRecord.getDriverId(), existingRecord.getVehicleId(), () -> {
            VehicleBorrowRecord record = this.getById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("借还车记录不存在");
            }
            if (!Integer.valueOf(1).equals(record.getDeleted())) {
                return true;
            }

            Vehicle vehicle = vehicleService.getById(record.getVehicleId());
            if (vehicle == null) {
                throw new IllegalArgumentException("车辆不存在");
            }

            VehicleBorrowRecord before = cloneRecord(record);
            record.setDeleted(0);
            record.setDeletedTime(null);
            record.setDeletedBy(null);
            record.setDeleteReason(null);
            boolean updated = this.updateById(record);
            if (!updated) {
                throw new IllegalStateException("恢复借还车记录失败");
            }

            if (isLatestRecordForVehicle(record.getVehicleId(), record.getId())) {
                if ("RETURNED".equals(record.getStatus())) {
                    syncVehicleWithReturnedRecord(record, resolveVehicleStatusFromReturnedRecord(record));
                } else {
                    syncVehicleWithTakenRecord(record);
                }
            }

            createEditLog(before, record, null, "系统");
            return true;
        });
    }

    @Transactional
    public boolean permanentDeleteByAdmin(Long recordId) {
        VehicleBorrowRecord existingRecord = this.getById(recordId);
        if (existingRecord == null) {
            throw new IllegalArgumentException("借还车记录不存在");
        }
        if (!Integer.valueOf(1).equals(existingRecord.getDeleted())) {
            throw new IllegalArgumentException("请先移入回收站后再永久删除");
        }

        return withBorrowLocks(existingRecord.getDriverId(), existingRecord.getVehicleId(), () -> {
            VehicleBorrowRecord record = this.getById(recordId);
            if (record == null) {
                throw new IllegalArgumentException("借还车记录不存在");
            }
            if (!Integer.valueOf(1).equals(record.getDeleted())) {
                throw new IllegalArgumentException("请先移入回收站后再永久删除");
            }

            List<String> photosToDelete = collectRecordPhotoValues(record);
            boolean removed = this.removeById(recordId);
            if (!removed) {
                throw new IllegalStateException("永久删除借还车记录失败");
            }
            editLogMapper.delete(new LambdaQueryWrapper<VehicleBorrowRecordEditLog>()
                    .eq(VehicleBorrowRecordEditLog::getRecordId, recordId));

            registerCommitCleanup(photosToDelete);
            return true;
        });
    }

    @Transactional(readOnly = true)
    public Page<VehicleBorrowRecord> getRecycleBin(int current, int size) {
        Page<VehicleBorrowRecord> page = new Page<>(current, size);
        Page<VehicleBorrowRecord> result = this.lambdaQuery()
                .eq(VehicleBorrowRecord::getDeleted, 1)
                .orderByDesc(VehicleBorrowRecord::getUpdateTime)
                .page(page);
        enrichRecords(result.getRecords());
        return result;
    }

    @Transactional
    public boolean clearRecycleBin() {
        List<VehicleBorrowRecord> deletedRecords = this.lambdaQuery()
                .eq(VehicleBorrowRecord::getDeleted, 1)
                .list();
        if (deletedRecords == null || deletedRecords.isEmpty()) {
            return true;
        }
        boolean removed = this.lambdaUpdate()
                .eq(VehicleBorrowRecord::getDeleted, 1)
                .remove();
        if (removed) {
            List<Long> deletedIds = deletedRecords.stream().map(VehicleBorrowRecord::getId).toList();
            editLogMapper.delete(new LambdaQueryWrapper<VehicleBorrowRecordEditLog>()
                    .in(VehicleBorrowRecordEditLog::getRecordId, deletedIds));
            for (VehicleBorrowRecord record : deletedRecords) {
                registerCommitCleanup(collectRecordPhotoValues(record));
            }
        }
        return removed;
    }

    public Map<String, Object> getMileageStatistics(Long driverId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<VehicleBorrowRecord> records = this.lambdaQuery()
                .eq(VehicleBorrowRecord::getDriverId, driverId)
                .eq(VehicleBorrowRecord::getStatus, "RETURNED")
                .ge(VehicleBorrowRecord::getReturnTime, startDate)
                .isNotNull(VehicleBorrowRecord::getReturnMileage)
                .isNotNull(VehicleBorrowRecord::getTakeMileage)
                .list();

        BigDecimal totalMileage = BigDecimal.ZERO;
        int tripCount = 0;
        for (VehicleBorrowRecord record : records) {
            BigDecimal tripMileage = record.getReturnMileage().subtract(record.getTakeMileage());
            if (tripMileage.compareTo(BigDecimal.ZERO) >= 0) {
                totalMileage = totalMileage.add(tripMileage);
                tripCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalMileage", totalMileage);
        result.put("tripCount", tripCount);
        result.put("days", days);
        return result;
    }

    private void syncVehicleWithTakenRecord(VehicleBorrowRecord record) {
        boolean updated = vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, record.getVehicleId())
                .set(Vehicle::getStatus, "IN_USE")
                .set(Vehicle::getCurrentDriverId, record.getDriverId())
                .set(Vehicle::getCurrentDriverName, record.getDriverName())
                .set(Vehicle::getCurrentDestination, record.getDestination())
                .set(Vehicle::getBorrowTime, record.getTakeTime())
                .set(Vehicle::getBorrowMileage, record.getTakeMileage())
                .set(Vehicle::getPickupPhotos, record.getTakeVehiclePhotos())
                .set(Vehicle::getPickupFuelPhoto, record.getTakeMileagePhoto())
                .set(Vehicle::getReturnTime, null)
                .set(Vehicle::getReturnPhotos, null)
                .set(Vehicle::getReturnFuelPhoto, null)
                .set(Vehicle::getReturnMileagePhoto, null)
                .set(Vehicle::getIsClean, null)
                .set(Vehicle::getCleanReason, null)
                .update();
        if (!updated) {
            throw new IllegalStateException("同步车辆借用信息失败");
        }
    }

    private void syncVehicleWithReturnedRecord(VehicleBorrowRecord record, String nextVehicleStatus) {
        boolean updated = vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, record.getVehicleId())
                .set(Vehicle::getStatus, nextVehicleStatus)
                .set(Vehicle::getCurrentDriverId, null)
                .set(Vehicle::getCurrentDriverName, null)
                .set(Vehicle::getCurrentDestination, null)
                .set(Vehicle::getBorrowTime, record.getTakeTime())
                .set(Vehicle::getBorrowMileage, record.getTakeMileage())
                .set(Vehicle::getPickupPhotos, record.getTakeVehiclePhotos())
                .set(Vehicle::getPickupFuelPhoto, record.getTakeMileagePhoto())
                .set(Vehicle::getReturnTime, record.getReturnTime())
                .set(Vehicle::getReturnPhotos, record.getReturnVehiclePhotos())
                .set(Vehicle::getReturnFuelPhoto, record.getReturnFuelPhoto())
                .set(Vehicle::getReturnMileagePhoto, record.getReturnMileagePhoto())
                .set(Vehicle::getIsClean, record.getIsClean())
                .set(Vehicle::getCleanReason, "PENDING_CHECK".equals(nextVehicleStatus) ? record.getActionRequired() : null)
                .set(Vehicle::getFuelReminderStatus, buildFuelReminderStatus(record.getIsFuelEnough()))
                .set(Vehicle::getFuelReminderNote, buildFuelReminderNote(record.getIsFuelEnough()))
                .set(Vehicle::getFuelReminderTime, buildFuelReminderTime(record.getIsFuelEnough(), record.getReturnTime()))
                .set(Vehicle::getCurrentMileage, record.getReturnMileage())
                .update();
        if (!updated) {
            throw new IllegalStateException("同步车辆归还信息失败");
        }
    }

    private void syncVehicleAfterRecordDeletion(Vehicle vehicle, VehicleBorrowRecord deletedRecord) {
        VehicleBorrowRecord latestRecord = this.lambdaQuery()
                .eq(VehicleBorrowRecord::getVehicleId, deletedRecord.getVehicleId())
                .and(w -> w.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .orderByDesc(VehicleBorrowRecord::getId)
                .last("limit 1")
                .one();
        if (latestRecord == null) {
            resetVehicleAfterDeletingLastRecord(vehicle, deletedRecord);
            return;
        }
        if ("TAKEN".equals(latestRecord.getStatus())) {
            syncVehicleWithTakenRecord(latestRecord);
            return;
        }
        syncVehicleWithReturnedRecord(latestRecord, resolveVehicleStatusFromReturnedRecord(latestRecord));
    }

    private void resetVehicleAfterDeletingLastRecord(Vehicle vehicle, VehicleBorrowRecord deletedRecord) {
        BigDecimal fallbackMileage = vehicle.getCurrentMileage();
        if (fallbackMileage == null) {
            fallbackMileage = deletedRecord.getReturnMileage() != null
                    ? deletedRecord.getReturnMileage()
                    : deletedRecord.getTakeMileage();
        }
        boolean updated = vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, vehicle.getId())
                .set(Vehicle::getStatus, "NORMAL")
                .set(Vehicle::getCurrentDriverId, null)
                .set(Vehicle::getCurrentDriverName, null)
                .set(Vehicle::getCurrentDestination, null)
                .set(Vehicle::getBorrowTime, null)
                .set(Vehicle::getBorrowMileage, null)
                .set(Vehicle::getPickupPhotos, null)
                .set(Vehicle::getPickupFuelPhoto, null)
                .set(Vehicle::getReturnTime, null)
                .set(Vehicle::getReturnPhotos, null)
                .set(Vehicle::getReturnFuelPhoto, null)
                .set(Vehicle::getReturnMileagePhoto, null)
                .set(Vehicle::getIsClean, null)
                .set(Vehicle::getCleanReason, null)
                .set(Vehicle::getFuelReminderStatus, "NONE")
                .set(Vehicle::getFuelReminderNote, null)
                .set(Vehicle::getFuelReminderTime, null)
                .set(fallbackMileage != null, Vehicle::getCurrentMileage, fallbackMileage)
                .update();
        if (!updated) {
            throw new IllegalStateException("重置车辆状态失败");
        }
    }

    private String resolveVehicleStatusFromReturnedRecord(VehicleBorrowRecord record) {
        if (record == null) {
            return "NORMAL";
        }
        if ("COMPLETED".equals(record.getFollowUpStatus())
                && record.getFollowUpResultStatus() != null
                && !record.getFollowUpResultStatus().isBlank()) {
            return record.getFollowUpResultStatus();
        }
        boolean hasVehicleIssue = record.getIssueDescription() != null && !record.getIssueDescription().isBlank();
        return hasVehicleIssue ? "PENDING_CHECK" : "NORMAL";
    }

    private List<String> collectRecordPhotoValues(VehicleBorrowRecord record) {
        List<String> photos = new ArrayList<>();
        if (record == null) {
            return photos;
        }
        photos.add(record.getTakeVehiclePhotos());
        photos.add(record.getTakeMileagePhoto());
        photos.add(record.getReturnVehiclePhotos());
        photos.add(record.getReturnMileagePhoto());
        photos.add(record.getReturnFuelPhoto());
        photos.add(record.getIssuePhotos());
        return photos;
    }

    private VehicleBorrowRecord cloneRecord(VehicleBorrowRecord source) {
        VehicleBorrowRecord snapshot = new VehicleBorrowRecord();
        BeanUtils.copyProperties(source, snapshot);
        return snapshot;
    }

    private String replacePhotoField(
            String currentValue,
            MultipartFile[] files,
            String fieldLabel,
            List<String> oldPhotosToDelete) {
        if (!hasSelectedFiles(files)) {
            return currentValue;
        }
        String newValue = uploadStorageService.saveImageFiles(files, fieldLabel);
        if (newValue == null || newValue.isBlank()) {
            throw new IllegalArgumentException("请上传" + fieldLabel);
        }
        if (currentValue != null && !currentValue.isBlank()) {
            oldPhotosToDelete.add(currentValue);
        }
        return newValue;
    }

    private boolean hasSelectedFiles(MultipartFile[] files) {
        return files != null && Arrays.stream(files).anyMatch(file -> file != null && !file.isEmpty());
    }

    private void registerCommitCleanup(List<String> csvValues) {
        if (csvValues == null || csvValues.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        List<String> cleanups = csvValues.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (cleanups.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    uploadStorageService.deleteCsvPaths(cleanups.toArray(String[]::new));
                }
            }
        });
    }

    private void createEditLog(
            VehicleBorrowRecord before,
            VehicleBorrowRecord after,
            Long operatorId,
            String operatorName) {
        List<String> changes = buildChangeSummary(before, after);
        if (changes.isEmpty()) {
            return;
        }
        VehicleBorrowRecordEditLog editLog = new VehicleBorrowRecordEditLog();
        editLog.setRecordId(after.getId());
        editLog.setRecordNo(after.getRecordNo());
        editLog.setOperatorId(operatorId);
        editLog.setOperatorName(operatorName);
        editLog.setChangeSummary(String.join("；", changes));
        editLog.setBeforeSnapshot(writeSnapshot(before));
        editLog.setAfterSnapshot(writeSnapshot(after));
        editLogMapper.insert(editLog);
    }

    private List<String> buildChangeSummary(VehicleBorrowRecord before, VehicleBorrowRecord after) {
        List<String> changes = new ArrayList<>();
        addValueChange(changes, "用车事由", before.getUsageReason(), after.getUsageReason());
        addValueChange(changes, "目的地/去向", before.getDestination(), after.getDestination());
        addValueChange(changes, "取车时间", before.getTakeTime(), after.getTakeTime());
        addValueChange(changes, "预计还车时间", before.getExpectedReturnTime(), after.getExpectedReturnTime());
        addValueChange(changes, "取车里程", before.getTakeMileage(), after.getTakeMileage());
        addPhotoChange(changes, "取车车辆照片", before.getTakeVehiclePhotos(), after.getTakeVehiclePhotos());
        addPhotoChange(changes, "取车公里数照片", before.getTakeMileagePhoto(), after.getTakeMileagePhoto());
        addValueChange(changes, "还车时间", before.getReturnTime(), after.getReturnTime());
        addValueChange(changes, "还车里程", before.getReturnMileage(), after.getReturnMileage());
        addPhotoChange(changes, "还车停车照片", before.getReturnVehiclePhotos(), after.getReturnVehiclePhotos());
        addPhotoChange(changes, "还车公里数照片", before.getReturnMileagePhoto(), after.getReturnMileagePhoto());
        addPhotoChange(changes, "还车油表照片", before.getReturnFuelPhoto(), after.getReturnFuelPhoto());
        addValueChange(changes, "车辆是否干净", formatCleanStatus(before.getIsClean()), formatCleanStatus(after.getIsClean()));
        addValueChange(changes, "油量是否不少于半箱", formatFuelStatus(before.getIsFuelEnough()), formatFuelStatus(after.getIsFuelEnough()));
        addValueChange(changes, "车辆异常说明", before.getIssueDescription(), after.getIssueDescription());
        addPhotoChange(changes, "车辆异常照片", before.getIssuePhotos(), after.getIssuePhotos());
        addValueChange(changes, "需处理事项", before.getActionRequired(), after.getActionRequired());
        addValueChange(changes, "闭环状态", before.getFollowUpStatus(), after.getFollowUpStatus());
        addValueChange(changes, "闭环结果", before.getFollowUpResultStatus(), after.getFollowUpResultStatus());
        return changes;
    }

    private void addValueChange(List<String> changes, String label, Object before, Object after) {
        String beforeText = normalizeDisplayValue(before);
        String afterText = normalizeDisplayValue(after);
        if (!Objects.equals(beforeText, afterText)) {
            changes.add(label + "：" + beforeText + " -> " + afterText);
        }
    }

    private void addPhotoChange(List<String> changes, String label, String before, String after) {
        String beforeText = normalizeCsvValue(before);
        String afterText = normalizeCsvValue(after);
        if (Objects.equals(beforeText, afterText)) {
            return;
        }
        if ("-".equals(afterText)) {
            changes.add(label + "已清空");
            return;
        }
        changes.add(label + "已替换(" + countCsvItems(after) + "张)");
    }

    private int countCsvItems(String csv) {
        if (csv == null || csv.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .count();
    }

    private String normalizeDisplayValue(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? "-" : text;
    }

    private String normalizeCsvValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("-");
    }

    private String formatCleanStatus(Integer value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(1).equals(value) ? "干净" : "需洗车";
    }

    private String formatFuelStatus(Integer value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(1).equals(value) ? "半箱及以上" : "不足半箱";
    }

    private String writeSnapshot(VehicleBorrowRecord record) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("recordId", record.getId());
        snapshot.put("recordNo", record.getRecordNo());
        snapshot.put("plateNumber", record.getPlateNumber());
        snapshot.put("driverName", record.getDriverName());
        snapshot.put("status", record.getStatus());
        snapshot.put("usageReason", record.getUsageReason());
        snapshot.put("destination", record.getDestination());
        snapshot.put("takeTime", record.getTakeTime());
        snapshot.put("expectedReturnTime", record.getExpectedReturnTime());
        snapshot.put("takeMileage", record.getTakeMileage());
        snapshot.put("takeVehiclePhotos", record.getTakeVehiclePhotos());
        snapshot.put("takeMileagePhoto", record.getTakeMileagePhoto());
        snapshot.put("returnTime", record.getReturnTime());
        snapshot.put("returnMileage", record.getReturnMileage());
        snapshot.put("returnVehiclePhotos", record.getReturnVehiclePhotos());
        snapshot.put("returnMileagePhoto", record.getReturnMileagePhoto());
        snapshot.put("returnFuelPhoto", record.getReturnFuelPhoto());
        snapshot.put("isClean", record.getIsClean());
        snapshot.put("isFuelEnough", record.getIsFuelEnough());
        snapshot.put("issueDescription", record.getIssueDescription());
        snapshot.put("issuePhotos", record.getIssuePhotos());
        snapshot.put("actionRequired", record.getActionRequired());
        snapshot.put("followUpStatus", record.getFollowUpStatus());
        snapshot.put("followUpRemark", record.getFollowUpRemark());
        snapshot.put("followUpResultStatus", record.getFollowUpResultStatus());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("写入借还车修改日志失败", exception);
        }
    }

    private boolean isLatestRecordForVehicle(Long vehicleId, Long recordId) {
        VehicleBorrowRecord latestRecord = this.lambdaQuery()
                .eq(VehicleBorrowRecord::getVehicleId, vehicleId)
                .and(w -> w.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .orderByDesc(VehicleBorrowRecord::getId)
                .last("limit 1")
                .one();
        return latestRecord != null && recordId.equals(latestRecord.getId());
    }

    private String appendRemarkNote(String remark, String note) {
        if (remark == null || remark.isBlank()) {
            return note;
        }
        return remark.trim() + "；" + note;
    }

    private String buildFuelReminderStatus(Integer isFuelEnough) {
        return Integer.valueOf(0).equals(isFuelEnough) ? "PENDING" : "NONE";
    }

    private String buildFuelReminderNote(Integer isFuelEnough) {
        return Integer.valueOf(0).equals(isFuelEnough)
                ? "上次还车时油量不足半箱，请尽快补油至半箱以上"
                : null;
    }

    private LocalDateTime buildFuelReminderTime(Integer isFuelEnough, LocalDateTime referenceTime) {
        return Integer.valueOf(0).equals(isFuelEnough) ? referenceTime : null;
    }

    static String resolveReturnFuelPhoto(String returnMileagePhoto, String returnFuelPhoto) {
        if (returnFuelPhoto == null || returnFuelPhoto.isBlank()) {
            return returnMileagePhoto;
        }
        return returnFuelPhoto;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String normalizeRequiredText(String value, String emptyMessage) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return value.trim();
    }

    private void validateBinaryValue(Integer value, String fieldLabel) {
        if (value == null || (!Integer.valueOf(0).equals(value) && !Integer.valueOf(1).equals(value))) {
            throw new IllegalArgumentException(fieldLabel + "只能选择是或否");
        }
    }

    private boolean shouldPreserveCompletedFollowUp(
            VehicleBorrowRecord beforeSnapshot,
            Integer isClean,
            Integer isFuelEnough,
            String issueDescription,
            String actionRequired) {
        return "COMPLETED".equals(beforeSnapshot.getFollowUpStatus())
                && Objects.equals(beforeSnapshot.getIsClean(), isClean)
                && Objects.equals(beforeSnapshot.getIsFuelEnough(), isFuelEnough)
                && Objects.equals(beforeSnapshot.getIssueDescription(), issueDescription)
                && Objects.equals(beforeSnapshot.getActionRequired(), actionRequired);
    }

    private String buildActionRequired(Integer isClean, Integer isFuelEnough, boolean hasIssue) {
        StringBuilder builder = new StringBuilder();
        if (Integer.valueOf(0).equals(isClean)) {
            builder.append("需要洗车");
        }
        if (Integer.valueOf(0).equals(isFuelEnough)) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append("需要加油");
        }
        if (hasIssue) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append("车辆异常待处理");
        }
        return builder.toString();
    }

    private VehicleBorrowRecord findActiveRecord(Long driverId) {
        return this.lambdaQuery()
                .eq(VehicleBorrowRecord::getDriverId, driverId)
                .eq(VehicleBorrowRecord::getStatus, "TAKEN")
                .and(w -> w.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .orderByDesc(VehicleBorrowRecord::getTakeTime)
                .last("limit 1")
                .one();
    }

    private VehicleBorrowRecord findActiveVehicleRecord(Long vehicleId) {
        return this.lambdaQuery()
                .eq(VehicleBorrowRecord::getVehicleId, vehicleId)
                .eq(VehicleBorrowRecord::getStatus, "TAKEN")
                .and(w -> w.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .orderByDesc(VehicleBorrowRecord::getTakeTime)
                .last("limit 1")
                .one();
    }

    private void enrichRecords(List<VehicleBorrowRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        List<Long> vehicleIds = records.stream()
                .map(VehicleBorrowRecord::getVehicleId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Vehicle> vehicleMap = new HashMap<>();
        if (!vehicleIds.isEmpty()) {
            List<Vehicle> vehicles = vehicleService.listByIds(vehicleIds);
            for (Vehicle vehicle : vehicles) {
                vehicleMap.put(vehicle.getId(), vehicle);
            }
        }

        List<Long> handlerIds = records.stream()
                .map(VehicleBorrowRecord::getFollowUpHandledBy)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, String> handlerMap = new HashMap<>();
        if (!handlerIds.isEmpty()) {
            List<SysUser> handlers = sysUserService.listByIds(handlerIds);
            for (SysUser handler : handlers) {
                String displayName = handler.getRealName() != null && !handler.getRealName().isBlank()
                        ? handler.getRealName()
                        : handler.getUsername();
                handlerMap.put(handler.getId(), displayName);
            }
        }

        for (VehicleBorrowRecord record : records) {
            Vehicle vehicle = vehicleMap.get(record.getVehicleId());
            if (vehicle != null) {
                record.setVehicleModel(vehicle.getModel());
            }
            if (record.getTakeMileage() != null && record.getReturnMileage() != null) {
                record.setTripMileage(record.getReturnMileage().subtract(record.getTakeMileage()));
            }
            if (record.getFollowUpHandledBy() != null) {
                record.setFollowUpHandledByName(handlerMap.get(record.getFollowUpHandledBy()));
            }
        }
    }

    private void enrichRecord(VehicleBorrowRecord record) {
        if (record == null) {
            return;
        }
        enrichRecords(List.of(record));
    }

    private <T> T withBorrowLocks(Long driverId, Long vehicleId, Supplier<T> action) {
        List<String> lockKeys = new ArrayList<>();
        if (driverId != null) {
            lockKeys.add("vehicle-borrow:driver:" + driverId);
        }
        if (vehicleId != null) {
            lockKeys.add("vehicle-borrow:vehicle:" + vehicleId);
        }
        List<String> uniqueLockKeys = lockKeys.stream().distinct().sorted().toList();
        List<String> acquiredLocks = new ArrayList<>();
        for (String lockKey : uniqueLockKeys) {
            acquireLock(lockKey);
            acquiredLocks.add(lockKey);
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                        releaseLock(acquiredLocks.get(i));
                    }
                }
            });
            return action.get();
        }

        try {
            return action.get();
        } finally {
            for (int i = acquiredLocks.size() - 1; i >= 0; i--) {
                releaseLock(acquiredLocks.get(i));
            }
        }
    }

    private void acquireLock(String lockKey) {
        Integer result = jdbcTemplate.queryForObject("SELECT GET_LOCK(?, 5)", Integer.class, lockKey);
        if (result == null || result != 1) {
            throw new IllegalStateException("系统繁忙，请稍后重试");
        }
    }

    private void releaseLock(String lockKey) {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT RELEASE_LOCK(?)", Integer.class, lockKey);
            if (result == null || result != 1) {
                log.warn("释放锁失败或锁不存在: " + lockKey);
            }
        } catch (Exception e) {
            log.error("释放锁时发生异常: " + lockKey + ", 错误: " + e.getMessage());
        }
    }
}
