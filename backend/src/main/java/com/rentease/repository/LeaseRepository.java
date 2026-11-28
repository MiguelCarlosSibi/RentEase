package com.rentease.repository;

import com.rentease.entity.Lease;
import com.rentease.entity.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaseRepository extends JpaRepository<Lease, Long> {

    String FETCH = "select l from Lease l join fetch l.room r join fetch l.tenant t join fetch t.user u ";

    @Query(FETCH + "where r.owner.id = :ownerId order by l.createdAt desc")
    List<Lease> findAllForOwner(@Param("ownerId") Long ownerId);

    @Query(FETCH + "where r.owner.id = :ownerId and l.status = :status order by l.createdAt desc")
    List<Lease> findForOwnerByStatus(@Param("ownerId") Long ownerId, @Param("status") LeaseStatus status);

    @Query(FETCH + "where l.id = :id and r.owner.id = :ownerId")
    Optional<Lease> findOneForOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query(FETCH + "where u.id = :userId and l.status = :status order by l.startDate")
    List<Lease> findForTenantUser(@Param("userId") Long userId, @Param("status") LeaseStatus status);

    @Query(FETCH + "where r.owner.id = :ownerId and l.status = :status and l.endDate between :fromDate and :toDate order by l.endDate")
    List<Lease> findEndingBetween(@Param("ownerId") Long ownerId, @Param("status") LeaseStatus status,
                                  @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select count(l) from Lease l where l.room.id = :roomId and l.status = :status")
    long countForRoom(@Param("roomId") Long roomId, @Param("status") LeaseStatus status);

    @Query("select count(l) from Lease l where l.room.owner.id = :ownerId and l.status = :status")
    long countForOwner(@Param("ownerId") Long ownerId, @Param("status") LeaseStatus status);

    @Query("select count(l) > 0 from Lease l where l.room.id = :roomId and l.tenant.id = :tenantId and l.status = :status")
    boolean existsForRoomAndTenant(@Param("roomId") Long roomId, @Param("tenantId") Long tenantId,
                                   @Param("status") LeaseStatus status);
}
