-- V5: Wallet freeze for account-takeover response (AUTH-13) and admin intervention.
-- Freezing blocks user-initiated debits (purchases) while allowing deposits/refunds and
-- administrative adjustments.

ALTER TABLE public.users ADD COLUMN IF NOT EXISTS wallet_frozen boolean DEFAULT false NOT NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS wallet_frozen_reason character varying(255);
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS wallet_frozen_at timestamp(6) with time zone;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS wallet_frozen_by bigint;
