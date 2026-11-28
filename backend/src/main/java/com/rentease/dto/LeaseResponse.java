package com.rentease.dto;

import com.rentease.entity.Lease;
import com.rentease.entity.LeaseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeaseResponse(
        Long id,
        Long roomId,
        String roomNumber,
        Long tenantId,
        String tenantName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        int dueDay,
        BigDecimal depositAmount,
        LeaseStatus status) {

    public static LeaseResponse from(Lease lease) {
        return new LeaseResponse(
                lease.getId(),
                lease.getRoom().getId(),
                lease.getRoom().getRoomNumber(),
                lease.getTenant().getId(),
                lease.getTenant().getUser().getFullName(),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getMonthlyRent(),
                lease.getDueDay(),
                lease.getDepositAmount(),
                lease.getStatus());
    }
}
