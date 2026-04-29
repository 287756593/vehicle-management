package com.company.vehicle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.company.vehicle.mapper")
public class VehicleManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(VehicleManagementApplication.class, args);
    }
}
