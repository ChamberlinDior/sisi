package com.pnis.backend.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter @Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Storage storage = new Storage();
    private final Security security = new Security();
    private final Audit audit = new Audit();
    private final Notification notification = new Notification();
    private final Sync sync = new Sync();
    private final Reporting reporting = new Reporting();
    private final Admin admin = new Admin();

    @Getter @Setter public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs  = 3600000L;
        private long refreshTokenExpirationMs = 604800000L;
        private long mfaTokenExpirationMs     = 300000L;
    }

    @Getter @Setter public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Getter @Setter public static class Storage {
        private String uploadDir = "./uploads";
        private long maxFileSizeBytes = 524288000L;
        private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "application/pdf");
        private String previewInlineTypes = "image/*,application/pdf";
    }

    @Getter @Setter public static class Security {
        private int maxLoginAttempts = 5;
        private int lockoutDurationMinutes = 30;
        private int sessionMaxConcurrent = 3;
        private int passwordMinLength = 12;
        private List<String> mfaRequiredRoles = List.of("ADMIN", "SUPER_ADMIN");
    }

    @Getter @Setter public static class Audit {
        private int retentionDays = 3650;
        private boolean wormEnabled = true;
    }

    @Getter @Setter public static class Notification {
        private boolean emailEnabled = true;
        private boolean smsEnabled = false;
        private boolean websocketEnabled = true;
    }

    @Getter @Setter public static class Sync {
        private boolean enabled = true;
        private String conflictResolution = "SERVER_WINS";
        private int maxPayloadMb = 50;
    }

    @Getter @Setter public static class Reporting {
        private String tempDir = "./temp/reports";
        private String watermarkText = "PNIS GABON – DOCUMENT CLASSIFIÉ";
        private String defaultClassification = "RESTRICTED";
    }

    @Getter @Setter public static class Admin {
        private String defaultSuperAdminEmail = "admin@pnis.gov.ga";
        private String defaultSuperAdminPassword = "Admin@PNIS2026!";
    }
}
