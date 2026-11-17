-- V10: Public ID (UUIDv4) for admin, monitoring and security entities.

ALTER TABLE public.admin_roles ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_roles_public_id ON public.admin_roles (public_id);

ALTER TABLE public.admin_permissions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_permissions_public_id ON public.admin_permissions (public_id);

ALTER TABLE public.audit_logs ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_logs_public_id ON public.audit_logs (public_id);

ALTER TABLE public.job_records ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_job_records_public_id ON public.job_records (public_id);

ALTER TABLE public.security_alerts ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_security_alerts_public_id ON public.security_alerts (public_id);

ALTER TABLE public.security_events ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_security_events_public_id ON public.security_events (public_id);

ALTER TABLE public.ip_bans ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_ip_bans_public_id ON public.ip_bans (public_id);

ALTER TABLE public.system_setting_history ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_setting_history_public_id ON public.system_setting_history (public_id);
