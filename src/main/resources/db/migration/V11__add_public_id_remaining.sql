-- V11: Public ID (UUIDv4) for the remaining tables so every entity maps public_id
-- via the shared PublicIdEntity base class (eliminates schema/domain drift).

ALTER TABLE public.ticket_messages ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_ticket_messages_public_id ON public.ticket_messages (public_id);

ALTER TABLE public.ticket_attachments ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_ticket_attachments_public_id ON public.ticket_attachments (public_id);

ALTER TABLE public.coupon_redemptions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_coupon_redemptions_public_id ON public.coupon_redemptions (public_id);

ALTER TABLE public.stock_import_batches ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_import_batches_public_id ON public.stock_import_batches (public_id);

ALTER TABLE public.user_favorite_services ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_favorite_services_public_id ON public.user_favorite_services (public_id);

ALTER TABLE public.user_notification_settings ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_notification_settings_public_id ON public.user_notification_settings (public_id);

ALTER TABLE public.user_security_tokens ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_security_tokens_public_id ON public.user_security_tokens (public_id);

ALTER TABLE public.user_admin_roles ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_admin_roles_public_id ON public.user_admin_roles (public_id);

ALTER TABLE public.role_permissions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permissions_public_id ON public.role_permissions (public_id);

ALTER TABLE public.order_events ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_order_events_public_id ON public.order_events (public_id);

ALTER TABLE public.checkout_sessions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_checkout_sessions_public_id ON public.checkout_sessions (public_id);

ALTER TABLE public.system_settings ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_settings_public_id ON public.system_settings (public_id);

ALTER TABLE public.email_logs ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_logs_public_id ON public.email_logs (public_id);
