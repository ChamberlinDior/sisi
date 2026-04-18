package com.pnis.backend.auth.model;

import com.pnis.backend.common.enums.ClassificationLevel;
import com.pnis.backend.tenant.model.Tenant;
import com.pnis.backend.tenant.model.Unit;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_email",    columnList = "email",    unique = true),
        @Index(name = "idx_user_tenant",   columnList = "tenant_id")
})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(length = 100)
    private String position;

    @Column(length = 50)
    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<AppRole> roles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "max_classification", length = 20)
    private ClassificationLevel maxClassification = ClassificationLevel.RESTRICTED;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "account_non_expired")
    private boolean accountNonExpired = true;

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired = true;

    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @Column(name = "lockout_until")
    private Instant lockoutUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password")
    private boolean mustChangePassword = false;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    @Column(name = "refresh_token_hash", length = 64)
    private String refreshTokenHash;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== GETTERS =====
    public Long                getId()                    { return id; }
    public String              getUsername()              { return username; }
    public String              getEmail()                 { return email; }
    public String              getPassword()              { return password; }
    public String              getFullName()              { return fullName; }
    public String              getPosition()              { return position; }
    public String              getPhoneNumber()           { return phoneNumber; }
    public Tenant              getTenant()                { return tenant; }
    public Unit                getUnit()                  { return unit; }
    public Set<AppRole>        getRoles()                 { return roles; }
    public ClassificationLevel getMaxClassification()     { return maxClassification; }
    public boolean             isEnabled()                { return enabled; }
    public boolean             isAccountNonExpired()      { return accountNonExpired; }
    public boolean             isCredentialsNonExpired()  { return credentialsNonExpired; }
    public boolean             isAccountNonLocked()       { return accountNonLocked; }
    public int                 getFailedLoginAttempts()   { return failedLoginAttempts; }
    public Instant             getLockoutUntil()          { return lockoutUntil; }
    public Instant             getLastLoginAt()           { return lastLoginAt; }
    public String              getLastLoginIp()           { return lastLoginIp; }
    public Instant             getPasswordChangedAt()     { return passwordChangedAt; }
    public boolean             isMustChangePassword()     { return mustChangePassword; }
    public boolean             isMfaEnabled()             { return mfaEnabled; }
    public String              getMfaSecret()             { return mfaSecret; }
    public String              getRefreshTokenHash()      { return refreshTokenHash; }
    public Instant             getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public Instant             getCreatedAt()             { return createdAt; }
    public Instant             getUpdatedAt()             { return updatedAt; }
    public String              getCreatedBy()             { return createdBy; }

    // ===== SETTERS =====
    public void setId(Long id)                                  { this.id = id; }
    public void setUsername(String username)                    { this.username = username; }
    public void setEmail(String email)                          { this.email = email; }
    public void setPassword(String password)                    { this.password = password; }
    public void setFullName(String fullName)                    { this.fullName = fullName; }
    public void setPosition(String position)                    { this.position = position; }
    public void setPhoneNumber(String phoneNumber)              { this.phoneNumber = phoneNumber; }
    public void setTenant(Tenant tenant)                        { this.tenant = tenant; }
    public void setUnit(Unit unit)                              { this.unit = unit; }
    public void setRoles(Set<AppRole> roles)                    { this.roles = roles; }
    public void setMaxClassification(ClassificationLevel level) { this.maxClassification = level; }
    public void setEnabled(boolean enabled)                     { this.enabled = enabled; }
    public void setAccountNonExpired(boolean v)                 { this.accountNonExpired = v; }
    public void setCredentialsNonExpired(boolean v)             { this.credentialsNonExpired = v; }
    public void setAccountNonLocked(boolean v)                  { this.accountNonLocked = v; }
    public void setFailedLoginAttempts(int n)                   { this.failedLoginAttempts = n; }
    public void setLockoutUntil(Instant t)                      { this.lockoutUntil = t; }
    public void setLastLoginAt(Instant t)                       { this.lastLoginAt = t; }
    public void setLastLoginIp(String ip)                       { this.lastLoginIp = ip; }
    public void setPasswordChangedAt(Instant t)                 { this.passwordChangedAt = t; }
    public void setMustChangePassword(boolean v)                { this.mustChangePassword = v; }
    public void setMfaEnabled(boolean v)                        { this.mfaEnabled = v; }
    public void setMfaSecret(String s)                          { this.mfaSecret = s; }
    public void setRefreshTokenHash(String h)                   { this.refreshTokenHash = h; }
    public void setRefreshTokenExpiresAt(Instant t)             { this.refreshTokenExpiresAt = t; }
    public void setCreatedBy(String createdBy)                  { this.createdBy = createdBy; }
    public void setUpdatedAt(Instant t)                         { this.updatedAt = t; }
}