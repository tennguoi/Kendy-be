-- Run once before deploying the ServiceType enum reduced to ACCOUNT_STOCK and MANUAL.
-- Legacy types already followed the manual-order branch, so they are normalized to MANUAL.
UPDATE services
SET type = 'MANUAL'
WHERE type NOT IN ('ACCOUNT_STOCK', 'MANUAL');
