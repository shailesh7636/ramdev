-- ============================================================
--  Ramdev Thresher — Database Schema  (PostgreSQL / Supabase)
--  Migrated from MySQL.  Run ONCE on a fresh Supabase database.
--  The app's DataSeeder handles all seed data automatically.
-- ============================================================

-- NOTE: In Supabase the database is always named "postgres".
--  There is no "CREATE DATABASE" or "USE db" statement in PostgreSQL.
--  Tables live in the "public" schema by default.

-- 1. ROLES
CREATE TABLE IF NOT EXISTS roles (
    id    BIGSERIAL   NOT NULL,
    name  VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_role_name UNIQUE (name)
);

-- 2. USERS
--   MySQL DATETIME → PostgreSQL TIMESTAMP WITH TIME ZONE
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(120) NOT NULL,
    mobile      VARCHAR(15)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT uq_user_mobile UNIQUE (mobile)
);

-- 3. USER <-> ROLE  (many-to-many join table)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)  ON DELETE CASCADE
);

-- 4. VIDEOS
--   Added cloudinary_public_id (was a later ALTER TABLE in MySQL migration)
CREATE TABLE IF NOT EXISTS videos (
    id                    BIGSERIAL     NOT NULL,
    title                 VARCHAR(255)  NOT NULL,
    title_guj             VARCHAR(255),
    description           TEXT,
    file_path             VARCHAR(500)  NOT NULL,
    thumbnail             VARCHAR(500),
    cloudinary_public_id  VARCHAR(500),
    category              VARCHAR(80),
    duration_sec          INT,
    uploaded_by           BIGINT,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_vid_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
);

-- ──────────────────────────────────────────────────────────────
--  NOTE: Do NOT add seed data here.
--  The Spring Boot DataSeeder.java handles roles + default users
--  automatically on every startup using JPA.
-- ──────────────────────────────────────────────────────────────
