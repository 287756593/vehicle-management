package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.vehicle.entity.FuelRecord;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.entity.VehicleBorrowRecord;
import com.company.vehicle.mapper.FuelRecordMapper;
import com.company.vehicle.mapper.VehicleBorrowRecordMapper;
import com.company.vehicle.mapper.VehicleMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.PostConstruct;

@Service
public class TodoCenterService {

    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "OFFICE_ADMIN");

    private final VehicleBorrowRecordMapper vehicleBorrowRecordMapper;
    private final FuelRecordMapper fuelRecordMapper;
    private final VehicleMapper vehicleMapper;
    private final JdbcTemplate jdbcTemplate;

    // Schema 检查结果在应用启动时缓存，运行期间表结构不变无需重复查询
    private boolean maintenanceTableExists;
    private boolean borrowHasDeletedColumn;
    private boolean maintenanceHasExpectedFinishTime;

    public TodoCenterService(
            VehicleBorrowRecordMapper vehicleBorrowRecordMapper,
            FuelRecordMapper fuelRecordMapper,
            VehicleMapper vehicleMapper,
            JdbcTemplate jdbcTemplate) {
        this.vehicleBorrowRecordMapper = vehicleBorrowRecordMapper;
        this.fuelRecordMapper = fuelRecordMapper;
        this.vehicleMapper = vehicleMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initSchemaCache() {
        maintenanceTableExists = checkTableExists("maintenance_work_order");
        borrowHasDeletedColumn = checkHasColumn("vehicle_borrow_record", "deleted");
        maintenanceHasExpectedFinishTime = maintenanceTableExists
                && checkHasColumn("maintenance_work_order", "expected_finish_time");
    }

    public Map<String, Object> getSummary(String role) {
        List<Map<String, Object>> items = buildItems(role);
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            String type = String.valueOf(item.get("todoType"));
            typeCounts.put(type, typeCounts.getOrDefault(type, 0L) + 1L);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", items.size());
        summary.put("highPriority", countByPriority(items, "HIGH"));
        summary.put("mediumPriority", countByPriority(items, "MEDIUM"));
        summary.put("lowPriority", countByPriority(items, "LOW"));
        summary.put("typeCounts", typeCounts);
        summary.put("hasMaintenanceSection", maintenanceTableExists);
        return summary;
    }

    public Map<String, Object> getItems(
            String role,
            int current,
            int size,
            String type,
            String priority,
            String keyword,
            Long vehicleId) {
        List<Map<String, Object>> items = buildItems(role);
        List<Map<String, Object>> filtered = items.stream()
                .filter(item -> matchesType(item, type))
                .filter(item -> matchesPriority(item, priority))
                .filter(item -> matchesVehicle(item, vehicleId))
                .filter(item -> matchesKeyword(item, keyword))
                .sorted(todoComparator())
                .toList();

        int safeCurrent = Math.max(current, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safeCurrent - 1) * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        long total = filtered.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", filtered.subList(fromIndex, toIndex));
        result.put("total", total);
        result.put("pages", pages);
        result.put("current", safeCurrent);
        return result;
    }

    private List<Map<String, Object>> buildItems(String role) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (ADMIN_ROLES.contains(role)) {
            items.addAll(buildBorrowFollowUpItems());
            items.addAll(buildBorrowOverdueItems());
            items.addAll(buildVehicleFuelReminderItems());
            items.addAll(buildVehicleExpirationItems());
            items.addAll(buildVehicleRestrictionReleaseItems());
        }
        if (ADMIN_ROLES.contains(role) || "FINANCE".equals(role)) {
            items.addAll(buildFuelApprovalItems());
        }
        if (ADMIN_ROLES.contains(role)) {
            items.addAll(buildMaintenanceItems());
        }
        return items;
    }

    private List<Map<String, Object>> buildBorrowFollowUpItems() {
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleBorrowRecord::getFollowUpStatus, "PENDING");
        if (borrowHasDeletedColumn) {
            wrapper.and(query -> query.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0));
        }
        List<VehicleBorrowRecord> records = vehicleBorrowRecordMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (VehicleBorrowRecord record : records) {
            items.add(buildItem(
                    todoKey("BORROW_FOLLOW_UP", record.getId()),
                    "BORROW_FOLLOW_UP",
                    record.getPlateNumber() + " 借还车闭环待处理",
                    record.getActionRequired() != null && !record.getActionRequired().isBlank()
                            ? record.getActionRequired()
                            : "当前记录仍有待闭环事项",
                    "HIGH",
                    record.getReturnTime() != null ? record.getReturnTime() : record.getUpdateTime(),
                    "VEHICLE_BORROW_RECORD",
                    record.getId(),
                    record.getVehicleId(),
                    record.getPlateNumber(),
                    "/borrow-records",
                    Map.of(
                            "followUpStatus", "PENDING",
                            "highlightId", String.valueOf(record.getId())
                    )
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildBorrowOverdueItems() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<VehicleBorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VehicleBorrowRecord::getStatus, "TAKEN")
                .isNotNull(VehicleBorrowRecord::getExpectedReturnTime)
                .lt(VehicleBorrowRecord::getExpectedReturnTime, now);
        if (borrowHasDeletedColumn) {
            wrapper.and(query -> query.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0));
        }
        List<VehicleBorrowRecord> records = vehicleBorrowRecordMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (VehicleBorrowRecord record : records) {
            items.add(buildItem(
                    todoKey("BORROW_OVERDUE", record.getId()),
                    "BORROW_OVERDUE",
                    record.getPlateNumber() + " 超期未还",
                    (record.getDriverName() != null ? record.getDriverName() : "驾驶员") + " 当前仍在使用该车辆",
                    "HIGH",
                    record.getExpectedReturnTime(),
                    "VEHICLE_BORROW_RECORD",
                    record.getId(),
                    record.getVehicleId(),
                    record.getPlateNumber(),
                    "/borrow-records",
                    Map.of(
                            "status", "TAKEN",
                            "highlightId", String.valueOf(record.getId())
                    )
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildFuelApprovalItems() {
        LambdaQueryWrapper<FuelRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FuelRecord::getCashReportStatus, "PENDING")
                .and(query -> query.isNull(FuelRecord::getDeleted).or().eq(FuelRecord::getDeleted, 0));
        List<FuelRecord> records = fuelRecordMapper.selectList(wrapper);
        List<Map<String, Object>> items = new ArrayList<>();
        for (FuelRecord record : records) {
            items.add(buildItem(
                    todoKey("FUEL_APPROVAL", record.getId()),
                    "FUEL_APPROVAL",
                    record.getPlateNumber() + " 现金加油待审批",
                    "当前记录需要办公室审批后才会纳入完整流转",
                    "HIGH",
                    record.getFuelDate(),
                    "FUEL_RECORD",
                    record.getId(),
                    record.getVehicleId(),
                    record.getPlateNumber(),
                    "/fuel-records",
                    Map.of(
                            "status", "PENDING",
                            "vehicleId", record.getVehicleId() == null ? "" : String.valueOf(record.getVehicleId())
                    )
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildVehicleFuelReminderItems() {
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getFuelReminderStatus, "PENDING"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            items.add(buildItem(
                    todoKey("VEHICLE_FUEL_REMINDER", vehicle.getId()),
                    "VEHICLE_FUEL_REMINDER",
                    vehicle.getPlateNumber() + " 待补油",
                    vehicle.getFuelReminderNote() != null && !vehicle.getFuelReminderNote().isBlank()
                            ? vehicle.getFuelReminderNote()
                            : "车辆当前存在待补油提醒",
                    "MEDIUM",
                    vehicle.getFuelReminderTime(),
                    "VEHICLE",
                    vehicle.getId(),
                    vehicle.getId(),
                    vehicle.getPlateNumber(),
                    "/vehicles",
                    Map.of(
                            "status", "",
                            "highlightVehicleId", String.valueOf(vehicle.getId())
                    )
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildVehicleExpirationItems() {
        LocalDate today = LocalDate.now();
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<>());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            long insuranceDays = daysUntil(today, vehicle.getInsuranceExpireDate());
            if (insuranceDays >= 0 && insuranceDays <= 30) {
                items.add(buildItem(
                        todoKey("VEHICLE_INSURANCE_EXPIRING", vehicle.getId()),
                        "VEHICLE_INSURANCE_EXPIRING",
                        vehicle.getPlateNumber() + " 保险即将到期",
                        insuranceDays == 0 ? "保险今天到期" : "保险还有 " + insuranceDays + " 天到期",
                        insuranceDays <= 7 ? "HIGH" : "MEDIUM",
                        vehicle.getInsuranceExpireDate() == null ? null : vehicle.getInsuranceExpireDate().atStartOfDay(),
                        "VEHICLE",
                        vehicle.getId(),
                        vehicle.getId(),
                        vehicle.getPlateNumber(),
                        "/vehicles",
                        Map.of("highlightVehicleId", String.valueOf(vehicle.getId()))
                ));
            }

            long inspectionDays = daysUntil(today, vehicle.getInspectionExpireDate());
            if (inspectionDays >= 0 && inspectionDays <= 30) {
                items.add(buildItem(
                        todoKey("VEHICLE_INSPECTION_EXPIRING", vehicle.getId()),
                        "VEHICLE_INSPECTION_EXPIRING",
                        vehicle.getPlateNumber() + " 年检即将到期",
                        inspectionDays == 0 ? "年检今天到期" : "年检还有 " + inspectionDays + " 天到期",
                        inspectionDays <= 7 ? "HIGH" : "MEDIUM",
                        vehicle.getInspectionExpireDate() == null ? null : vehicle.getInspectionExpireDate().atStartOfDay(),
                        "VEHICLE",
                        vehicle.getId(),
                        vehicle.getId(),
                        vehicle.getPlateNumber(),
                        "/vehicles",
                        Map.of("highlightVehicleId", String.valueOf(vehicle.getId()))
                ));
            }
        }
        return items;
    }

    private List<Map<String, Object>> buildVehicleRestrictionReleaseItems() {
        LocalDate today = LocalDate.now();
        List<Vehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getTrafficRestrictionReleaseDate, today));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if (!isRestrictedToday(vehicle.getPlateNumber(), today)) {
                continue;
            }
            items.add(buildItem(
                    todoKey("VEHICLE_RESTRICTION_RELEASED", vehicle.getId()),
                    "VEHICLE_RESTRICTION_RELEASED",
                    vehicle.getPlateNumber() + " 今日限行已放行",
                    "该车辆今日本应限行，但已被管理员手动放行",
                    "LOW",
                    today.atStartOfDay(),
                    "VEHICLE",
                    vehicle.getId(),
                    vehicle.getId(),
                    vehicle.getPlateNumber(),
                    "/vehicles",
                    Map.of("highlightVehicleId", String.valueOf(vehicle.getId()))
            ));
        }
        return items;
    }

    private List<Map<String, Object>> buildMaintenanceItems() {
        if (!maintenanceTableExists) {
            return List.of();
        }

        boolean hasExpectedFinishTime = maintenanceHasExpectedFinishTime;
        String dueColumn = hasExpectedFinishTime ? "expected_finish_time" : "repair_finish_time";
        String selectSql = "SELECT mwo.id, mwo.order_no, mwo.vehicle_id, mwo.status, "
                + "COALESCE(NULLIF(mwo.plate_number_snapshot, ''), v.plate_number, '未知车辆') AS plate_number, "
                + "mwo." + dueColumn + " AS due_time, mwo.create_time "
                + "FROM maintenance_work_order mwo LEFT JOIN vehicle v ON v.id = mwo.vehicle_id ";
        List<Map<String, Object>> items = new ArrayList<>();
        items.addAll(queryMaintenanceItems(
                selectSql + "WHERE status = 'PENDING_APPROVAL'",
                "MAINTENANCE_PENDING_APPROVAL",
                "HIGH",
                "维修工单待审批",
                "当前工单需要进入审批流"
        ));
        items.addAll(queryMaintenanceItems(
                selectSql + "WHERE status = 'WAIT_ACCEPTANCE'",
                "MAINTENANCE_WAIT_ACCEPTANCE",
                "HIGH",
                "维修工单待验收",
                "当前工单已完工，待办公室验收"
        ));
        if (hasExpectedFinishTime) {
            items.addAll(queryMaintenanceItems(
                    selectSql
                            + "WHERE status IN ('PENDING_APPROVAL', 'APPROVED', 'IN_REPAIR', 'WAIT_ACCEPTANCE') "
                            + "AND expected_finish_time IS NOT NULL AND expected_finish_time < NOW()",
                    "MAINTENANCE_OVERDUE",
                    "HIGH",
                    "维修工单已超期",
                    "当前工单已超过预计完工时间"
            ));
        }
        return items;
    }

    private List<Map<String, Object>> queryMaintenanceItems(
            String sql,
            String todoType,
            String priority,
            String titleSuffix,
            String description) {
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Long vehicleId = rs.getLong("vehicle_id");
                String plateNumber = rs.getString("plate_number"); // 已由 SQL JOIN 解析
                LocalDateTime dueTime = null;
                try {
                    dueTime = rs.getTimestamp("due_time") == null
                            ? null
                            : rs.getTimestamp("due_time").toLocalDateTime();
                } catch (Exception ignored) {
                }
                LocalDateTime createTime = rs.getTimestamp("create_time") == null
                        ? null
                        : rs.getTimestamp("create_time").toLocalDateTime();
                return buildItem(
                        todoKey(todoType, rs.getLong("id")),
                        todoType,
                        plateNumber + " " + titleSuffix,
                        description + "（工单号 " + rs.getString("order_no") + "）",
                        priority,
                        dueTime != null ? dueTime : createTime,
                        "MAINTENANCE_WORK_ORDER",
                        rs.getLong("id"),
                        vehicleId,
                        plateNumber,
                        "/maintenance-work-orders",
                        Map.of(
                                "status", rs.getString("status"),
                                "highlightOrderId", String.valueOf(rs.getLong("id"))
                        )
                );
            });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> buildItem(
            String todoKey,
            String todoType,
            String title,
            String description,
            String priority,
            LocalDateTime dueTime,
            String sourceType,
            Long sourceId,
            Long vehicleId,
            String plateNumber,
            String actionRoute,
            Map<String, String> actionQuery) {
        Map<String, Object> item = new HashMap<>();
        item.put("todoKey", todoKey);
        item.put("todoType", todoType);
        item.put("title", title);
        item.put("description", description);
        item.put("priority", priority);
        item.put("dueTime", dueTime);
        item.put("sourceType", sourceType);
        item.put("sourceId", sourceId);
        item.put("vehicleId", vehicleId);
        item.put("plateNumber", plateNumber);
        item.put("actionRoute", actionRoute);
        item.put("actionQuery", actionQuery);
        item.put("priorityWeight", priorityWeight(priority));
        item.put("sortTime", dueTime == null ? 0L : dueTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return item;
    }

    private boolean matchesType(Map<String, Object> item, String type) {
        return type == null || type.isBlank() || Objects.equals(item.get("todoType"), type.trim());
    }

    private boolean matchesPriority(Map<String, Object> item, String priority) {
        return priority == null || priority.isBlank() || Objects.equals(item.get("priority"), priority.trim());
    }

    private boolean matchesVehicle(Map<String, Object> item, Long vehicleId) {
        return vehicleId == null || Objects.equals(item.get("vehicleId"), vehicleId);
    }

    private boolean matchesKeyword(Map<String, Object> item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return containsText(item.get("title"), normalized)
                || containsText(item.get("description"), normalized)
                || containsText(item.get("plateNumber"), normalized);
    }

    private boolean containsText(Object value, String normalizedKeyword) {
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private Comparator<Map<String, Object>> todoComparator() {
        return Comparator
                .comparingInt((Map<String, Object> item) -> (Integer) item.getOrDefault("priorityWeight", 0))
                .reversed()
                .thenComparingLong(item -> (Long) item.getOrDefault("sortTime", 0L))
                .thenComparing(item -> String.valueOf(item.getOrDefault("title", "")));
    }

    private long countByPriority(Collection<Map<String, Object>> items, String priority) {
        return items.stream()
                .filter(item -> Objects.equals(item.get("priority"), priority))
                .count();
    }

    private String todoKey(String todoType, Long id) {
        return todoType + ":" + id;
    }

    private int priorityWeight(String priority) {
        return switch (priority) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private long daysUntil(LocalDate today, LocalDate target) {
        if (target == null) {
            return -1;
        }
        return target.toEpochDay() - today.toEpochDay();
    }

    private boolean checkTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean checkHasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean isRestrictedToday(String plateNumber, LocalDate today) {
        if (plateNumber == null || plateNumber.isBlank() || today == null) {
            return false;
        }
        return switch (today.getDayOfWeek()) {
            case MONDAY -> lastPlateDigit(plateNumber) == 1 || lastPlateDigit(plateNumber) == 6;
            case TUESDAY -> lastPlateDigit(plateNumber) == 2 || lastPlateDigit(plateNumber) == 7;
            case WEDNESDAY -> lastPlateDigit(plateNumber) == 3 || lastPlateDigit(plateNumber) == 8;
            case THURSDAY -> lastPlateDigit(plateNumber) == 4 || lastPlateDigit(plateNumber) == 9;
            case FRIDAY -> lastPlateDigit(plateNumber) == 5 || lastPlateDigit(plateNumber) == 0;
            default -> false;
        };
    }

    private int lastPlateDigit(String plateNumber) {
        for (int index = plateNumber.length() - 1; index >= 0; index--) {
            char current = plateNumber.charAt(index);
            if (Character.isDigit(current)) {
                return current - '0';
            }
        }
        return -1;
    }
}
