package com.company.vehicle.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VehicleDataNormalizer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public VehicleDataNormalizer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.update(
                "UPDATE vehicle SET current_driver_id = NULL, current_driver_name = NULL, current_destination = NULL WHERE status <> 'IN_USE'"
        );
        jdbcTemplate.update(
                "UPDATE vehicle v "
                        + "JOIN vehicle_borrow_record r ON r.id = ("
                        + "  SELECT MAX(r2.id) FROM vehicle_borrow_record r2 "
                        + "  WHERE r2.vehicle_id = v.id AND r2.status = 'TAKEN'"
                        + ") "
                        + "SET v.current_destination = r.destination "
                        + "WHERE v.status = 'IN_USE'"
        );
        jdbcTemplate.update(
                "UPDATE vehicle v "
                        + "JOIN vehicle_borrow_record r ON r.vehicle_id = v.id "
                        + "AND r.id = (SELECT MAX(r2.id) FROM vehicle_borrow_record r2 WHERE r2.vehicle_id = v.id) "
                        + "SET v.status = 'NORMAL', v.clean_reason = NULL "
                        + "WHERE v.status = 'PENDING_CHECK' "
                        + "AND r.follow_up_status = 'PENDING' "
                        + "AND (r.issue_description IS NULL OR TRIM(r.issue_description) = '')"
        );
        jdbcTemplate.update(
                "UPDATE vehicle SET clean_reason = NULL WHERE status = 'NORMAL'"
        );
    }
}
