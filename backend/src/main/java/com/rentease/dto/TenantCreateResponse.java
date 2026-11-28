package com.rentease.dto;

/**
 * Result of adding a tenant. The temporary password is returned only when the SMS could not be
 * delivered, so the owner can hand it to the tenant in person. It is never stored in plain text.
 */
public record TenantCreateResponse(
        TenantResponse tenant,
        boolean credentialsSentBySms,
        String temporaryPassword,
        String message) {
}
