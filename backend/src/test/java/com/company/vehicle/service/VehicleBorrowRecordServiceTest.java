package com.company.vehicle.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VehicleBorrowRecordServiceTest {

    @Test
    void shouldReuseMileagePhotoWhenReturnFuelPhotoMissing() {
        assertEquals(
                "/uploads/dashboard.jpg",
                VehicleBorrowRecordService.resolveReturnFuelPhoto("/uploads/dashboard.jpg", "")
        );
    }

    @Test
    void shouldKeepDedicatedFuelPhotoWhenProvided() {
        assertEquals(
                "/uploads/fuel.jpg",
                VehicleBorrowRecordService.resolveReturnFuelPhoto("/uploads/dashboard.jpg", "/uploads/fuel.jpg")
        );
    }
}
