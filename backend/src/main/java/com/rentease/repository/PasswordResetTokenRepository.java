package com.rentease.repository;

import com.rentease.entity.PasswordResetToken;
import com.rentease.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(LocalDateTime time);

    /**
     * Marks a token as used in one atomic statement. It returns 0 if the token was already used
     * or has expired, which is what makes every token single-use even for simultaneous requests.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now "
            + "where t.id = :id and t.usedAt is null and t.expiresAt > :now")
    int markUsed(@Param("id") Long id, @Param("now") LocalDateTime now);
}
