-- Data integrity: required columns, value checks and lookup indexes.
-- Written to be valid on both PostgreSQL (runtime) and H2 (tests).

-- Backfill so the NOT NULL constraints below cannot fail on legacy rows
UPDATE client SET points = 0 WHERE points IS NULL;

ALTER TABLE client ALTER COLUMN points SET DEFAULT 0;
ALTER TABLE client ALTER COLUMN points SET NOT NULL;
ALTER TABLE client ALTER COLUMN name SET NOT NULL;
ALTER TABLE client ADD CONSTRAINT chk_client_points_non_negative CHECK (points >= 0);

ALTER TABLE establishment ALTER COLUMN name SET NOT NULL;
ALTER TABLE establishment ALTER COLUMN value_per_point SET NOT NULL;
ALTER TABLE establishment ADD CONSTRAINT chk_establishment_value_per_point CHECK (value_per_point > 0);

ALTER TABLE purchase ALTER COLUMN client_id SET NOT NULL;
ALTER TABLE purchase ALTER COLUMN establishment_id SET NOT NULL;
ALTER TABLE purchase ALTER COLUMN amount SET NOT NULL;
ALTER TABLE purchase ADD CONSTRAINT chk_purchase_amount_positive CHECK (amount > 0);

CREATE INDEX idx_client_establishment ON client (establishment_id);
CREATE INDEX idx_purchase_client ON purchase (client_id);
CREATE INDEX idx_purchase_establishment ON purchase (establishment_id);
CREATE INDEX idx_users_establishment ON users (establishment_id);
