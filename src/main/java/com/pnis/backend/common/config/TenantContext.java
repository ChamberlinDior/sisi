package com.pnis.backend.common.config;

/**
 * Stocke le tenantId courant dans un ThreadLocal.
 * Alimenté par JwtAuthenticationFilter après vérification du JWT.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER  = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) { CURRENT_TENANT.set(tenantId); }
    public static Long  getTenantId()             { return CURRENT_TENANT.get(); }

    public static void setCurrentUser(String username) { CURRENT_USER.set(username); }
    public static String getCurrentUser()              { return CURRENT_USER.get(); }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
}
