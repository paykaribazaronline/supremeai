-- SupremeAI 2.0 — Migration 07: Multi-Tenant Rate Limiting & Config

-- Tenant (organization) limits table
CREATE TABLE IF NOT EXISTS tenant_limits (
    tenant_id TEXT PRIMARY KEY,
    org_name TEXT,
    requests_per_minute INTEGER DEFAULT 60,
    max_tokens_per_day INTEGER DEFAULT 100000,
    max_concurrent_sessions INTEGER DEFAULT 5,
    billing_tier TEXT DEFAULT 'free' CHECK (billing_tier IN ('free', 'starter', 'pro', 'enterprise')),
    stripe_customer_id TEXT,
    custom_limits JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Tenant usage tracking
CREATE TABLE IF NOT EXISTS tenant_usage (
    id BIGSERIAL PRIMARY KEY,
    tenant_id TEXT REFERENCES tenant_limits(tenant_id),
    date DATE DEFAULT CURRENT_DATE,
    requests_today INTEGER DEFAULT 0,
    tokens_today INTEGER DEFAULT 0,
    cost_today DECIMAL(10,4) DEFAULT 0.0,
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, date)
);

CREATE INDEX IF NOT EXISTS idx_tenant_usage_date ON tenant_usage(tenant_id, date);

-- SSO configurations
CREATE TABLE IF NOT EXISTS sso_configs (
    tenant_id TEXT PRIMARY KEY,
    provider TEXT NOT NULL CHECK (provider IN ('saml', 'oidc', 'google', 'azure', 'okta')),
    sp_entity_id TEXT,
    acs_url TEXT,
    sls_url TEXT,
    idp_entity_id TEXT,
    idp_sso_url TEXT,
    idp_slo_url TEXT,
    idp_x509_cert TEXT,
    oidc_client_id TEXT,
    oidc_client_secret TEXT,
    oidc_redirect_uri TEXT,
    oidc_domain TEXT,
    oidc_tenant TEXT,
    role_mappings JSONB DEFAULT '{}',
    enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Offline sync logs
CREATE TABLE IF NOT EXISTS offline_sync_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT,
    session_id TEXT,
    action_type TEXT NOT NULL,
    payload JSONB DEFAULT '{}',
    status TEXT DEFAULT 'synced' CHECK (status IN ('pending', 'synced', 'failed')),
    created_at TIMESTAMP DEFAULT NOW(),
    synced_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_offline_sync_user ON offline_sync_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_offline_sync_status ON offline_sync_logs(status);

-- Insert default free tier for all future tenants (trigger-based in prod)
INSERT INTO tenant_limits (tenant_id, org_name, billing_tier)
VALUES ('default', 'SupremeAI Default', 'free')
ON CONFLICT (tenant_id) DO NOTHING;
