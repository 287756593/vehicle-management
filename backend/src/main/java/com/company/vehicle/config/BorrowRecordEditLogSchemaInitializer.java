package com.company.vehicle.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BorrowRecordEditLogSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public BorrowRecordEditLogSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS vehicle_borrow_record_edit_log ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "record_id BIGINT NOT NULL COMMENT '借还车记录ID',"
                        + "record_no VARCHAR(50) NOT NULL COMMENT '借还车单号',"
                        + "operator_id BIGINT COMMENT '修改人ID',"
                        + "operator_name VARCHAR(100) COMMENT '修改人名称',"
                        + "change_summary VARCHAR(1000) COMMENT '修改摘要',"
                        + "before_snapshot TEXT COMMENT '修改前快照',"
                        + "after_snapshot TEXT COMMENT '修改后快照',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "INDEX idx_borrow_record_edit_record (record_id),"
                        + "INDEX idx_borrow_record_edit_time (create_time)"
                        + ") COMMENT='借还车记录修改日志表'"
        );
        addColumnIfMissing("vehicle_borrow_record_edit_log", "operator_name", "VARCHAR(100) COMMENT '修改人名称'");
        addColumnIfMissing("vehicle_borrow_record_edit_log", "change_summary", "VARCHAR(1000) COMMENT '修改摘要'");
        addColumnIfMissing("vehicle_borrow_record_edit_log", "before_snapshot", "TEXT COMMENT '修改前快照'");
        addColumnIfMissing("vehicle_borrow_record_edit_log", "after_snapshot", "TEXT COMMENT '修改后快照'");
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
