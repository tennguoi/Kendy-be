-- V7: Public ID (UUIDv4) for catalog, coupon, ticket and content aggregate roots.

ALTER TABLE public.service_categories ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_service_categories_public_id ON public.service_categories (public_id);

ALTER TABLE public.services ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_services_public_id ON public.services (public_id);

ALTER TABLE public.coupons ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_coupons_public_id ON public.coupons (public_id);

ALTER TABLE public.tickets ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_tickets_public_id ON public.tickets (public_id);

ALTER TABLE public.content_items ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_content_items_public_id ON public.content_items (public_id);

ALTER TABLE public.user_entitlements ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_entitlements_public_id ON public.user_entitlements (public_id);
