package com.rentease.repository;

import com.rentease.entity.Room;
import com.rentease.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("select r from Room r where r.owner.id = :ownerId order by r.roomNumber")
    List<Room> findAllForOwner(@Param("ownerId") Long ownerId);

    @Query("select r from Room r where r.id = :id and r.owner.id = :ownerId")
    Optional<Room> findOneForOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /** Locks the row so two leases cannot be created at the same time and exceed the room capacity. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id and r.owner.id = :ownerId")
    Optional<Room> findForUpdate(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query("select count(r) > 0 from Room r where r.owner.id = :ownerId and lower(r.roomNumber) = lower(:roomNumber)")
    boolean existsNumber(@Param("ownerId") Long ownerId, @Param("roomNumber") String roomNumber);

    @Query("select count(r) > 0 from Room r where r.owner.id = :ownerId and lower(r.roomNumber) = lower(:roomNumber) and r.id <> :id")
    boolean existsNumberExcept(@Param("ownerId") Long ownerId, @Param("roomNumber") String roomNumber, @Param("id") Long id);

    @Query("select count(r) from Room r where r.owner.id = :ownerId and r.status <> :status")
    long countNotInStatus(@Param("ownerId") Long ownerId, @Param("status") RoomStatus status);

    @Query("select count(r) from Room r where r.owner.id = :ownerId and r.status = :status")
    long countInStatus(@Param("ownerId") Long ownerId, @Param("status") RoomStatus status);
}
