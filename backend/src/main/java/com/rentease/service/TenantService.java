package com.rentease.service;

import com.rentease.dto.TenantCreateResponse;
import com.rentease.dto.TenantRequest;
import com.rentease.dto.TenantResponse;
import com.rentease.dto.TenantUpdateRequest;
import com.rentease.entity.Lease;
import com.rentease.entity.LeaseStatus;
import com.rentease.entity.Role;
import com.rentease.entity.SmsType;
import com.rentease.entity.Tenant;
import com.rentease.entity.User;
import com.rentease.entity.UserStatus;
import com.rentease.exception.ApiException;
import com.rentease.repository.LeaseRepository;
import com.rentease.repository.TenantRepository;
import com.rentease.repository.UserRepository;
import com.rentease.util.PhoneNumbers;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Tenant management for owners (FR013, FR014, FR015). */
@Service
public class TenantService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";

    private final TenantRepository tenants;
    private final LeaseRepository leases;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SmsService sms;

    public TenantService(TenantRepository tenants,
                         LeaseRepository leases,
                         UserRepository users,
                         PasswordEncoder passwordEncoder,
                         SmsService sms) {
        this.tenants = tenants;
        this.leases = leases;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sms = sms;
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> list(Long ownerId, String search) {
        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        Map<Long, Long> active = leases.findForOwnerByStatus(ownerId, LeaseStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(l -> l.getTenant().getId(), Collectors.counting()));

        return tenants.findAllForOwner(ownerId).stream()
                .filter(t -> q.isEmpty()
                        || t.getUser().getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || t.getUser().getEmail().toLowerCase(Locale.ROOT).contains(q)
                        || t.getUser().getMobileNumber().contains(q))
                .map(t -> TenantResponse.from(t, active.getOrDefault(t.getId(), 0L)))
                .toList();
    }

    /**
     * Creates the tenant profile and the login account, then sends the temporary credentials by SMS.
     * The account must change the temporary password at first login.
     */
    @Transactional
    public TenantCreateResponse create(Long ownerId, TenantRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered",
                    Map.of("email", "This email is already registered"));
        }

        String temporaryPassword = generateTemporaryPassword();

        User account = new User();
        account.setFullName(request.fullName().trim());
        account.setEmail(email);
        account.setMobileNumber(PhoneNumbers.normalize(request.mobileNumber()));
        account.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        account.setRole(Role.TENANT);
        account.setStatus(UserStatus.ACTIVE);
        account.setMustChangePassword(true);
        users.save(account);

        Tenant tenant = new Tenant();
        tenant.setOwner(users.getReferenceById(ownerId));
        tenant.setUser(account);
        tenant.setEmergencyContactName(request.emergencyContactName().trim());
        tenant.setEmergencyContactNumber(PhoneNumbers.normalize(request.emergencyContactNumber()));
        tenants.save(tenant);

        String body = "RentEase: your tenant account is ready. Login email: " + email
                + " Temporary password: " + temporaryPassword + " You must change it when you first log in.";
        // The SMS log never keeps the password (NFR006).
        String logBody = "RentEase: tenant account created. Login email: " + email
                + ". A temporary password was included in this message.";
        boolean sent = sms.send(account, SmsType.CREDENTIALS, account.getMobileNumber(), body, logBody);

        String message = sent
                ? "Tenant added. The login details were sent by SMS to " + account.getMobileNumber() + "."
                : "Tenant added, but the SMS could not be sent. Give the temporary password below to the tenant.";
        return new TenantCreateResponse(TenantResponse.from(tenant, 0), sent, sent ? null : temporaryPassword, message);
    }

    @Transactional
    public TenantResponse update(Long ownerId, Long id, TenantUpdateRequest request) {
        Tenant tenant = find(ownerId, id);
        User account = tenant.getUser();
        account.setFullName(request.fullName().trim());
        account.setMobileNumber(PhoneNumbers.normalize(request.mobileNumber()));
        tenant.setEmergencyContactName(request.emergencyContactName().trim());
        tenant.setEmergencyContactNumber(PhoneNumbers.normalize(request.emergencyContactNumber()));
        users.save(account);
        tenants.save(tenant);
        return TenantResponse.from(tenant, activeLeases(ownerId, tenant.getId()));
    }

    /**
     * FR015: a deactivated tenant cannot log in, but all bills and payments stay. Existing sessions
     * end at once because the token version is raised.
     */
    @Transactional
    public TenantResponse setActive(Long ownerId, Long id, boolean active) {
        Tenant tenant = find(ownerId, id);
        User account = tenant.getUser();
        account.setStatus(active ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        if (!active) {
            account.incrementTokenVersion();
        }
        users.save(account);
        return TenantResponse.from(tenant, activeLeases(ownerId, tenant.getId()));
    }

    // ------------------------------------------------------------------ helpers

    private Tenant find(Long ownerId, Long id) {
        return tenants.findOneForOwner(id, ownerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    private long activeLeases(Long ownerId, Long tenantId) {
        return leases.findForOwnerByStatus(ownerId, LeaseStatus.ACTIVE).stream()
                .filter((Lease l) -> l.getTenant().getId().equals(tenantId))
                .count();
    }

    /** 10 characters with at least one letter and one digit; ambiguous characters (0, O, 1, l, I) are left out. */
    static String generateTemporaryPassword() {
        String all = LETTERS + DIGITS;
        List<Character> chars = new ArrayList<>();
        chars.add(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        for (int i = 0; i < 8; i++) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }
}
