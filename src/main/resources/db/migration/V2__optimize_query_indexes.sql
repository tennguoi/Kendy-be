-- ====================================================================
-- V2__optimize_query_indexes.sql
-- Kendrick / KendyDigital Performance Optimization Migration
-- Adds composite indexes to accelerate high-frequency queries,
-- pagination, background lifecycle jobs, and eliminate table scans.
-- ====================================================================

-- 1. Orders: optimize user order list, status filter, and daily revenue reporting
CREATE INDEX IF NOT EXISTS idx_orders_user_created_desc
    ON public.orders USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_user_status_created_desc
    ON public.orders USING btree (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_status_created
    ON public.orders USING btree (status, created_at);

-- 2. Wallet Transactions: optimize user transaction history and daily deposit aggregation
CREATE INDEX IF NOT EXISTS idx_wallet_tx_user_created_desc
    ON public.wallet_transactions USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wallet_tx_report
    ON public.wallet_transactions USING btree (type, direction, created_at);

-- 3. Audit Logs: eliminate Full Table Scan when filtering by actor_user_id
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created
    ON public.audit_logs USING btree (actor_user_id, created_at DESC);

-- 4. Ticket Messages: optimize message loading by ticket ordered by creation time
CREATE INDEX IF NOT EXISTS idx_ticket_messages_ticket_created
    ON public.ticket_messages USING btree (ticket_id, created_at ASC);

-- 5. Tickets: optimize user ticket list and status filtering
CREATE INDEX IF NOT EXISTS idx_tickets_user_created_desc
    ON public.tickets USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tickets_user_status_created_desc
    ON public.tickets USING btree (user_id, status, created_at DESC);

-- 6. Deposit Requests: optimize user deposit list and background expiration scanner
CREATE INDEX IF NOT EXISTS idx_deposits_user_created_desc
    ON public.deposit_requests USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_deposits_status_expired
    ON public.deposit_requests USING btree (status, expired_at ASC);

-- 7. User Entitlements: optimize user entitlement history and lifecycle expiration scanner
CREATE INDEX IF NOT EXISTS idx_entitlements_user_created_desc
    ON public.user_entitlements USING btree (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_entitlements_status_expires
    ON public.user_entitlements USING btree (status, expires_at ASC);

-- 8. Account Credentials: optimize inventory allocation and delivered credentials lookup
CREATE INDEX IF NOT EXISTS idx_credentials_service_status_created
    ON public.account_credentials USING btree (service_id, status, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_credentials_user_delivered
    ON public.account_credentials USING btree (delivered_to_user_id, delivered_at DESC, created_at DESC);

-- 9. Service Items: optimize storefront catalog sorting and category listing
CREATE INDEX IF NOT EXISTS idx_services_catalog_listing
    ON public.services USING btree (status, public_visible, sort_order ASC, name ASC);

CREATE INDEX IF NOT EXISTS idx_services_category_listing
    ON public.services USING btree (category_id, status, public_visible, sort_order ASC);
