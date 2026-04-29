package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.vehicle.dto.DriverVehicleActivityItem;
import com.company.vehicle.entity.MaintenanceWorkOrder;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.entity.VehicleBorrowRecord;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DriverVehicleActivityService {

    private static final List<String> ACTIVE_MAINTENANCE_STATUSES = List.of(
            "PENDING_APPROVAL",
            "APPROVED",
            "IN_REPAIR",
            "WAIT_ACCEPTANCE"
    );

    private final VehicleService vehicleService;
    private final VehicleBorrowRecordService vehicleBorrowRecordService;
    private final MaintenanceWorkOrderService maintenanceWorkOrderService;

    public DriverVehicleActivityService(
            VehicleService vehicleService,
            VehicleBorrowRecordService vehicleBorrowRecordService,
            MaintenanceWorkOrderService maintenanceWorkOrderService) {
        this.vehicleService = vehicleService;
        this.vehicleBorrowRecordService = vehicleBorrowRecordService;
        this.maintenanceWorkOrderService = maintenanceWorkOrderService;
    }

    public Map<String, Object> getOverview(String keyword) {
        List<Vehicle> vehicles = loadVehicles(keyword);
        vehicleService.enrichTrafficRestriction(vehicles);

        List<Long> vehicleIds = vehicles.stream()
                .map(Vehicle::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, VehicleBorrowRecord> activeBorrowByVehicle = findLatestBorrowRecords(vehicleIds, true);
        Map<Long, VehicleBorrowRecord> pendingFollowUpByVehicle = findLatestPendingFollowUpRecords(vehicleIds);
        Map<Long, MaintenanceWorkOrder> activeMaintenanceByVehicle = findLatestActiveMaintenanceOrders(vehicleIds);

        List<DriverVehicleActivityItem> data = vehicles.stream()
                .map(vehicle -> buildItem(
                        vehicle,
                        activeBorrowByVehicle.get(vehicle.getId()),
                        pendingFollowUpByVehicle.get(vehicle.getId()),
                        activeMaintenanceByVehicle.get(vehicle.getId())))
                .sorted(Comparator
                        .comparing(DriverVehicleActivityItem::getPriority, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DriverVehicleActivityItem::getPlateNumber, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        long cntAvailable = 0, cntInUse = 0, cntPending = 0, cntMaintenance = 0,
                cntOverdue = 0, cntRestricted = 0, cntFuelReminder = 0;
        for (DriverVehicleActivityItem item : data) {
            if (Boolean.TRUE.equals(item.getAvailableForBorrow())) cntAvailable++;
            String type = item.getActivityType();
            if ("IN_USE".equals(type) || "OVERDUE_RETURN".equals(type)) cntInUse++;
            if ("FOLLOW_UP_PENDING".equals(type)) cntPending++;
            if (type != null && type.startsWith("MAINTENANCE_")) cntMaintenance++;
            if ("OVERDUE_RETURN".equals(type)) cntOverdue++;
            if (isRestrictedForBorrow(item)) cntRestricted++;
            if ("PENDING".equals(item.getFuelReminderStatus())) cntFuelReminder++;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", data.size());
        summary.put("available", cntAvailable);
        summary.put("inUse", cntInUse);
        summary.put("pending", cntPending);
        summary.put("maintenance", cntMaintenance);
        summary.put("overdue", cntOverdue);
        summary.put("restricted", cntRestricted);
        summary.put("fuelReminderPending", cntFuelReminder);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("data", data);
        return result;
    }

    private List<Vehicle> loadVehicles(String keyword) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String normalized = keyword.trim();
            wrapper.and(query -> query.like(Vehicle::getPlateNumber, normalized)
                    .or().like(Vehicle::getBrand, normalized)
                    .or().like(Vehicle::getModel, normalized)
                    .or().like(Vehicle::getParkingLocation, normalized));
        }
        wrapper.orderByAsc(Vehicle::getPlateNumber);
        return vehicleService.list(wrapper);
    }

    private Map<Long, VehicleBorrowRecord> findLatestBorrowRecords(List<Long> vehicleIds, boolean activeOnly) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VehicleBorrowRecord::getVehicleId, vehicleIds)
                .and(query -> query.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .eq(activeOnly, VehicleBorrowRecord::getStatus, "TAKEN")
                .orderByDesc(VehicleBorrowRecord::getTakeTime)
                .orderByDesc(VehicleBorrowRecord::getId);
        return toLatestBorrowRecordMap(vehicleBorrowRecordService.list(wrapper));
    }

    private Map<Long, VehicleBorrowRecord> findLatestPendingFollowUpRecords(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VehicleBorrowRecord::getVehicleId, vehicleIds)
                .and(query -> query.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .eq(VehicleBorrowRecord::getStatus, "RETURNED")
                .eq(VehicleBorrowRecord::getFollowUpStatus, "PENDING")
                .orderByDesc(VehicleBorrowRecord::getReturnTime)
                .orderByDesc(VehicleBorrowRecord::getId);
        return toLatestBorrowRecordMap(vehicleBorrowRecordService.list(wrapper));
    }

    private Map<Long, MaintenanceWorkOrder> findLatestActiveMaintenanceOrders(List<Long> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Map.of();
        }
        return maintenanceWorkOrderService.lambdaQuery()
                .in(MaintenanceWorkOrder::getVehicleId, vehicleIds)
                .in(MaintenanceWorkOrder::getStatus, ACTIVE_MAINTENANCE_STATUSES)
                .orderByDesc(MaintenanceWorkOrder::getReportedTime)
                .orderByDesc(MaintenanceWorkOrder::getId)
                .list()
                .stream()
                .filter(order -> order.getVehicleId() != null)
                .collect(Collectors.toMap(
                        MaintenanceWorkOrder::getVehicleId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<Long, VehicleBorrowRecord> toLatestBorrowRecordMap(List<VehicleBorrowRecord> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        return records.stream()
                .filter(record -> record.getVehicleId() != null)
                .collect(Collectors.toMap(
                        VehicleBorrowRecord::getVehicleId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private DriverVehicleActivityItem buildItem(
            Vehicle vehicle,
            VehicleBorrowRecord activeBorrowRecord,
            VehicleBorrowRecord pendingFollowUpRecord,
            MaintenanceWorkOrder activeMaintenanceOrder) {
        DriverVehicleActivityItem item = new DriverVehicleActivityItem();
        item.setVehicleId(vehicle.getId());
        item.setPlateNumber(vehicle.getPlateNumber());
        item.setVehicleType(vehicle.getVehicleType());
        item.setBrand(vehicle.getBrand());
        item.setModel(vehicle.getModel());
        item.setParkingLocation(vehicle.getParkingLocation());
        item.setStatus(vehicle.getStatus());
        item.setStatusLabel(resolveVehicleStatusLabel(vehicle.getStatus()));
        item.setFuelReminderStatus(vehicle.getFuelReminderStatus());
        item.setFuelReminderNote(vehicle.getFuelReminderNote());
        item.setFuelReminderTime(vehicle.getFuelReminderTime());
        item.setTrafficRestrictedToday(vehicle.getTrafficRestrictedToday());
        item.setTrafficRestrictionReleasedToday(vehicle.getTrafficRestrictionReleasedToday());
        item.setTrafficRestrictionMessage(vehicle.getTrafficRestrictionMessage());
        item.setUpdateTime(vehicle.getUpdateTime());

        String vehicleStatus = vehicle.getStatus() == null ? "NORMAL" : vehicle.getStatus();
        switch (vehicleStatus) {
            case "IN_USE" -> populateInUseItem(item, vehicle, activeBorrowRecord);
            case "PENDING_CHECK" -> populatePendingFollowUpItem(item, vehicle, pendingFollowUpRecord);
            case "MAINTENANCE" -> populateMaintenanceItem(item, vehicle, activeMaintenanceOrder);
            default -> populateIdleItem(item, vehicle);
        }
        return item;
    }

    private void populateIdleItem(DriverVehicleActivityItem item, Vehicle vehicle) {
        boolean restricted = isRestrictedForBorrow(item);
        boolean available = "NORMAL".equals(vehicle.getStatus()) && !restricted;
        item.setAvailableForBorrow(available);
        item.setActivityType(restricted ? "RESTRICTED_IDLE" : "IDLE");
        item.setPriority(restricted ? 40 : 10);
        if (restricted) {
            item.setActivityTitle("当前空闲，但今天受限行影响暂不可借用");
            item.setActivitySubtitle(firstNonBlank(
                    vehicle.getTrafficRestrictionMessage(),
                    buildParkingLine(vehicle.getParkingLocation()),
                    "请联系办公室确认是否放行"));
            return;
        }
        item.setActivityTitle(firstNonBlank(
                buildParkingIdleTitle(vehicle.getParkingLocation()),
                "当前空闲，可直接借用"));
        item.setActivitySubtitle(firstNonBlank(
                buildFuelReminderLine(vehicle.getFuelReminderStatus(), vehicle.getFuelReminderNote()),
                buildParkingLine(vehicle.getParkingLocation()),
                "当前没有待处理事项"));
    }

    private void populateInUseItem(DriverVehicleActivityItem item, Vehicle vehicle, VehicleBorrowRecord record) {
        String driverName = firstNonBlank(
                record == null ? null : record.getDriverName(),
                vehicle.getCurrentDriverName(),
                "驾驶员");
        String destination = firstNonBlank(
                record == null ? null : record.getDestination(),
                vehicle.getCurrentDestination());
        item.setAvailableForBorrow(false);
        item.setBorrowRecordId(record == null ? null : record.getId());
        item.setCurrentDriverName(driverName);
        item.setCurrentDestination(destination);
        item.setUsageReason(record == null ? null : record.getUsageReason());
        item.setBorrowTime(firstNonBlankTime(record == null ? null : record.getTakeTime(), vehicle.getBorrowTime()));
        item.setExpectedReturnTime(record == null ? null : record.getExpectedReturnTime());

        boolean overdue = record != null
                && record.getExpectedReturnTime() != null
                && record.getExpectedReturnTime().isBefore(LocalDateTime.now());

        item.setActivityType(overdue ? "OVERDUE_RETURN" : "IN_USE");
        item.setPriority(overdue ? 95 : 60);
        item.setActivityTitle(overdue
                ? driverName + " 正在使用，已超过预计还车时间"
                : driverName + " 正在用车" + (destination == null ? "" : "，前往 " + destination));
        item.setActivitySubtitle(overdue
                ? buildOverdueLine(record)
                : firstNonBlank(
                        buildBorrowScheduleLine(item.getBorrowTime(), item.getExpectedReturnTime()),
                        buildDestinationLine(destination),
                        "借车进行中"));
    }

    private void populatePendingFollowUpItem(DriverVehicleActivityItem item, Vehicle vehicle, VehicleBorrowRecord record) {
        item.setAvailableForBorrow(false);
        item.setActivityType("FOLLOW_UP_PENDING");
        item.setPriority(88);
        item.setBorrowRecordId(record == null ? null : record.getId());
        item.setCurrentDriverName(record == null ? null : record.getDriverName());
        item.setCurrentDestination(record == null ? null : record.getDestination());
        item.setUsageReason(record == null ? null : record.getUsageReason());
        item.setBorrowTime(record == null ? null : record.getTakeTime());
        item.setExpectedReturnTime(record == null ? null : record.getExpectedReturnTime());
        item.setFollowUpStatus(record == null ? null : record.getFollowUpStatus());
        item.setActionRequired(firstNonBlank(
                record == null ? null : record.getActionRequired(),
                vehicle.getCleanReason()));
        item.setActivityTitle("本车已归还，等待办公室处理闭环");
        item.setActivitySubtitle(firstNonBlank(
                item.getActionRequired(),
                buildFuelReminderLine(vehicle.getFuelReminderStatus(), vehicle.getFuelReminderNote()),
                "请等待办公室处理异常、清洁或补油事项"));
    }

    private void populateMaintenanceItem(DriverVehicleActivityItem item, Vehicle vehicle, MaintenanceWorkOrder order) {
        item.setAvailableForBorrow(false);
        item.setMaintenanceOrderId(order == null ? null : order.getId());
        item.setMaintenanceStatus(order == null ? null : order.getStatus());
        item.setMaintenanceStatusLabel(resolveMaintenanceStatusLabel(order == null ? null : order.getStatus()));
        item.setMaintenanceIssueDescription(order == null ? null : order.getIssueDescription());
        item.setRepairVendor(order == null ? null : order.getRepairVendor());
        item.setExpectedFinishTime(order == null ? null : order.getExpectedFinishTime());
        item.setRepairStartTime(order == null ? null : order.getRepairStartTime());
        item.setRepairFinishTime(order == null ? null : order.getRepairFinishTime());

        String maintenanceStatus = order == null || order.getStatus() == null ? "IN_REPAIR" : order.getStatus();
        switch (maintenanceStatus) {
            case "PENDING_APPROVAL" -> {
                item.setActivityType("MAINTENANCE_PENDING_APPROVAL");
                item.setPriority(82);
                item.setActivityTitle("维修工单已创建，当前等待审批");
            }
            case "WAIT_ACCEPTANCE" -> {
                item.setActivityType("MAINTENANCE_WAIT_ACCEPTANCE");
                item.setPriority(86);
                item.setActivityTitle("维修已完成，当前等待验收");
            }
            case "APPROVED" -> {
                item.setActivityType("MAINTENANCE_APPROVED");
                item.setPriority(74);
                item.setActivityTitle("维修已批准，等待送修或开工");
            }
            default -> {
                item.setActivityType("MAINTENANCE_IN_PROGRESS");
                item.setPriority(76);
                item.setActivityTitle("车辆正在维修中");
            }
        }

        item.setActivitySubtitle(firstNonBlank(
                buildMaintenanceLine(order),
                firstNonBlank(order == null ? null : order.getIssueDescription(), vehicle.getCleanReason()),
                "维修处理中"));
    }

    private LocalDateTime firstNonBlankTime(LocalDateTime primary, LocalDateTime fallback) {
        return primary != null ? primary : fallback;
    }

    private String resolveVehicleStatusLabel(String status) {
        if ("IN_USE".equals(status)) {
            return "使用中";
        }
        if ("PENDING_CHECK".equals(status)) {
            return "待处理";
        }
        if ("MAINTENANCE".equals(status)) {
            return "维修中";
        }
        return "空闲中";
    }

    private String resolveMaintenanceStatusLabel(String status) {
        if ("PENDING_APPROVAL".equals(status)) {
            return "待审批";
        }
        if ("APPROVED".equals(status)) {
            return "已批准";
        }
        if ("WAIT_ACCEPTANCE".equals(status)) {
            return "待验收";
        }
        if ("IN_REPAIR".equals(status)) {
            return "维修中";
        }
        return status;
    }

    private boolean isRestrictedForBorrow(DriverVehicleActivityItem item) {
        return Integer.valueOf(1).equals(item.getTrafficRestrictedToday())
                && !Integer.valueOf(1).equals(item.getTrafficRestrictionReleasedToday());
    }

    private String buildParkingIdleTitle(String parkingLocation) {
        if (parkingLocation == null || parkingLocation.isBlank()) {
            return null;
        }
        return parkingLocation.trim() + " 停放中，可直接借用";
    }

    private String buildParkingLine(String parkingLocation) {
        if (parkingLocation == null || parkingLocation.isBlank()) {
            return null;
        }
        return "停放位置：" + parkingLocation.trim();
    }

    private String buildDestinationLine(String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        return "当前去向：" + destination.trim();
    }

    private String buildFuelReminderLine(String fuelReminderStatus, String fuelReminderNote) {
        if (!"PENDING".equals(fuelReminderStatus)) {
            return null;
        }
        return firstNonBlank(fuelReminderNote, "上次还车油量不足半箱，当前仍有待补油提醒");
    }

    private String buildBorrowScheduleLine(LocalDateTime borrowTime, LocalDateTime expectedReturnTime) {
        if (borrowTime == null && expectedReturnTime == null) {
            return null;
        }
        if (expectedReturnTime == null) {
            return "取车时间：" + formatDateTimeText(borrowTime);
        }
        return "取车 " + formatDateTimeText(borrowTime) + "，预计还车 " + formatDateTimeText(expectedReturnTime);
    }

    private String buildOverdueLine(VehicleBorrowRecord record) {
        if (record == null || record.getExpectedReturnTime() == null) {
            return "预计还车时间已超过，请联系当前驾驶员确认";
        }
        Duration duration = Duration.between(record.getExpectedReturnTime(), LocalDateTime.now());
        long hours = Math.max(duration.toHours(), 0);
        if (hours < 1) {
            return "已超过预计还车时间，请联系当前驾驶员确认";
        }
        return "预计还车 " + formatDateTimeText(record.getExpectedReturnTime()) + "，当前已超时约 " + hours + " 小时";
    }

    private String buildMaintenanceLine(MaintenanceWorkOrder order) {
        if (order == null) {
            return null;
        }
        String issue = firstNonBlank(order.getIssueDescription());
        String vendor = firstNonBlank(order.getRepairVendor());
        String finish = formatDateTimeText(order.getExpectedFinishTime());
        if (issue == null && vendor == null && finish == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        if (issue != null) {
            builder.append("送修原因：").append(issue);
        }
        if (vendor != null) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append("维修厂：").append(vendor);
        }
        if (finish != null) {
            if (builder.length() > 0) {
                builder.append("；");
            }
            builder.append("预计完成：").append(finish);
        }
        return builder.toString();
    }

    private String formatDateTimeText(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toString().replace("T", " ").substring(0, 16);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
