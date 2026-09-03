-- Test/demo accounts so the user-management feature can be exercised end to end.
--
-- Every account below shares the password:  Test1234!
-- (BCrypt hash generated with the same BCryptPasswordEncoder the app uses.)
--
-- The runtime PLATFORM_ADMIN (admin@pointsback.local / ChangeMe123!) is still created by
-- AdminBootstrapRunner on startup and is intentionally NOT inserted here.
--
-- Establishment ids come from V2__insert_initial_data.sql:
--   1 = Supermarket A      2 = Restaurant B
--
-- ids are left to AUTO_INCREMENT.

-- Extra platform admin (not establishment-scoped) --------------------------------------------
INSERT INTO users (name, email, password_hash, role, establishment_id, active) VALUES
  ('Marina Admin', 'admin2@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'PLATFORM_ADMIN', NULL, TRUE);

-- Supermarket A (establishment 1): 1 owner + 3 staff (1 inactive) ----------------------------
INSERT INTO users (name, email, password_hash, role, establishment_id, active) VALUES
  ('Alice Almeida',  'owner.super@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_OWNER', 1, TRUE),
  ('Bruno Barbosa',  'bruno.super@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_STAFF', 1, TRUE),
  ('Carla Costa',    'carla.super@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_STAFF', 1, TRUE),
  ('Diego Dias',     'diego.super@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_STAFF', 1, FALSE);

-- Restaurant B (establishment 2): 1 owner + 3 staff (1 inactive) -----------------------------
INSERT INTO users (name, email, password_hash, role, establishment_id, active) VALUES
  ('Otavio Oliveira', 'owner.resto@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_OWNER', 2, TRUE),
  ('Elena Esteves',   'elena.resto@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_STAFF', 2, TRUE),
  ('Felipe Farias',   'felipe.resto@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_STAFF', 2, TRUE),
  ('Gabriela Gomes',  'gabi.resto@pointsback.local',
   '$2a$10$CvozkcZt1qZeQ0d3gRrw3ei4bM05MxTJ1cvTId9k2fLTFNfArwRRO', 'ESTABLISHMENT_STAFF', 2, FALSE);
