-- ============================================================
--  Migration: Add cloudinary_public_id to videos table
--  (PostgreSQL / Supabase version)
--
--  If you are using spring.jpa.hibernate.ddl-auto=update
--  Hibernate will add the column automatically on next startup.
--  Run this manually ONLY if you manage schema changes explicitly.
-- ============================================================

ALTER TABLE videos
  ADD COLUMN IF NOT EXISTS cloudinary_public_id VARCHAR(500) NULL;

COMMENT ON COLUMN videos.cloudinary_public_id IS
  'Cloudinary public_id required to delete the asset via the API';
