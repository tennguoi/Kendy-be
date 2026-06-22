-- PostgreSQL migration for time-limited customer access.
ALTER TABLE services
    ADD COLUMN IF NOT EXISTS access_strategy VARCHAR(32),
    ADD COLUMN IF NOT EXISTS access_duration_days INTEGER;

UPDATE services
SET access_strategy = CASE
    WHEN type = 'ACCOUNT_STOCK' THEN 'DEDICATED_ACCOUNT'
    ELSE 'MANUAL'
END
WHERE access_strategy IS NULL;

CREATE TABLE IF NOT EXISTS user_entitlements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    service_id BIGINT NOT NULL REFERENCES services(id),
    source_order_id BIGINT NOT NULL REFERENCES orders(id),
    access_strategy VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    access_identifier VARCHAR(255),
    external_resource_id VARCHAR(255),
    provider_metadata TEXT,
    starts_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    suspended_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    renewal_requested_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_entitlements_source_order UNIQUE (source_order_id)
);

CREATE INDEX IF NOT EXISTS idx_entitlements_user_status
    ON user_entitlements(user_id, status);
CREATE INDEX IF NOT EXISTS idx_entitlements_expires_at
    ON user_entitlements(expires_at);
CREATE INDEX IF NOT EXISTS idx_entitlements_external_resource
    ON user_entitlements(external_resource_id);

INSERT INTO user_entitlements (
    user_id,
    service_id,
    source_order_id,
    access_strategy,
    status,
    access_identifier,
    starts_at,
    expires_at,
    created_at,
    updated_at
)
SELECT
    o.user_id,
    o.service_id,
    o.id,
    COALESCE(
        s.access_strategy,
        CASE WHEN s.type = 'ACCOUNT_STOCK' THEN 'DEDICATED_ACCOUNT' ELSE 'MANUAL' END
    ),
    'ACTIVE',
    COALESCE(ac.login_identifier, u.email),
    COALESCE(o.completed_at, o.created_at),
    COALESCE(
        ac.expires_at,
        CASE
            WHEN s.access_duration_days IS NOT NULL
            THEN COALESCE(o.completed_at, o.created_at)
                + make_interval(days => s.access_duration_days)
        END
    ),
    NOW(),
    NOW()
FROM orders o
JOIN services s ON s.id = o.service_id
JOIN users u ON u.id = o.user_id
LEFT JOIN account_credentials ac ON ac.assigned_order_id = o.id
WHERE o.status = 'COMPLETED'
ON CONFLICT (source_order_id) DO NOTHING;
