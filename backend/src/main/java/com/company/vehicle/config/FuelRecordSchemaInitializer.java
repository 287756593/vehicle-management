package com.company.vehicle.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FuelRecordSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public FuelRecordSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("fuel_record", "deleted", "TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是'");
        addColumnIfMissing("fuel_record", "cash_reason", "VARCHAR(500) COMMENT '加油说明'");
        addColumnIfMissing("fuel_record", "leader_approval_photo", "VARCHAR(500) COMMENT '领导同意截图'");
        addColumnIfMissing("fuel_record", "fuel_gauge_photo", "VARCHAR(500) COMMENT '加油后油表照片'");
        addColumnIfMissing("fuel_record", "is_fuel_enough_after_fuel", "TINYINT DEFAULT 1 COMMENT '加油后是否不少于半箱: 1-是, 0-否'");
        addColumnIfMissing("fuel_record", "budget_year", "INT COMMENT '预算年度'");
        addColumnIfMissing("fuel_record", "fuel_mileage", "DECIMAL(10,2) COMMENT '加油时公里数'");
        addColumnIfMissing("fuel_record", "reimbursement_status", "VARCHAR(20) DEFAULT 'UNREIMBURSED' COMMENT '报销状态: UNREIMBURSED-未报销, REIMBURSED-已报销'");
        addColumnIfMissing("fuel_record", "reimbursed_time", "DATETIME COMMENT '报销时间'");
        addColumnIfMissing("vehicle", "fuel_reminder_status", "VARCHAR(20) DEFAULT 'NONE' COMMENT '补油提醒状态: NONE-无, PENDING-待补油, COMPLETED-已完成'");
        addColumnIfMissing("vehicle", "fuel_reminder_note", "VARCHAR(255) COMMENT '补油提醒说明'");
        addColumnIfMissing("vehicle", "fuel_reminder_time", "DATETIME COMMENT '补油提醒时间'");
        addColumnIfMissing("vehicle", "current_destination", "VARCHAR(255) COMMENT '当前借用去向'");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                table,
                column
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
