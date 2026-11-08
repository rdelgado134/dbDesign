-- COMP 440 Phase 2 schema: online marketplace
-- Run the whole script once in MySQL 8.0.16+ (e.g. Workbench) before launching the app.
-- To pick up changes to an existing database: DROP DATABASE comp440; then run this again.
--
-- Where each business rule is enforced (see README for the full table):
--   * keys and UNIQUE/CHECK constraints are declared on the tables below
--   * daily limits, no self-review, read-only reviews and "keep at least one
--     category" are triggers at the bottom of this file

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

CREATE TABLE IF NOT EXISTS item (
    item_id         INT AUTO_INCREMENT PRIMARY KEY,
    seller_username VARCHAR(50)    NOT NULL,
    title           VARCHAR(150)   NOT NULL,
    description     TEXT,
    date_posted     DATE           NOT NULL DEFAULT (CURRENT_DATE),
    price           DECIMAL(10, 2) NOT NULL,
    CONSTRAINT chk_item_price CHECK (price >= 0),
    CONSTRAINT fk_item_seller FOREIGN KEY (seller_username) REFERENCES `user`(username)
);

-- "category" is a multivalued attribute of ITEM in the ER diagram,
-- so each (item, category) pair is one row here.
CREATE TABLE IF NOT EXISTS itemCategory (
    item_id       INT         NOT NULL,
    category_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (item_id, category_name),
    CONSTRAINT fk_itemcategory_item FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE,
    -- one lowercase word, e.g. electronics, books, gaming
    -- ('c' = case-sensitive; plain REGEXP ignores case under the default collation)
    CONSTRAINT chk_category_lowercase_word CHECK (REGEXP_LIKE(category_name, '^[a-z]+$', 'c'))
);

CREATE TABLE IF NOT EXISTS review (
    review_id         INT AUTO_INCREMENT PRIMARY KEY,
    reviewer_username VARCHAR(50) NOT NULL,
    item_id           INT         NOT NULL,
    rating            VARCHAR(10) NOT NULL,
    comment           TEXT        NOT NULL,
    review_date       DATE        NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_username) REFERENCES `user`(username),
    CONSTRAINT fk_review_item FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE,
    -- a user may submit only one review for a particular item
    CONSTRAINT uq_review_one_per_item UNIQUE (reviewer_username, item_id),
    CONSTRAINT chk_review_rating CHECK (rating IN ('Excellent', 'Good', 'Fair', 'Poor'))
);

-- ---------------------------------------------------------------------------
-- Business-rule triggers
-- Each one raises SQLSTATE '45000' with a message the app shows to the user.
-- DELIMITER is a Workbench / mysql client command, so ; inside a trigger body
-- doesn't end the CREATE TRIGGER statement early.
-- ---------------------------------------------------------------------------

DROP TRIGGER IF EXISTS trg_item_max_two_per_day;
DROP TRIGGER IF EXISTS trg_review_max_two_per_day;
DROP TRIGGER IF EXISTS trg_review_not_own_item;
DROP TRIGGER IF EXISTS trg_review_read_only;
DROP TRIGGER IF EXISTS trg_itemcategory_keep_one;

DELIMITER $$

-- A user may post at most two items per calendar day.
CREATE TRIGGER trg_item_max_two_per_day
BEFORE INSERT ON item
FOR EACH ROW
BEGIN
    DECLARE posted_that_day INT;
    -- date_posted is normally left to its default (today); seed data can supply past dates
    SET NEW.date_posted = COALESCE(NEW.date_posted, CURRENT_DATE);
    SELECT COUNT(*) INTO posted_that_day
      FROM item
     WHERE seller_username = NEW.seller_username
       AND date_posted = NEW.date_posted;
    IF posted_that_day >= 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'You can post at most 2 items per day. Try again tomorrow.';
    END IF;
END$$

-- A user may submit at most two reviews per calendar day
-- (the assignment listed both 2 and 3; the instructor confirmed 2).
CREATE TRIGGER trg_review_max_two_per_day
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
    DECLARE reviewed_that_day INT;
    SET NEW.review_date = COALESCE(NEW.review_date, CURRENT_DATE);
    SELECT COUNT(*) INTO reviewed_that_day
      FROM review
     WHERE reviewer_username = NEW.reviewer_username
       AND review_date = NEW.review_date;
    IF reviewed_that_day >= 2 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'You can submit at most 2 reviews per day. Try again tomorrow.';
    END IF;
END$$

-- A user cannot review their own item.
CREATE TRIGGER trg_review_not_own_item
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM item
                WHERE item_id = NEW.item_id
                  AND seller_username = NEW.reviewer_username) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'You cannot review your own item.';
    END IF;
END$$

-- Once submitted, a review cannot be modified.
CREATE TRIGGER trg_review_read_only
BEFORE UPDATE ON review
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Reviews cannot be changed after they are submitted.';
END$$

-- Every item keeps at least one category.
-- Deleting a whole item still works: MySQL doesn't fire triggers for rows
-- removed by ON DELETE CASCADE.
CREATE TRIGGER trg_itemcategory_keep_one
BEFORE DELETE ON itemCategory
FOR EACH ROW
BEGIN
    IF (SELECT COUNT(*) FROM itemCategory WHERE item_id = OLD.item_id) <= 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'An item must keep at least one category.';
    END IF;
END$$

DELIMITER ;