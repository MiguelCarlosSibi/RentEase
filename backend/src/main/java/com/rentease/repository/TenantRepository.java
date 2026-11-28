package com.rentease.repository;

import com.rentease.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query("select t from Tenant t join fetch t.user u where t.owner.id = :ownerId order by u.fullName")
    List<Tenant> findAllForOwner(@Param("ownerId") Long ownerId);

    @Query("select t from Tenant t join fetch t.user u where t.id = :id and t.owner.id = :ownerId")
    Optional<Tenant> findOneForOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Query("select count(t) from Tenant t where t.owner.id = :ownerId")
    long countForOwner(@Param("ownerId") Long ownerId);
}
