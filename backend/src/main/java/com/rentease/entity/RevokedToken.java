package com.rentease.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Access tokens that were ended by logout; the server rejects them until they expire. */
@Entity
@Table(name = "revoked_token")
public class RevokedToken {

    @Id
    @Column(name = "token_id", length = 64)
    private String tokenId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String tokenId, LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() { return tokenId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
