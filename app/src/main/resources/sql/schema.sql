-- COMP 440 Phase 1 schema
-- Run once in MySQL (e.g. Workbench) before launching the app.

CREATE DATABASE IF NOT EXISTS comp440;
USE comp440;

CREATE TABLE IF NOT EXISTS `user` (
    username  VARCHAR(50)  NOT NULL,
    password  VARCHAR(255) NOT NULL,  -- hashed password, never plaintext
    firstName VARCHAR(50)  NOT NULL,
    lastName  VARCHAR(50)  NOT NULL,
    email     VARCHAR(100) NOT NULL,
    phone     VARCHAR(20)  NOT NULL,
    PRIMARY KEY (username),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT uq_user_phone UNIQUE (phone)
);
