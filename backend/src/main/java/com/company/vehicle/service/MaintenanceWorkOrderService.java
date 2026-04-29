package com.company.vehicle.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.vehicle.entity.MaintenanceWorkOrder;
import com.company.vehicle.entity.MaintenanceWorkOrderAttachment;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.entity.VehicleBorrowRecord;
import com.company.vehicle.mapper.MaintenanceWorkOrderAttachmentMapper;
import com.company.vehicle.mapper.MaintenanceWorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MaintenanceWorkOrderService extends ServiceImpl<MaintenanceWorkOrderMapper, MaintenanceWorkOrder> {

    private static final List<String> ACTIVE_STATUSES = List.of(
            "PENDING_APPROVAL",
            "APPROVED",
            "IN_REPAIR",
            "WAIT_ACCEPTANCE"
    );
    private static final List<String> CLOSED_STATUSES = List.of("COMPLETED", "CANCELED", "REJECTED");

    private final MaintenanceWorkOrderAttachmentMapper attachmentMapper;
    private final VehicleService vehicleService;
    private final VehicleBorrowRecordService vehicleBorrowRecordService;

    public MaintenanceWorkOrderService(
            MaintenanceWorkOrderAttachmentMapper attachmentMapper,
            VehicleService vehicleService,
            VehicleBorrowRecordService vehicleBorrowRecordService) {
        this.attachmentMapper = attachmentMapper;
        this.vehicleService = vehicleService;
        this.vehicleBorrowRecordService = vehicleBorrowRecordService;
    }

    public Page<MaintenanceWorkOrder> getPage(
            int current,
            int size,
            String status,
            Long vehicleId,
            String plateNumber,
            String keyword) {
        Page<MaintenanceWorkOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<MaintenanceWorkOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(MaintenanceWorkOrder::getStatus, status.trim());
        }
        if (vehicleId != null) {
            wrapper.eq(MaintenanceWorkOrder::getVehicleId, vehicleId);
        }
        if (plateNumber != null && !plateNumber.isBlank()) {
            wrapper.like(MaintenanceWorkOrder::getPlateNumberSnapshot, plateNumber.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(query -> query.like(MaintenanceWorkOrder::getOrderNo, normalized)
                    .or().like(MaintenanceWorkOrder::getIssueDescription, normalized)
                    .or().like(MaintenanceWorkOrder::getRepairVendor, normalized)
                    .or().like(MaintenanceWorkOrder::getRemark, normalized));
        }
        wrapper.orderByDesc(MaintenanceWorkOrder::getCreateTime);
        return this.page(page, wrapper);
    }

    public MaintenanceWorkOrder getDetail(Long id) {
        MaintenanceWorkOrder order = this.getById(id);
        if (order == null) {
            return null;
        }
        order.setAttachments(attachmentMapper.selectList(
                new LambdaQueryWrapper<MaintenanceWorkOrderAttachment>()
                        .eq(MaintenanceWorkOrderAttachment::getOrderId, id)
                        .orderByAsc(MaintenanceWorkOrderAttachment::getCreateTime)
                        .orderByAsc(MaintenanceWorkOrderAttachment::getId)
        ));
        return order;
    }

    @Transactional
    public MaintenanceWorkOrder createManual(
            Long vehicleId,
            String plateNumber,
            String workType,
            String issueDescription,
            BigDecimal estimatedCost,
            LocalDateTime expectedFinishTime,
            String repairVendor,
            String repairContact,
            String remark,
            Long reportedBy) {
        Vehicle vehicle = resolveVehicle(vehicleId, plateNumber);
        MaintenanceWorkOrder order = new MaintenanceWorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setVehicleId(vehicle.getId());
        order.setPlateNumberSnapshot(vehicle.getPlateNumber());
        order.setSourceType("MANUAL");
        order.setWorkType(normalize(workType) == null ? "REPAIR" : normalize(workType));
        order.setStatus("DRAFT");
        order.setIssueDescription(normalize(issueDescription));
        order.setEstimatedCost(estimatedCost);
        order.setExpectedFinishTime(expectedFinishTime);
        order.setRepairVendor(normalize(repairVendor));
        order.setRepairContact(normalize(repairContact));
        order.setRemark(normalize(remark));
        order.setReportedBy(reportedBy);
        order.setReportedTime(LocalDateTime.now());
        if (!this.save(order)) {
            throw new IllegalStateException("创建维修工单失败");
        }
        return order;
    }

    @Transactional
    public MaintenanceWorkOrder createFromBorrowRecord(
            Long recordId,
            Long operatorId,
            String issueDescription,
            LocalDateTime expectedFinishTime,
            String remark) {
        VehicleBorrowRecord record = vehicleBorrowRecordService.getById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("借还车记录不存在");
        }
        if (!"RETURNED".equals(record.getStatus())) {
            throw new IllegalArgumentException("仅支持已还车记录创建维修工单");
        }
        if (Integer.valueOf(1).equals(record.getDeleted())) {
            throw new IllegalArgumentException("借还车记录已删除，不能创建维修工单");
        }
        Vehicle vehicle = vehicleService.getByIdForUpdate(record.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        if (hasUnclosedOrderForBorrowRecord(recordId)) {
            throw new IllegalArgumentException("该借还车记录已有未关闭维修工单，请勿重复创建");
        }
        MaintenanceWorkOrder order = new MaintenanceWorkOrder();
        order.setOrderNo(generateOrderNo());
        order.setVehicleId(record.getVehicleId());
        order.setPlateNumberSnapshot(record.getPlateNumber());
        order.setSourceType("BORROW_RECORD");
        order.setSourceRecordId(recordId);
        order.setWorkType("REPAIR");
        order.setStatus("PENDING_APPROVAL");
        String resolvedIssue = normalize(issueDescription);
        if (resolvedIssue == null) {
            resolvedIssue = normalize(record.getIssueDescription());
        }
        if (resolvedIssue == null) {
            resolvedIssue = normalize(record.getActionRequired());
        }
        order.setIssueDescription(resolvedIssue);
        order.setReportedBy(operatorId);
        order.setReportedTime(LocalDateTime.now());
        order.setExpectedFinishTime(expectedFinishTime);
        order.setRemark(normalize(remark));
        if (!this.save(order)) {
            throw new IllegalStateException("创建维修工单失败");
        }
        setVehicleMaintenance(record.getVehicleId());
        return order;
    }

    @Transactional
    public boolean submit(Long id) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if (!"DRAFT".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态无法提交审批");
        }
        order.setStatus("PENDING_APPROVAL");
        boolean updated = this.updateById(order);
        if (updated) {
            setVehicleMaintenance(order.getVehicleId());
        }
        return updated;
    }

    @Transactional
    public boolean approve(Long id, Long approverId, String comment) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if (!"PENDING_APPROVAL".equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态无法审批通过");
        }
        order.setStatus("APPROVED");
        order.setRemark(mergeRemark(order.getRemark(), buildOperatorComment("审批通过", approverId, comment)));
        boolean updated = this.updateById(order);
        if (updated) {
            setVehicleMaintenance(order.getVehicleId());
        }
        return updated;
    }

    @Transactional
    public boolean reject(Long id, Long approverId, String comment) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if (!"PENDING_APPROVAL".equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态无法审批驳回");
        }
        order.setStatus("REJECTED");
        order.setRemark(mergeRemark(order.getRemark(), buildOperatorComment("审批驳回", approverId, comment)));
        boolean updated = this.updateById(order);
        if (updated) {
            resetVehicleIfNoActiveOrders(order.getVehicleId(), order.getId());
        }
        return updated;
    }

    @Transactional
    public boolean startRepair(Long id, LocalDateTime startTime, String vendor, String contact) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if (!"APPROVED".equals(order.getStatus())) {
            throw new IllegalArgumentException("只有已批准的工单可以开工");
        }
        order.setStatus("IN_REPAIR");
        order.setRepairStartTime(startTime != null ? startTime : LocalDateTime.now());
        if (normalize(vendor) != null) {
            order.setRepairVendor(normalize(vendor));
        }
        if (normalize(contact) != null) {
            order.setRepairContact(normalize(contact));
        }
        boolean updated = this.updateById(order);
        if (updated) {
            setVehicleMaintenance(order.getVehicleId());
        }
        return updated;
    }

    @Transactional
    public boolean finishRepair(Long id, LocalDateTime finishTime, BigDecimal actualCost, String remark) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if (!"IN_REPAIR".equals(order.getStatus())) {
            throw new IllegalArgumentException("只有维修中的工单可以完工");
        }
        order.setStatus("WAIT_ACCEPTANCE");
        order.setRepairFinishTime(finishTime != null ? finishTime : LocalDateTime.now());
        if (actualCost != null) {
            order.setActualCost(actualCost);
        }
        order.setRemark(mergeRemark(order.getRemark(), normalize(remark)));
        return this.updateById(order);
    }

    @Transactional
    public boolean accept(Long id, Long acceptBy, String acceptanceResult, String closeResultStatus, String remark) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if (!"WAIT_ACCEPTANCE".equals(order.getStatus())) {
            throw new IllegalArgumentException("只有待验收的工单可以验收");
        }
        order.setStatus("COMPLETED");
        order.setAcceptedBy(acceptBy);
        order.setAcceptedTime(LocalDateTime.now());
        order.setAcceptanceResult(normalize(acceptanceResult));
        order.setCloseResultStatus(normalize(closeResultStatus));
        order.setRemark(mergeRemark(order.getRemark(), normalize(remark)));
        boolean updated = this.updateById(order);
        if (updated) {
            resetVehicleIfNoActiveOrders(order.getVehicleId(), null);
        }
        return updated;
    }

    @Transactional
    public boolean cancel(Long id, String remark) {
        MaintenanceWorkOrder order = getAndValidate(id);
        if ("COMPLETED".equals(order.getStatus()) || "CANCELED".equals(order.getStatus())) {
            throw new IllegalArgumentException("当前状态不能取消工单");
        }
        order.setStatus("CANCELED");
        order.setRemark(mergeRemark(order.getRemark(), remark));
        boolean updated = this.updateById(order);
        if (updated) {
            resetVehicleIfNoActiveOrders(order.getVehicleId(), order.getId());
        }
        return updated;
    }

    public boolean hasUnclosedOrderForBorrowRecord(Long recordId) {
        if (recordId == null) {
            return false;
        }
        Long count = this.lambdaQuery()
                .eq(MaintenanceWorkOrder::getSourceType, "BORROW_RECORD")
                .eq(MaintenanceWorkOrder::getSourceRecordId, recordId)
                .notIn(MaintenanceWorkOrder::getStatus, CLOSED_STATUSES)
                .count();
        return count != null && count > 0;
    }

    private MaintenanceWorkOrder getAndValidate(Long id) {
        MaintenanceWorkOrder order = this.getById(id);
        if (order == null) {
            throw new IllegalArgumentException("维修工单不存在");
        }
        return order;
    }

    private Vehicle resolveVehicle(Long vehicleId, String plateNumber) {
        if (vehicleId != null) {
            Vehicle vehicle = vehicleService.getByIdForUpdate(vehicleId);
            if (vehicle != null) {
                return vehicle;
            }
            throw new IllegalArgumentException("车辆不存在");
        }
        if (plateNumber == null || plateNumber.trim().isBlank()) {
            throw new IllegalArgumentException("请选择车辆或填写车牌号");
        }
        Vehicle byPlate = vehicleService.getByPlateNumber(plateNumber.trim());
        if (byPlate == null) {
            throw new IllegalArgumentException("未找到对应车牌的车辆");
        }
        Vehicle vehicle = vehicleService.getByIdForUpdate(byPlate.getId());
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        return vehicle;
    }

    private void setVehicleMaintenance(Long vehicleId) {
        Vehicle vehicle = vehicleService.getByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw new IllegalArgumentException("车辆不存在");
        }
        if ("IN_USE".equals(vehicle.getStatus())) {
            throw new IllegalArgumentException("车辆当前正在使用中，无法转入维修状态");
        }
        vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, vehicleId)
                .set(Vehicle::getStatus, "MAINTENANCE")
                .update();
    }

    private void resetVehicleIfNoActiveOrders(Long vehicleId, Long excludeOrderId) {
        Long activeCount = this.lambdaQuery()
                .eq(MaintenanceWorkOrder::getVehicleId, vehicleId)
                .in(MaintenanceWorkOrder::getStatus, ACTIVE_STATUSES)
                .ne(excludeOrderId != null, MaintenanceWorkOrder::getId, excludeOrderId)
                .count();
        if (activeCount != null && activeCount > 0) {
            return;
        }
        Vehicle vehicle = vehicleService.getById(vehicleId);
        if (vehicle == null || !"MAINTENANCE".equals(vehicle.getStatus())) {
            return;
        }
        vehicleService.lambdaUpdate()
                .eq(Vehicle::getId, vehicleId)
                .set(Vehicle::getStatus, "NORMAL")
                .update();
    }

    private String generateOrderNo() {
        return "MW" + IdUtil.getSnowflakeNextIdStr();
    }

    private String mergeRemark(String existing, String append) {
        String left = normalize(existing);
        String right = normalize(append);
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left + "；" + right;
    }

    private String buildOperatorComment(String action, Long operatorId, String comment) {
        String normalized = normalize(comment);
        String operator = operatorId == null ? "系统" : "用户#" + operatorId;
        if (normalized == null) {
            return action + "(" + operator + ")";
        }
        return action + "(" + operator + "): " + normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
