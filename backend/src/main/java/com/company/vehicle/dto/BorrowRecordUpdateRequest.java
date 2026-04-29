package com.company.vehicle.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class BorrowRecordUpdateRequest {
    private String usageReason;
    private String destination;
    private String takeTime;
    private String expectedReturnTime;
    private BigDecimal takeMileage;

    private String returnTime;
    private BigDecimal returnMileage;
    private Integer isClean;
    private Integer isFuelEnough;
    private String issueDescription;

    private MultipartFile[] takeVehiclePhotos;
    private MultipartFile[] takeMileagePhotos;
    private MultipartFile[] returnVehiclePhotos;
    private MultipartFile[] returnMileagePhotos;
    private MultipartFile[] returnFuelPhotos;
    private MultipartFile[] issuePhotos;
}
