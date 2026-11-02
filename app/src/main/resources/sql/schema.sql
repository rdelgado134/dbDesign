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


-- Josiah
-- The following tables I'm using at the moment are based off the tables
-- I supposed in my google doc. Let me know if there are any issues, otherwise
-- I will continue as I am

CREATE TABLE item (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_username VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    date_posted DATE NOT NULL DEFAULT (CURRENT_DATE),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    FOREIGN KEY (seller_username) REFERENCES user(username)
);

CREATE TABLE itemCategory (
    item_id INT NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (item_id, category_name),
    FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE,
    CHECK (category_name REGEXP '^[a-z]+$')
);

CREATE TABLE review (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    reviewer_username VARCHAR(50) NOT NULL,
    item_id INT NOT NULL,
    rating VARCHAR(10) NOT NULL,
    comment TEXT,
    review_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    FOREIGN KEY (reviewer_username) REFERENCES user(username),
    FOREIGN KEY (item_id) REFERENCES item(item_id),
    -- enforces one review per user
    UNIQUE (reviewer_username, item_id)
);
