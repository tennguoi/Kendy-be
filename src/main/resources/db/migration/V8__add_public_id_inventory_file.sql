-- V8: Public ID (UUIDv4) for inventory, file and warranty aggregate roots.

ALTER TABLE public.account_credentials ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_account_credentials_public_id ON public.account_credentials (public_id);

ALTER TABLE public.stored_files ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_stored_files_public_id ON public.stored_files (public_id);

ALTER TABLE public.warranty_requests ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_warranty_requests_public_id ON public.warranty_requests (public_id);
