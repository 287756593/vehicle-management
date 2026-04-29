package com.company.vehicle.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BorrowRecordSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public BorrowRecordSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS vehicle_borrow_record ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "record_no VARCHAR(50) NOT NULL UNIQUE COMMENT '借还车单号',"
                        + "vehicle_id BIGINT NOT NULL COMMENT '车辆ID',"
                        + "plate_number VARCHAR(20) NOT NULL COMMENT '借用时车牌号',"
                        + "driver_id BIGINT NOT NULL COMMENT '驾驶员ID',"
                        + "driver_name VARCHAR(50) NOT NULL COMMENT '驾驶员姓名',"
                        + "status VARCHAR(20) DEFAULT 'TAKEN' COMMENT '状态: TAKEN-已取车, RETURNED-已还车',"
                        + "usage_reason VARCHAR(500) COMMENT '用车事由',"
                        + "destination VARCHAR(255) COMMENT '目的地/去向',"
                        + "expected_return_time DATETIME COMMENT '预计还车时间',"
                        + "take_mileage DECIMAL(10,2) NOT NULL COMMENT '取车时里程',"
                        + "take_vehicle_photos TEXT COMMENT '取车车辆照片',"
                        + "take_mileage_photo VARCHAR(500) COMMENT '取车里程照片',"
                        + "take_time DATETIME NOT NULL COMMENT '取车时间',"
                        + "return_mileage DECIMAL(10,2) COMMENT '还车时累计里程',"
                        + "return_vehicle_photos TEXT COMMENT '还车停车照片',"
                        + "return_mileage_photo VARCHAR(500) COMMENT '还车里程照片',"
                        + "return_fuel_photo VARCHAR(500) COMMENT '还车油表照片',"
                        + "is_clean TINYINT DEFAULT 1 COMMENT '是否干净: 1-是, 0-否',"
                        + "is_fuel_enough TINYINT DEFAULT 1 COMMENT '油量是否不少于半箱: 1-是, 0-否',"
                        + "issue_description VARCHAR(500) COMMENT '车辆异常说明',"
                        + "issue_photos TEXT COMMENT '车辆异常照片',"
                        + "action_required VARCHAR(255) COMMENT '还车后需处理事项',"
                        + "follow_up_status VARCHAR(20) DEFAULT 'NONE' COMMENT '跟进状态: NONE-无需处理, PENDING-待处理, COMPLETED-已处理',"
                        + "follow_up_remark VARCHAR(500) COMMENT '跟进处理说明',"
                        + "follow_up_handled_by BIGINT COMMENT '跟进处理人',"
                        + "follow_up_handled_time DATETIME COMMENT '跟进处理时间',"
                        + "follow_up_result_status VARCHAR(20) COMMENT '处理后车辆状态',"
                        + "return_time DATETIME COMMENT '还车时间',"
                        + "create_time DATETIME DEFAULT CURRENT_TIMESTAMP,"
                        + "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                        + "INDEX idx_vehicle_borrow_vehicle (vehicle_id),"
                        + "INDEX idx_vehicle_borrow_driver (driver_id),"
                        + "INDEX idx_vehicle_borrow_status (status),"
                        + "INDEX idx_vehicle_borrow_take_time (take_time)"
                        + ") COMMENT='借还车记录表'"
        );
        addColumnIfMissing("vehicle_borrow_record", "usage_reason", "VARCHAR(500) COMMENT '用车事由'");
        addColumnIfMissing("vehicle_borrow_record", "destination", "VARCHAR(255) COMMENT '目的地/去向'");
        addColumnIfMissing("vehicle_borrow_record", "expected_return_time", "DATETIME COMMENT '预计还车时间'");
        addColumnIfMissing("vehicle_borrow_record", "issue_description", "VARCHAR(500) COMMENT '车辆异常说明'");
        addColumnIfMissing("vehicle_borrow_record", "issue_photos", "TEXT COMMENT '车辆异常照片'");
        addColumnIfMissing("vehicle_borrow_record", "follow_up_status", "VARCHAR(20) DEFAULT 'NONE' COMMENT '跟进状态: NONE-无需处理, PENDING-待处理, COMPLETED-已处理'");
        addColumnIfMissing("vehicle_borrow_record", "follow_up_remark", "VARCHAR(500) COMMENT '跟进处理说明'");
        addColumnIfMissing("vehicle_borrow_record", "follow_up_handled_by", "BIGINT COMMENT '跟进处理人'");
        addColumnIfMissing("vehicle_borrow_record", "follow_up_handled_time", "DATETIME COMMENT '跟进处理时间'");
        addColumnIfMissing("vehicle_borrow_record", "follow_up_result_status", "VARCHAR(20) COMMENT '处理后车辆状态'");
        addColumnIfMissing("vehicle_borrow_record", "deleted", "TINYINT DEFAULT 0 COMMENT '是否删除: 0-否,1-是'");
        addColumnIfMissing("vehicle_borrow_record", "deleted_time", "DATETIME COMMENT '删除时间'");
        addColumnIfMissing("vehicle_borrow_record", "deleted_by", "BIGINT COMMENT '删除人ID'");
        addColumnIfMissing("vehicle_borrow_record", "delete_reason", "VARCHAR(500) COMMENT '删除原因'");
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
