-- V9: Public ID (UUIDv4) for notification, session and API key entities.

ALTER TABLE public.user_notifications ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_notifications_public_id ON public.user_notifications (public_id);

ALTER TABLE public.admin_notifications ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_admin_notifications_public_id ON public.admin_notifications (public_id);

ALTER TABLE public.auth_sessions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_sessions_public_id ON public.auth_sessions (public_id);

ALTER TABLE public.user_api_keys ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_api_keys_public_id ON public.user_api_keys (public_id);
