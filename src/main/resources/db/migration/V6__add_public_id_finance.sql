-- V6: Public ID (UUIDv4) for finance aggregate roots.
-- External identifier exposed via API; internal Long id remains the primary key.
-- Backfill uses gen_random_uuid() (UUIDv4, PostgreSQL 13+). Existing rows get a
-- unique value per row thanks to the volatile default.

ALTER TABLE public.orders ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_public_id ON public.orders (public_id);

ALTER TABLE public.deposit_requests ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_deposit_requests_public_id ON public.deposit_requests (public_id);

ALTER TABLE public.wallet_transactions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_wallet_transactions_public_id ON public.wallet_transactions (public_id);

ALTER TABLE public.bank_transactions ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_bank_transactions_public_id ON public.bank_transactions (public_id);

ALTER TABLE public.manual_order_tasks ADD COLUMN IF NOT EXISTS public_id UUID NOT NULL DEFAULT gen_random_uuid();
CREATE UNIQUE INDEX IF NOT EXISTS uk_manual_order_tasks_public_id ON public.manual_order_tasks (public_id);
