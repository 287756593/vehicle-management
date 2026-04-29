package com.company.vehicle.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DriverSchemaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DriverSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DriverSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureOptionalLicenseNumber();
        ensureSortOrderColumn();
        ensureDriverUserUniqueIndex();
    }

    private void ensureOptionalLicenseNumber() {
        if (!isColumnNullable("driver", "license_number")) {
            jdbcTemplate.execute("ALTER TABLE driver MODIFY COLUMN license_number VARCHAR(50) NULL COMMENT '驾驶证号'");
        }
        jdbcTemplate.execute("UPDATE driver SET license_number = NULL WHERE license_number = ''");
    }

    private void ensureSortOrderColumn() {
        addColumnIfMissing("driver", "sort_order", "INT COMMENT '登录排序，数字越小越靠前'");
        jdbcTemplate.execute("UPDATE driver SET sort_order = id WHERE sort_order IS NULL");
    }

    private void ensureDriverUserUniqueIndex() {
        if (hasIndex("driver", "uk_driver_user")) {
            return;
        }
        Integer duplicateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM (SELECT user_id FROM driver WHERE user_id IS NOT NULL GROUP BY user_id HAVING COUNT(*) > 1) t",
                Integer.class
        );
        if (duplicateCount != null && duplicateCount > 0) {
            log.warn("driver.user_id 存在重复数据，跳过唯一索引创建，请先清理历史数据");
            return;
        }
        jdbcTemplate.execute("ALTER TABLE driver ADD UNIQUE INDEX uk_driver_user (user_id)");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private boolean isColumnNullable(String tableName, String columnName) {
        String nullable = jdbcTemplate.queryForObject(
                "SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class,
                tableName,
                columnName
        );
        return "YES".equalsIgnoreCase(nullable);
    }

    private boolean hasIndex(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }
}
