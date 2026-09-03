-- Clients now belong to exactly one establishment, required for establishment-scoped authorization
ALTER TABLE client ADD COLUMN establishment_id BIGINT;

-- Backfill existing seed clients based on their purchase history
UPDATE client SET establishment_id = 1 WHERE id = 1;
UPDATE client SET establishment_id = 2 WHERE id = 2;

ALTER TABLE client ALTER COLUMN establishment_id BIGINT NOT NULL;
ALTER TABLE client ADD CONSTRAINT fk_client_establishment FOREIGN KEY (establishment_id) REFERENCES establishment(id);
