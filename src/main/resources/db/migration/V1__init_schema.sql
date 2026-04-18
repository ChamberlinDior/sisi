-- ============================================================
-- PNIS BACKEND – Migration Flyway V1
-- Création du schéma complet de la plateforme nationale
-- ============================================================

SET NAMES utf8mb4;
SET character_set_client = utf8mb4;

-- ============================================================
-- TENANTS & UNITÉS
-- ============================================================
CREATE TABLE IF NOT EXISTS tenants (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(30)  NOT NULL UNIQUE,
    name             VARCHAR(200) NOT NULL,
    description      VARCHAR(1000),
    isolation_mode   VARCHAR(30)  DEFAULT 'LOGICAL',
    encryption_key_id VARCHAR(100),
    active           TINYINT(1)   DEFAULT 1,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6),
    INDEX idx_tenant_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS units (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    parent_unit_id BIGINT,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    description    VARCHAR(500),
    active         TINYINT(1)   DEFAULT 1,
    created_at     DATETIME(6),
    FOREIGN KEY (tenant_id)      REFERENCES tenants(id),
    FOREIGN KEY (parent_unit_id) REFERENCES units(id),
    INDEX idx_unit_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sharing_contracts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_tenant_id BIGINT       NOT NULL,
    target_tenant_id BIGINT       NOT NULL,
    object_type      VARCHAR(50)  DEFAULT 'ALL',
    max_classification VARCHAR(20) DEFAULT 'RESTRICTED',
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    valid_from       DATETIME(6),
    valid_until      DATETIME(6),
    granted_by       VARCHAR(100),
    created_at       DATETIME(6),
    FOREIGN KEY (source_tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (target_tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- AUTH
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30)  NOT NULL UNIQUE,
    description VARCHAR(300)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                  VARCHAR(100)  NOT NULL UNIQUE,
    email                     VARCHAR(200)  NOT NULL UNIQUE,
    password                  VARCHAR(255)  NOT NULL,
    full_name                 VARCHAR(200),
    position                  VARCHAR(100),
    phone_number              VARCHAR(50),
    tenant_id                 BIGINT,
    unit_id                   BIGINT,
    max_classification        VARCHAR(20)   DEFAULT 'RESTRICTED',
    enabled                   TINYINT(1)    DEFAULT 1,
    account_non_expired       TINYINT(1)    DEFAULT 1,
    credentials_non_expired   TINYINT(1)    DEFAULT 1,
    account_non_locked        TINYINT(1)    DEFAULT 1,
    failed_login_attempts     INT           DEFAULT 0,
    lockout_until             DATETIME(6),
    last_login_at             DATETIME(6),
    last_login_ip             VARCHAR(45),
    password_changed_at       DATETIME(6),
    must_change_password      TINYINT(1)    DEFAULT 0,
    mfa_enabled               TINYINT(1)    DEFAULT 0,
    mfa_secret                VARCHAR(64),
    refresh_token_hash        VARCHAR(64),
    refresh_token_expires_at  DATETIME(6),
    created_at                DATETIME(6),
    updated_at                DATETIME(6),
    created_by                VARCHAR(100),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    FOREIGN KEY (unit_id)   REFERENCES units(id),
    INDEX idx_user_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- AUDIT (WORM – pas d'UPDATE/DELETE)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_events (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT,
    actor         VARCHAR(100) NOT NULL,
    actor_ip      VARCHAR(45),
    action        VARCHAR(100) NOT NULL,
    object_type   VARCHAR(80),
    object_id     BIGINT,
    object_uuid   VARCHAR(36),
    result        VARCHAR(20)  DEFAULT 'SUCCESS',
    details       VARCHAR(4000),
    correlation_id VARCHAR(36),
    previous_hash VARCHAR(64),
    self_hash     VARCHAR(64),
    created_at    DATETIME(6)  NOT NULL,
    INDEX idx_audit_actor       (actor),
    INDEX idx_audit_object_type (object_type),
    INDEX idx_audit_object_id   (object_id),
    INDEX idx_audit_tenant      (tenant_id),
    INDEX idx_audit_created     (created_at),
    INDEX idx_audit_action      (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT,
    recipient_username  VARCHAR(100) NOT NULL,
    title               VARCHAR(100) NOT NULL,
    body                VARCHAR(2000) NOT NULL,
    event_type          VARCHAR(50),
    source_type         VARCHAR(80),
    source_id           BIGINT,
    priority            VARCHAR(20)  DEFAULT 'INFO',
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    channel             VARCHAR(20)  DEFAULT 'IN_APP',
    sent_at             DATETIME(6),
    read_at             DATETIME(6),
    expires_at          DATETIME(6),
    created_at          DATETIME(6)  NOT NULL,
    INDEX idx_notif_recipient (recipient_username),
    INDEX idx_notif_tenant    (tenant_id),
    INDEX idx_notif_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- COLLECTE
-- ============================================================
CREATE TABLE IF NOT EXISTS collected_data (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id           BIGINT,
    owner_org_id        BIGINT,
    record_status       VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope    VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel      VARCHAR(50),
    deleted             TINYINT(1)   DEFAULT 0,
    deletion_reason     VARCHAR(500),
    version             BIGINT       DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    integrity_hash      VARCHAR(64),
    title               VARCHAR(200) NOT NULL,
    raw_content         TEXT,
    data_type           VARCHAR(50),
    subject             VARCHAR(200),
    operation_ref       VARCHAR(100),
    location_label      VARCHAR(300),
    latitude            DOUBLE,
    longitude           DOUBLE,
    event_timestamp     DATETIME(6),
    source_reliability  VARCHAR(5),
    info_credibility    VARCHAR(5),
    confidence_score    INT,
    auto_classification VARCHAR(20),
    import_job_id       BIGINT,
    external_ref        VARCHAR(200),
    dedup_hash          VARCHAR(64),
    is_duplicate        TINYINT(1)   DEFAULT 0,
    duplicate_of_id     BIGINT,
    verification_notes  VARCHAR(2000),
    verified_by         VARCHAR(100),
    verified_at         DATETIME(6),
    sync_status         VARCHAR(20)  DEFAULT 'SYNCED',
    INDEX idx_collect_tenant  (tenant_id),
    INDEX idx_collect_status  (record_status),
    INDEX idx_collect_channel (source_channel),
    INDEX idx_collect_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS import_jobs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT       NOT NULL,
    file_name      VARCHAR(100) NOT NULL,
    file_path      VARCHAR(500),
    file_hash      VARCHAR(64),
    file_type      VARCHAR(20),
    target_type    VARCHAR(80),
    status         VARCHAR(30)  NOT NULL DEFAULT 'QUEUED',
    total_rows     INT          DEFAULT 0,
    processed_rows INT          DEFAULT 0,
    success_rows   INT          DEFAULT 0,
    error_rows     INT          DEFAULT 0,
    error_details  TEXT,
    submitted_by   VARCHAR(100) NOT NULL,
    started_at     DATETIME(6),
    completed_at   DATETIME(6),
    created_at     DATETIME(6),
    INDEX idx_import_tenant (tenant_id),
    INDEX idx_import_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- ENTITÉS
-- ============================================================
CREATE TABLE IF NOT EXISTS entity_records (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid               VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id          BIGINT,
    owner_org_id       BIGINT,
    record_status      VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope   VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel     VARCHAR(50),
    deleted            TINYINT(1)   DEFAULT 0,
    deletion_reason    VARCHAR(500),
    version            BIGINT       DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    integrity_hash     VARCHAR(64),
    reference_code     VARCHAR(60)  NOT NULL UNIQUE,
    entity_type        VARCHAR(30)  NOT NULL,
    label              VARCHAR(300) NOT NULL,
    description        TEXT,
    first_name         VARCHAR(100),
    last_name          VARCHAR(100),
    birth_date         VARCHAR(20),
    birth_place        VARCHAR(200),
    nationality        VARCHAR(100),
    id_number          VARCHAR(100),
    org_type           VARCHAR(100),
    country            VARCHAR(100),
    plate_number       VARCHAR(50),
    vehicle_type       VARCHAR(80),
    contact_value      VARCHAR(200),
    latitude           DOUBLE,
    longitude          DOUBLE,
    confidence_score   INT          DEFAULT 50,
    sensitivity_score  INT          DEFAULT 0,
    threat_level       VARCHAR(20),
    merged_into_id     BIGINT,
    is_merged          TINYINT(1)   DEFAULT 0,
    INDEX idx_entity_tenant (tenant_id),
    INDEX idx_entity_type   (entity_type),
    INDEX idx_entity_label  (label(100)),
    INDEX idx_entity_status (record_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS entity_aliases (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_id   BIGINT       NOT NULL,
    alias_value VARCHAR(300) NOT NULL,
    alias_type  VARCHAR(30),
    created_at  DATETIME(6),
    FOREIGN KEY (entity_id) REFERENCES entity_records(id) ON DELETE CASCADE,
    INDEX idx_alias_entity (entity_id),
    INDEX idx_alias_value  (alias_value(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS entity_links (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_entity_id BIGINT       NOT NULL,
    target_entity_id BIGINT       NOT NULL,
    link_type        VARCHAR(80)  NOT NULL,
    description      VARCHAR(500),
    certainty_level  VARCHAR(20)  DEFAULT 'POSSIBLE',
    valid_from       DATETIME(6),
    valid_until      DATETIME(6),
    source_ref       VARCHAR(200),
    tenant_id        BIGINT,
    created_by       VARCHAR(100),
    created_at       DATETIME(6),
    FOREIGN KEY (source_entity_id) REFERENCES entity_records(id),
    FOREIGN KEY (target_entity_id) REFERENCES entity_records(id),
    INDEX idx_link_source (source_entity_id),
    INDEX idx_link_target (target_entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DOSSIERS
-- ============================================================
CREATE TABLE IF NOT EXISTS intelligence_cases (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid               VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id          BIGINT,
    owner_org_id       BIGINT,
    record_status      VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope   VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel     VARCHAR(50),
    deleted            TINYINT(1)   DEFAULT 0,
    deletion_reason    VARCHAR(500),
    version            BIGINT       DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    integrity_hash     VARCHAR(64),
    reference_code     VARCHAR(80)  NOT NULL UNIQUE,
    title              VARCHAR(300) NOT NULL,
    description        TEXT,
    case_type          VARCHAR(50),
    case_status        VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    priority_level     VARCHAR(20)  DEFAULT 'MEDIUM',
    owner_unit_id      BIGINT,
    opened_by_id       BIGINT,
    lead_officer_id    BIGINT,
    target_summary     VARCHAR(2000),
    objective          VARCHAR(2000),
    geo_area           VARCHAR(500),
    opened_at          DATETIME(6),
    deadline           DATETIME(6),
    closed_at          DATETIME(6),
    closure_reason     VARCHAR(2000),
    parent_case_id     BIGINT,
    INDEX idx_case_tenant (tenant_id),
    INDEX idx_case_status (case_status),
    FOREIGN KEY (owner_unit_id)   REFERENCES units(id),
    FOREIGN KEY (opened_by_id)    REFERENCES users(id),
    FOREIGN KEY (lead_officer_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS case_notes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id         BIGINT       NOT NULL,
    title           VARCHAR(300) NOT NULL,
    content         TEXT         NOT NULL,
    note_type       VARCHAR(50),
    classification  VARCHAR(20)  DEFAULT 'RESTRICTED',
    author_id       BIGINT,
    tenant_id       BIGINT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    FOREIGN KEY (case_id)   REFERENCES intelligence_cases(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    INDEX idx_note_case (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- WORKFLOW
-- ============================================================
CREATE TABLE IF NOT EXISTS workflow_tasks (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                  VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id             BIGINT,
    owner_org_id          BIGINT,
    record_status         VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level  VARCHAR(20)  NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope      VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel        VARCHAR(50),
    deleted               TINYINT(1)   DEFAULT 0,
    deletion_reason       VARCHAR(500),
    version               BIGINT       DEFAULT 0,
    created_at            DATETIME(6)  NOT NULL,
    updated_at            DATETIME(6)  NOT NULL,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    integrity_hash        VARCHAR(64),
    object_type           VARCHAR(80)  NOT NULL,
    object_id             BIGINT       NOT NULL,
    object_uuid           VARCHAR(36),
    title                 VARCHAR(300) NOT NULL,
    description           TEXT,
    task_type             VARCHAR(50),
    task_status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    priority              VARCHAR(20)  DEFAULT 'MEDIUM',
    assignee_id           BIGINT,
    assignee_username     VARCHAR(100),
    due_date              DATETIME(6),
    started_at            DATETIME(6),
    completed_at          DATETIME(6),
    triggered_transition  VARCHAR(100),
    completion_note       VARCHAR(2000),
    reminder_count        INT          DEFAULT 0,
    last_reminder_at      DATETIME(6),
    parent_task_id        BIGINT,
    escalation_level      INT          DEFAULT 0,
    FOREIGN KEY (assignee_id) REFERENCES users(id),
    INDEX idx_task_tenant   (tenant_id),
    INDEX idx_task_assignee (assignee_id),
    INDEX idx_task_object   (object_type, object_id),
    INDEX idx_task_status   (task_status),
    INDEX idx_task_due      (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- ALERTES
-- ============================================================
CREATE TABLE IF NOT EXISTS alerts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id           BIGINT,
    owner_org_id        BIGINT,
    record_status       VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope    VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel      VARCHAR(50),
    deleted             TINYINT(1)   DEFAULT 0,
    deletion_reason     VARCHAR(500),
    version             BIGINT       DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    integrity_hash      VARCHAR(64),
    reference_code      VARCHAR(60)  NOT NULL UNIQUE,
    title               VARCHAR(300) NOT NULL,
    body                TEXT         NOT NULL,
    alert_type          VARCHAR(50),
    alert_status        VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    severity            VARCHAR(20)  DEFAULT 'MEDIUM',
    case_id             BIGINT,
    entity_id           BIGINT,
    geo_zone_id         BIGINT,
    latitude            DOUBLE,
    longitude           DOUBLE,
    radius_meters       DOUBLE,
    trigger_keywords    VARCHAR(500),
    recipients          VARCHAR(2000),
    published_to_pnis   TINYINT(1)   DEFAULT 0,
    published_at        DATETIME(6),
    expires_at          DATETIME(6),
    acknowledged_by     VARCHAR(100),
    acknowledged_at     DATETIME(6),
    lifted_reason       VARCHAR(1000),
    lifted_at           DATETIME(6),
    is_auto_generated   TINYINT(1)   DEFAULT 0,
    INDEX idx_alert_tenant   (tenant_id),
    INDEX idx_alert_status   (alert_status),
    INDEX idx_alert_severity (severity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- GÉOLOCALISATION
-- ============================================================
CREATE TABLE IF NOT EXISTS geo_events (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid               VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id          BIGINT,
    owner_org_id       BIGINT,
    record_status      VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope   VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel     VARCHAR(50),
    deleted            TINYINT(1)   DEFAULT 0,
    deletion_reason    VARCHAR(500),
    version            BIGINT       DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    integrity_hash     VARCHAR(64),
    title              VARCHAR(200) NOT NULL,
    description        TEXT,
    event_type         VARCHAR(50),
    latitude           DOUBLE       NOT NULL,
    longitude          DOUBLE       NOT NULL,
    altitude           DOUBLE,
    accuracy           DOUBLE,
    radius_meters      DOUBLE,
    entity_id          BIGINT,
    case_id            BIGINT,
    triggered_alert_id BIGINT,
    event_timestamp    DATETIME(6),
    address_label      VARCHAR(500),
    country            VARCHAR(100),
    region             VARCHAR(100),
    city               VARCHAR(100),
    geojson            TEXT,
    INDEX idx_geo_tenant  (tenant_id),
    INDEX idx_geo_type    (event_type),
    INDEX idx_geo_entity  (entity_id),
    INDEX idx_geo_case    (case_id),
    INDEX idx_geo_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- COULOIR PRIVÉ
-- ============================================================
CREATE TABLE IF NOT EXISTS corridor_messages (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id              BIGINT,
    channel_id             VARCHAR(100) NOT NULL,
    sender_username        VARCHAR(100) NOT NULL,
    recipient_username     VARCHAR(100) NOT NULL,
    subject                VARCHAR(300) NOT NULL,
    encrypted_body         TEXT         NOT NULL,
    is_encrypted           TINYINT(1)   DEFAULT 1,
    message_status         VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    read_receipt_required  TINYINT(1)   DEFAULT 0,
    ttl_hours              INT          DEFAULT 0,
    forward_forbidden      TINYINT(1)   DEFAULT 1,
    priority               VARCHAR(20)  DEFAULT 'NORMAL',
    sent_at                DATETIME(6),
    delivered_at           DATETIME(6),
    read_at                DATETIME(6),
    expires_at             DATETIME(6),
    revoked_at             DATETIME(6),
    revoked_by             VARCHAR(100),
    revoke_reason          VARCHAR(500),
    created_at             DATETIME(6)  NOT NULL,
    INDEX idx_corridor_sender    (sender_username),
    INDEX idx_corridor_recipient (recipient_username),
    INDEX idx_corridor_channel   (channel_id),
    INDEX idx_corridor_status    (message_status),
    INDEX idx_corridor_tenant    (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- DOCUMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS document_resources (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id           BIGINT,
    owner_org_id        BIGINT,
    record_status       VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope    VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel      VARCHAR(50),
    deleted             TINYINT(1)   DEFAULT 0,
    deletion_reason     VARCHAR(500),
    version             BIGINT       DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    integrity_hash      VARCHAR(64),
    original_name       VARCHAR(300) NOT NULL,
    stored_name         VARCHAR(300) NOT NULL,
    storage_path        VARCHAR(1000) NOT NULL,
    mime_type           VARCHAR(200),
    file_size           BIGINT,
    file_hash           VARCHAR(64),
    doc_status          VARCHAR(30)  NOT NULL DEFAULT 'UPLOADED',
    owner_type          VARCHAR(80),
    owner_id            BIGINT,
    title               VARCHAR(200),
    description         VARCHAR(2000),
    version_number      INT          DEFAULT 1,
    previous_version_id BIGINT,
    watermark_text      VARCHAR(200),
    is_sealed           TINYINT(1)   DEFAULT 0,
    av_status           VARCHAR(30)  DEFAULT 'PENDING',
    av_scanned_at       DATETIME(6),
    INDEX idx_doc_tenant (tenant_id),
    INDEX idx_doc_owner  (owner_type, owner_id),
    INDEX idx_doc_hash   (file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- PUBLICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS publications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid                VARCHAR(36)  NOT NULL UNIQUE,
    tenant_id           BIGINT,
    owner_org_id        BIGINT,
    record_status       VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    classification_level VARCHAR(20) NOT NULL DEFAULT 'RESTRICTED',
    visibility_scope    VARCHAR(20)  NOT NULL DEFAULT 'TENANT_ONLY',
    source_channel      VARCHAR(50),
    deleted             TINYINT(1)   DEFAULT 0,
    deletion_reason     VARCHAR(500),
    version             BIGINT       DEFAULT 0,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    integrity_hash      VARCHAR(64),
    reference_code      VARCHAR(60)  NOT NULL UNIQUE,
    title               VARCHAR(300) NOT NULL,
    content             TEXT         NOT NULL,
    pub_type            VARCHAR(50)  NOT NULL,
    pub_status          VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    target_tenants      VARCHAR(500) DEFAULT 'ALL_TENANTS',
    is_anonymous        TINYINT(1)   DEFAULT 0,
    alert_id            BIGINT,
    case_id             BIGINT,
    published_at        DATETIME(6),
    expires_at          DATETIME(6),
    retracted_at        DATETIME(6),
    retract_reason      VARCHAR(1000),
    reviewed_by         VARCHAR(100),
    view_count          BIGINT       DEFAULT 0,
    INDEX idx_pub_tenant    (tenant_id),
    INDEX idx_pub_status    (pub_status),
    INDEX idx_pub_type      (pub_type),
    INDEX idx_pub_published (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- RÉFÉRENTIELS (Administration)
-- ============================================================
CREATE TABLE IF NOT EXISTS reference_data (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT,
    category    VARCHAR(80)  NOT NULL,
    code        VARCHAR(100) NOT NULL,
    label       VARCHAR(300) NOT NULL,
    description VARCHAR(1000),
    extra_data  TEXT,
    sort_order  INT          DEFAULT 0,
    active      TINYINT(1)   DEFAULT 1,
    is_system   TINYINT(1)   DEFAULT 0,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    created_by  VARCHAR(100),
    INDEX idx_ref_category (category),
    INDEX idx_ref_tenant   (tenant_id),
    INDEX idx_ref_code     (code),
    UNIQUE KEY uq_ref_cat_code (category, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
