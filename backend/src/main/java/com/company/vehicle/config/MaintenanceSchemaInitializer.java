package com.company.vehicle.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public MaintenanceSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createWorkOrderTableIfMissing();
        ensureWorkOrderColumns();
        createAttachmentTableIfMissing();
    }

    private void createWorkOrderTableIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'maintenance_work_order'",
                Integer.class);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute("""
                CREATE TABLE maintenance_work_order (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_no VARCHAR(50) NOT NULL UNIQUE,
                    vehicle_id BIGINT NOT NULL,
                    plate_number_snapshot VARCHAR(20),
                    source_type VARCHAR(50),
                    source_record_id BIGINT,
                    work_type VARCHAR(50),
                    status VARCHAR(30) NOT NULL,
                    issue_description VARCHAR(1000),
                    issue_photos TEXT,
                    reported_by BIGINT,
                    reported_time DATETIME,
                    repair_vendor VARCHAR(100),
                    repair_contact VARCHAR(100),
                    estimated_cost DECIMAL(10,2),
                    actual_cost DECIMAL(10,2),
                    planned_start_time DATETIME,
                    expected_finish_time DATETIME,
                    repair_start_time DATETIME,
                    repair_finish_time DATETIME,
                    accepted_by BIGINT,
                    accepted_time DATETIME,
                    acceptance_result VARCHAR(500),
                    close_result_status VARCHAR(50),
                    remark VARCHAR(1000),
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_vehicle (vehicle_id),
                    INDEX idx_status (status),
                    INDEX idx_source (source_type, source_record_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);
    }

    private void ensureWorkOrderColumns() {
        addColumnIfMissing("maintenance_work_order", "plate_number_snapshot", "VARCHAR(20)");
        addColumnIfMissing("maintenance_work_order", "source_type", "VARCHAR(50)");
        addColumnIfMissing("maintenance_work_order", "source_record_id", "BIGINT");
        addColumnIfMissing("maintenance_work_order", "work_type", "VARCHAR(50)");
        addColumnIfMissing("maintenance_work_order", "issue_description", "VARCHAR(1000)");
        addColumnIfMissing("maintenance_work_order", "issue_photos", "TEXT");
        addColumnIfMissing("maintenance_work_order", "reported_by", "BIGINT");
        addColumnIfMissing("maintenance_work_order", "reported_time", "DATETIME");
        addColumnIfMissing("maintenance_work_order", "repair_vendor", "VARCHAR(100)");
        addColumnIfMissing("maintenance_work_order", "repair_contact", "VARCHAR(100)");
        addColumnIfMissing("maintenance_work_order", "estimated_cost", "DECIMAL(10,2)");
        addColumnIfMissing("maintenance_work_order", "actual_cost", "DECIMAL(10,2)");
        addColumnIfMissing("maintenance_work_order", "planned_start_time", "DATETIME");
        addColumnIfMissing("maintenance_work_order", "expected_finish_time", "DATETIME");
        addColumnIfMissing("maintenance_work_order", "repair_start_time", "DATETIME");
        addColumnIfMissing("maintenance_work_order", "repair_finish_time", "DATETIME");
        addColumnIfMissing("maintenance_work_order", "accepted_by", "BIGINT");
        addColumnIfMissing("maintenance_work_order", "accepted_time", "DATETIME");
        addColumnIfMissing("maintenance_work_order", "acceptance_result", "VARCHAR(500)");
        addColumnIfMissing("maintenance_work_order", "close_result_status", "VARCHAR(50)");
        addColumnIfMissing("maintenance_work_order", "remark", "VARCHAR(1000)");
        addColumnIfMissing("maintenance_work_order", "create_time", "DATETIME DEFAULT CURRENT_TIMESTAMP");
        addColumnIfMissing("maintenance_work_order", "update_time", "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    }

    private void createAttachmentTableIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'maintenance_work_order_attachment'",
                Integer.class);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute("""
                CREATE TABLE maintenance_work_order_attachment (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    order_id BIGINT NOT NULL,
                    category VARCHAR(50) NOT NULL,
                    file_path VARCHAR(500) NOT NULL,
                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_order (order_id),
                    INDEX idx_category (category)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }
}
