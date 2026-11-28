package com.rentease.dto;

import com.rentease.entity.Room;
import com.rentease.entity.RoomStatus;

import java.math.BigDecimal;

public record RoomResponse(
        Long id,
        String roomNumber,
        String description,
        int capacity,
        BigDecimal monthlyRate,
        RoomStatus status,
        long activeLeases) {

    public static RoomResponse from(Room room, long activeLeases) {
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getDescription(),
                room.getCapacity(),
                room.getMonthlyRate(),
                room.getStatus(),
                activeLeases);
    }
}
