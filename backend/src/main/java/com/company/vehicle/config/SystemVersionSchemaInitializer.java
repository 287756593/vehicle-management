package com.company.vehicle.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SystemVersionSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SystemVersionSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS system_version_info ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "version_no VARCHAR(32) NOT NULL COMMENT '版本号',"
                        + "version_title VARCHAR(120) COMMENT '版本标题',"
                        + "change_log TEXT NOT NULL COMMENT '更新日志',"
                        + "release_time DATETIME NOT NULL COMMENT '发布时间',"
                        + "is_current TINYINT DEFAULT 0 COMMENT '是否当前版本: 1-是, 0-否',"
                        + "created_by VARCHAR(50) COMMENT '创建人',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                        + "INDEX idx_system_version_current (is_current),"
                        + "INDEX idx_system_version_release_time (release_time)"
                        + ") COMMENT='系统版本信息表'"
        );
        addColumnIfMissing("system_version_info", "version_title", "VARCHAR(120) COMMENT '版本标题'");
        addColumnIfMissing("system_version_info", "change_log", "TEXT COMMENT '更新日志'");
        addColumnIfMissing("system_version_info", "release_time", "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间'");
        addColumnIfMissing("system_version_info", "is_current", "TINYINT DEFAULT 0 COMMENT '是否当前版本: 1-是, 0-否'");
        addColumnIfMissing("system_version_info", "created_by", "VARCHAR(50) COMMENT '创建人'");
        initializeDefaultVersion();
        ensureCurrentVersionExists();
    }

    private void initializeDefaultVersion() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_version_info", Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.update(
                    "INSERT INTO system_version_info (version_no, version_title, change_log, release_time, is_current, created_by) VALUES (?, ?, ?, NOW(), 1, ?)",
                    "1.32",
                    "半年报告与管理员补登记",
                    "新增管理端“半年用车报告”，支持导出近 6 个月中文 PDF，汇总用车概况、加油概况和车辆公里数等信息\n"
                            + "新增管理员后台“补登记”能力，可补录忘记登记的借车记录，补登记时不要求上传任何照片\n"
                            + "补登记增加业务兜底校验：驾驶员或车辆存在未归还记录时禁止重复补登记，取车里程不能小于车辆当前里程\n"
                            + "修复半年报告页初始化白屏问题",
                    "system"
            );
        }
    }

    private void ensureCurrentVersionExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_version_info WHERE is_current = 1",
                Integer.class
        );
        if (count == null || count == 0) {
            Long latestId = jdbcTemplate.queryForObject(
                    "SELECT id FROM system_version_info ORDER BY release_time DESC, id DESC LIMIT 1",
                    Long.class
            );
            if (latestId != null) {
                jdbcTemplate.update("UPDATE system_version_info SET is_current = 1 WHERE id = ?", latestId);
            }
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                table,
                column
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
