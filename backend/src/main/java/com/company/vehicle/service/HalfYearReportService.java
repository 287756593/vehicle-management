package com.company.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.vehicle.entity.FuelRecord;
import com.company.vehicle.entity.Vehicle;
import com.company.vehicle.entity.VehicleBorrowRecord;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HalfYearReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VehicleService vehicleService;
    private final VehicleBorrowRecordService vehicleBorrowRecordService;
    private final FuelRecordService fuelRecordService;

    public HalfYearReportService(
            VehicleService vehicleService,
            VehicleBorrowRecordService vehicleBorrowRecordService,
            FuelRecordService fuelRecordService) {
        this.vehicleService = vehicleService;
        this.vehicleBorrowRecordService = vehicleBorrowRecordService;
        this.fuelRecordService = fuelRecordService;
    }

    public Map<String, Object> buildHalfYearReport(String endMonthValue) {
        YearMonth endMonth = parseEndMonth(endMonthValue);
        YearMonth startMonth = endMonth.minusMonths(5);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = startMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = endMonth.equals(YearMonth.from(now))
                ? now
                : endMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Vehicle> vehicles = vehicleService.list();
        List<VehicleBorrowRecord> borrowRecords = vehicleBorrowRecordService.list(new LambdaQueryWrapper<VehicleBorrowRecord>()
                .and(wrapper -> wrapper.isNull(VehicleBorrowRecord::getDeleted).or().eq(VehicleBorrowRecord::getDeleted, 0))
                .ge(VehicleBorrowRecord::getTakeTime, startTime)
                .le(VehicleBorrowRecord::getTakeTime, endTime)
                .orderByAsc(VehicleBorrowRecord::getTakeTime));

        List<FuelRecord> fuelRecords = fuelRecordService.list(new LambdaQueryWrapper<FuelRecord>()
                .and(wrapper -> wrapper.isNull(FuelRecord::getDeleted).or().eq(FuelRecord::getDeleted, 0))
                .ge(FuelRecord::getFuelDate, startTime)
                .le(FuelRecord::getFuelDate, endTime)
                .orderByAsc(FuelRecord::getFuelDate));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", buildPeriod(startMonth, endMonth, startTime, endTime));
        report.put("overview", buildOverview(borrowRecords, fuelRecords));
        report.put("monthlyTrend", buildMonthlyTrend(startMonth, endMonth, borrowRecords, fuelRecords));
        List<Map<String, Object>> vehicleSummaries = buildVehicleSummaries(vehicles, borrowRecords, fuelRecords);
        report.put("vehicleSummaries", vehicleSummaries);
        report.put("topDrivers", buildTopDrivers(borrowRecords));
        report.put("topDestinations", buildTopDestinations(borrowRecords));
        report.put("insights", buildInsights(vehicleSummaries, borrowRecords, fuelRecords));
        report.put("generatedAt", DATE_TIME_FORMATTER.format(now));
        return report;
    }

    private Map<String, Object> buildPeriod(
            YearMonth startMonth,
            YearMonth endMonth,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        Map<String, Object> period = new LinkedHashMap<>();
        period.put("startMonth", startMonth.format(MONTH_FORMATTER));
        period.put("endMonth", endMonth.format(MONTH_FORMATTER));
        period.put("startDate", startTime.toLocalDate().toString());
        period.put("endDate", endTime.toLocalDate().toString());
        period.put("label", startMonth.format(MONTH_FORMATTER) + " 至 " + endMonth.format(MONTH_FORMATTER));
        return period;
    }

    private Map<String, Object> buildOverview(List<VehicleBorrowRecord> borrowRecords, List<FuelRecord> fuelRecords) {
        List<VehicleBorrowRecord> completedBorrowRecords = borrowRecords.stream()
                .filter(record -> "RETURNED".equals(record.getStatus()))
                .toList();
        BigDecimal totalMileage = sumBigDecimal(completedBorrowRecords, this::calculateTripMileage);
        BigDecimal totalFuelAmount = sumBigDecimal(fuelRecords, FuelRecord::getFuelAmount);
        BigDecimal totalFuelCost = sumBigDecimal(fuelRecords, FuelRecord::getTotalAmount);
        long cashFuelCount = fuelRecords.stream().filter(record -> Integer.valueOf(1).equals(record.getIsCash())).count();
        long pendingFollowUpCount = borrowRecords.stream()
                .filter(record -> "PENDING".equals(record.getFollowUpStatus()))
                .count();
        long issueCount = borrowRecords.stream()
                .filter(record -> isNotBlank(record.getIssueDescription()))
                .count();
        long uniqueVehicles = borrowRecords.stream()
                .map(VehicleBorrowRecord::getVehicleId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long uniqueDrivers = borrowRecords.stream()
                .map(VehicleBorrowRecord::getDriverId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal avgTripMileage = completedBorrowRecords.isEmpty()
                ? BigDecimal.ZERO
                : safeDivide(totalMileage, BigDecimal.valueOf(completedBorrowRecords.size()), 1);

        List<Duration> durations = completedBorrowRecords.stream()
                .filter(record -> record.getTakeTime() != null && record.getReturnTime() != null)
                .map(record -> Duration.between(record.getTakeTime(), record.getReturnTime()))
                .filter(duration -> !duration.isNegative())
                .toList();
        BigDecimal avgDurationHours = durations.isEmpty()
                ? BigDecimal.ZERO
                : safeDivide(
                durations.stream()
                        .map(duration -> BigDecimal.valueOf(duration.toMinutes()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                BigDecimal.valueOf(durations.size() * 60L),
                1);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalTrips", borrowRecords.size());
        overview.put("returnedTrips", completedBorrowRecords.size());
        overview.put("activeTrips", borrowRecords.stream().filter(record -> "TAKEN".equals(record.getStatus())).count());
        overview.put("uniqueVehicles", uniqueVehicles);
        overview.put("uniqueDrivers", uniqueDrivers);
        overview.put("pendingFollowUpCount", pendingFollowUpCount);
        overview.put("issueCount", issueCount);
        overview.put("totalMileage", totalMileage);
        overview.put("avgTripMileage", avgTripMileage);
        overview.put("avgTripDurationHours", avgDurationHours);
        overview.put("fuelRecordCount", fuelRecords.size());
        overview.put("cashFuelCount", cashFuelCount);
        overview.put("totalFuelAmount", totalFuelAmount);
        overview.put("totalFuelCost", totalFuelCost);
        overview.put("avgFuelCostPerRecord", fuelRecords.isEmpty()
                ? BigDecimal.ZERO
                : safeDivide(totalFuelCost, BigDecimal.valueOf(fuelRecords.size()), 2));
        overview.put("avgFuelPrice", totalFuelAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : safeDivide(totalFuelCost, totalFuelAmount, 2));
        return overview;
    }

    private List<Map<String, Object>> buildMonthlyTrend(
            YearMonth startMonth,
            YearMonth endMonth,
            List<VehicleBorrowRecord> borrowRecords,
            List<FuelRecord> fuelRecords) {
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            YearMonth currentMonth = cursor;
            List<VehicleBorrowRecord> monthlyBorrows = borrowRecords.stream()
                    .filter(record -> record.getTakeTime() != null && YearMonth.from(record.getTakeTime()).equals(currentMonth))
                    .toList();
            List<FuelRecord> monthlyFuelRecords = fuelRecords.stream()
                    .filter(record -> record.getFuelDate() != null && YearMonth.from(record.getFuelDate()).equals(currentMonth))
                    .toList();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("month", currentMonth.format(MONTH_FORMATTER));
            item.put("tripCount", monthlyBorrows.size());
            item.put("vehicleCount", monthlyBorrows.stream()
                    .map(VehicleBorrowRecord::getVehicleId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count());
            item.put("mileage", sumBigDecimal(monthlyBorrows, this::calculateTripMileage));
            item.put("fuelVolume", sumBigDecimal(monthlyFuelRecords, FuelRecord::getFuelAmount));
            item.put("fuelCost", sumBigDecimal(monthlyFuelRecords, FuelRecord::getTotalAmount));
            item.put("issueCount", monthlyBorrows.stream().filter(record -> isNotBlank(record.getIssueDescription())).count());
            item.put("vehicleDetails", buildMonthlyVehicleDetails(monthlyBorrows, monthlyFuelRecords));
            trend.add(item);
            cursor = cursor.plusMonths(1);
        }
        return trend;
    }

    private List<Map<String, Object>> buildMonthlyVehicleDetails(
            List<VehicleBorrowRecord> monthlyBorrows,
            List<FuelRecord> monthlyFuelRecords) {
        Map<String, List<VehicleBorrowRecord>> borrowGroups = monthlyBorrows.stream()
                .collect(Collectors.groupingBy(
                        record -> buildVehicleGroupKey(record.getVehicleId(), record.getPlateNumber()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<String, List<FuelRecord>> fuelGroups = monthlyFuelRecords.stream()
                .collect(Collectors.groupingBy(
                        record -> buildVehicleGroupKey(record.getVehicleId(), record.getPlateNumber()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<String, Boolean> keys = new LinkedHashMap<>();
        borrowGroups.keySet().forEach(key -> keys.put(key, Boolean.TRUE));
        fuelGroups.keySet().forEach(key -> keys.put(key, Boolean.TRUE));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (String key : keys.keySet()) {
            List<VehicleBorrowRecord> vehicleBorrows = borrowGroups.getOrDefault(key, List.of());
            List<FuelRecord> vehicleFuelRecords = fuelGroups.getOrDefault(key, List.of());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", resolveVehicleId(vehicleBorrows, vehicleFuelRecords));
            row.put("plateNumber", resolveVehiclePlateNumber(vehicleBorrows, vehicleFuelRecords));
            row.put("tripCount", vehicleBorrows.size());
            row.put("mileage", sumBigDecimal(vehicleBorrows, this::calculateTripMileage));
            row.put("fuelVolume", sumBigDecimal(vehicleFuelRecords, FuelRecord::getFuelAmount));
            row.put("fuelCost", sumBigDecimal(vehicleFuelRecords, FuelRecord::getTotalAmount));
            rows.add(row);
        }

        return rows.stream()
                .sorted(Comparator
                        .comparing((Map<String, Object> item) -> ((Number) item.get("tripCount")).longValue())
                        .thenComparing(item -> (BigDecimal) item.get("mileage"))
                        .thenComparing(item -> (BigDecimal) item.get("fuelCost"))
                        .reversed()
                        .thenComparing(item -> String.valueOf(item.get("plateNumber"))))
                .toList();
    }

    private List<Map<String, Object>> buildVehicleSummaries(
            List<Vehicle> vehicles,
            List<VehicleBorrowRecord> borrowRecords,
            List<FuelRecord> fuelRecords) {
        Map<Long, List<VehicleBorrowRecord>> borrowByVehicle = borrowRecords.stream()
                .filter(r -> r.getVehicleId() != null)
                .collect(Collectors.groupingBy(VehicleBorrowRecord::getVehicleId));
        Map<Long, List<FuelRecord>> fuelByVehicle = fuelRecords.stream()
                .filter(r -> r.getVehicleId() != null)
                .collect(Collectors.groupingBy(FuelRecord::getVehicleId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            List<VehicleBorrowRecord> vehicleBorrowRecords = borrowByVehicle.getOrDefault(vehicle.getId(), List.of());
            List<FuelRecord> vehicleFuelRecords = fuelByVehicle.getOrDefault(vehicle.getId(), List.of());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("vehicleId", vehicle.getId());
            row.put("plateNumber", defaultString(vehicle.getPlateNumber(), "-"));
            row.put("model", joinVehicleModel(vehicle));
            row.put("status", defaultString(vehicle.getStatus(), "-"));
            row.put("currentMileage", defaultBigDecimal(vehicle.getCurrentMileage()));
            row.put("tripCount", vehicleBorrowRecords.size());
            row.put("returnedTripCount", vehicleBorrowRecords.stream().filter(record -> "RETURNED".equals(record.getStatus())).count());
            row.put("totalMileage", sumBigDecimal(vehicleBorrowRecords, this::calculateTripMileage));
            row.put("fuelCost", sumBigDecimal(vehicleFuelRecords, FuelRecord::getTotalAmount));
            row.put("fuelVolume", sumBigDecimal(vehicleFuelRecords, FuelRecord::getFuelAmount));
            row.put("cashFuelCost", vehicleFuelRecords.stream()
                    .filter(record -> Integer.valueOf(1).equals(record.getIsCash()))
                    .map(record -> defaultBigDecimal(record.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            row.put("latestDriver", resolveLatestDriverName(vehicleBorrowRecords));
            row.put("latestDestination", resolveLatestDestination(vehicleBorrowRecords));
            rows.add(row);
        }

        return rows.stream()
                .sorted(Comparator
                        .comparing((Map<String, Object> item) -> ((Number) item.get("tripCount")).longValue())
                        .thenComparing(item -> (BigDecimal) item.get("totalMileage"))
                        .reversed()
                        .thenComparing(item -> String.valueOf(item.get("plateNumber"))))
                .toList();
    }

    private List<Map<String, Object>> buildTopDrivers(List<VehicleBorrowRecord> borrowRecords) {
        Map<String, List<VehicleBorrowRecord>> grouped = borrowRecords.stream()
                .filter(record -> isNotBlank(record.getDriverName()))
                .collect(Collectors.groupingBy(VehicleBorrowRecord::getDriverName, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("driverName", entry.getKey());
                    item.put("tripCount", entry.getValue().size());
                    item.put("mileage", sumBigDecimal(entry.getValue(), this::calculateTripMileage));
                    return item;
                })
                .sorted(Comparator
                        .comparing((Map<String, Object> item) -> (BigDecimal) item.get("mileage"))
                        .thenComparing(item -> (Integer) item.get("tripCount"))
                        .reversed())
                .limit(5)
                .toList();
    }

    private List<Map<String, Object>> buildTopDestinations(List<VehicleBorrowRecord> borrowRecords) {
        Map<String, Long> destinationCount = borrowRecords.stream()
                .map(VehicleBorrowRecord::getDestination)
                .filter(this::isNotBlank)
                .collect(Collectors.groupingBy(this::normalizeText, LinkedHashMap::new, Collectors.counting()));

        return destinationCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("destination", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

    private List<String> buildInsights(
            List<Map<String, Object>> vehicleSummaries,
            List<VehicleBorrowRecord> borrowRecords,
            List<FuelRecord> fuelRecords) {
        List<String> insights = new ArrayList<>();
        BigDecimal totalMileage = sumBigDecimal(borrowRecords, this::calculateTripMileage);
        BigDecimal totalFuelCost = sumBigDecimal(fuelRecords, FuelRecord::getTotalAmount);
        insights.add("近 6 个月共完成 " + borrowRecords.size() + " 次用车，累计行驶 " + formatDecimal(totalMileage, 1) + " 公里。");
        insights.add("同期共登记 " + fuelRecords.size() + " 条加油记录，累计金额 " + formatCurrency(totalFuelCost) + "。");

        vehicleSummaries.stream().findFirst().ifPresent(topVehicle -> {
            insights.add("使用频次最高的车辆是 " + topVehicle.get("plateNumber")
                    + "，半年内出车 " + topVehicle.get("tripCount")
                    + " 次，累计行驶 " + formatDecimal((BigDecimal) topVehicle.get("totalMileage"), 1) + " 公里。");
        });

        long pendingFollowUpCount = borrowRecords.stream().filter(record -> "PENDING".equals(record.getFollowUpStatus())).count();
        if (pendingFollowUpCount > 0) {
            insights.add("当前仍有 " + pendingFollowUpCount + " 条借还车闭环事项未完成，建议优先核查异常与补油。");
        }

        long cashFuelCount = fuelRecords.stream().filter(record -> Integer.valueOf(1).equals(record.getIsCash())).count();
        if (cashFuelCount > 0) {
            insights.add("现金加油记录共 " + cashFuelCount + " 条，建议重点关注审批与报销闭环。");
        }

        if (insights.size() < 4) {
            insights.add("建议结合车辆当前总里程、半年出车次数和加油金额，定期识别高频车辆的保养窗口。");
        }
        return insights;
    }

    private BigDecimal calculateTripMileage(VehicleBorrowRecord record) {
        if (record == null || record.getTakeMileage() == null || record.getReturnMileage() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal mileage = record.getReturnMileage().subtract(record.getTakeMileage());
        return mileage.compareTo(BigDecimal.ZERO) > 0 ? mileage : BigDecimal.ZERO;
    }

    private BigDecimal sumBigDecimal(Collection<?> items, Function<Object, BigDecimal> mapper) {
        BigDecimal total = BigDecimal.ZERO;
        for (Object item : items) {
            BigDecimal value = mapper.apply(item);
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private <T> BigDecimal sumBigDecimal(List<T> items, Function<T, BigDecimal> mapper) {
        return sumBigDecimal((Collection<?>) items, item -> mapper.apply((T) item));
    }

    private String resolveLatestDriverName(List<VehicleBorrowRecord> vehicleBorrowRecords) {
        return vehicleBorrowRecords.stream()
                .filter(record -> record.getTakeTime() != null)
                .max(Comparator.comparing(VehicleBorrowRecord::getTakeTime))
                .map(VehicleBorrowRecord::getDriverName)
                .filter(this::isNotBlank)
                .orElse("-");
    }

    private String resolveLatestDestination(List<VehicleBorrowRecord> vehicleBorrowRecords) {
        return vehicleBorrowRecords.stream()
                .filter(record -> record.getTakeTime() != null)
                .max(Comparator.comparing(VehicleBorrowRecord::getTakeTime))
                .map(VehicleBorrowRecord::getDestination)
                .filter(this::isNotBlank)
                .orElse("-");
    }

    private Long resolveVehicleId(List<VehicleBorrowRecord> vehicleBorrowRecords, List<FuelRecord> fuelRecords) {
        return vehicleBorrowRecords.stream()
                .map(VehicleBorrowRecord::getVehicleId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> fuelRecords.stream()
                        .map(FuelRecord::getVehicleId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    private String resolveVehiclePlateNumber(List<VehicleBorrowRecord> vehicleBorrowRecords, List<FuelRecord> fuelRecords) {
        return vehicleBorrowRecords.stream()
                .map(VehicleBorrowRecord::getPlateNumber)
                .filter(this::isNotBlank)
                .findFirst()
                .orElseGet(() -> fuelRecords.stream()
                        .map(FuelRecord::getPlateNumber)
                        .filter(this::isNotBlank)
                        .findFirst()
                        .orElse("-"));
    }

    private String buildVehicleGroupKey(Long vehicleId, String plateNumber) {
        if (vehicleId != null) {
            return "ID:" + vehicleId;
        }
        if (isNotBlank(plateNumber)) {
            return "PLATE:" + normalizeText(plateNumber);
        }
        return "UNKNOWN";
    }

    private String joinVehicleModel(Vehicle vehicle) {
        String brand = defaultString(vehicle.getBrand(), "");
        String model = defaultString(vehicle.getModel(), "");
        String combined = (brand + " " + model).trim();
        return combined.isBlank() ? "-" : combined;
    }

    private YearMonth parseEndMonth(String endMonthValue) {
        if (endMonthValue == null || endMonthValue.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(endMonthValue.trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("统计月份格式不正确，应为 yyyy-MM");
        }
    }

    private BigDecimal safeDivide(BigDecimal left, BigDecimal right, int scale) {
        if (left == null || right == null || right.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return left.divide(right, scale, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String formatDecimal(BigDecimal value, int scale) {
        return defaultBigDecimal(value).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatCurrency(BigDecimal value) {
        return formatDecimal(value, 2) + " 元";
    }
}
