package com.company.vehicle.config;

import com.company.vehicle.service.DriverService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DriverAccountInitializer implements CommandLineRunner {

    private final DriverService driverService;

    public DriverAccountInitializer(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public void run(String... args) {
        driverService.ensureAllDriverAccounts();
    }
}
