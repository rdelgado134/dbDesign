-- COMP 440 Phase 2 demo data
-- Run AFTER schema.sql. Safe to re-run: it first removes only the demo users and their data.
-- Every demo user's password is:  Password1!
--
-- Expected query results with this data:
--   Q1  electronics: Gaming Laptop + Smartphone (tie at $1200); books: Fantasy Trilogy;
--       gaming: Gaming Laptop; home: Coffee Maker; sports: Tennis Racket
--   Q2  electronics + books -> alice;  electronics + gaming -> bob;  books + sports -> dave
--   Q3  alice -> Gaming Laptop only (Mystery Novel Set has a Poor review, Headphones has none)
--   Q4  2026-10-20 -> alice, bob (tie);  2026-10-21 -> dave;  2026-10-22 -> alice, bob, carol, erin
--   Q5  erin (both of her reviews are Poor; dave also wrote a Poor review but not only Poor)
--   Q6  bob, dave, erin (erin's items have no reviews at all; frank posted nothing)

USE comp440;

DELETE FROM review WHERE reviewer_username IN ('alice', 'bob', 'carol', 'dave', 'erin', 'frank');
DELETE FROM item   WHERE seller_username   IN ('alice', 'bob', 'carol', 'dave', 'erin', 'frank');  -- also removes their categories and reviews
DELETE FROM `user` WHERE username          IN ('alice', 'bob', 'carol', 'dave', 'erin', 'frank');

INSERT INTO `user` (username, password, firstName, lastName, email, phone) VALUES
    ('alice', '210000:w5aUWIJw7P7TUAOAfGs3vw==:C5otv5wxEZhJm0Rmm+xcf81KJ/6wj9V4jxgk8h6eBzE=', 'Alice', 'Nguyen', 'alice@example.com', '8185550101'),
    ('bob', '210000:87hfh5ASDnUaO5XfVC/Gjw==:HW2VdNDYC/ephZ/+zCVrWH4HfDLyi6ZJm/XzNsapjO8=', 'Bob', 'Martinez', 'bob@example.com', '8185550102'),
    ('carol', '210000:vtHN17gorpUUUBLzEfUdRQ==:7f5+ByljnoAyrexviGnGLPBaQ85ToA10775c7riYurQ=', 'Carol', 'Kim', 'carol@example.com', '8185550103'),
    ('dave', '210000:lRiZ91ro9hHJX9M/uHX30w==:TgKVMBF1E9W9rJGmR/iZfP1hA8Y+sX01ZQ52qkqJQXc=', 'Dave', 'Patel', 'dave@example.com', '8185550104'),
    ('erin', '210000:VOOaWkr7OAQimQrrQRJXpw==:gN+ZOosVTK7BLpDQwKuzaGZerNbz1YzKo9DeYvMYQLU=', 'Erin', 'Johnson', 'erin@example.com', '8185550105'),
    ('frank', '210000:8kYNAHvHB+3Vhb+L0kl63w==:LaeXCZfXSVnvVQcIPV0JQmGLqRdSEuQ7ON1LDIiVxJE=', 'Frank', 'Lopez', 'frank@example.com', '8185550106');

-- At most 2 items per seller per date, so the daily-limit trigger accepts every row.
INSERT INTO item (item_id, seller_username, title, description, date_posted, price) VALUES
    (1001, 'alice', 'Gaming Laptop', 'RTX 4070, 16GB RAM, barely used.', '2026-10-20', 1200.00),
    (1002, 'alice', 'Mystery Novel Set', 'Five paperback mysteries in good condition.', '2026-10-20', 40.00),
    (1003, 'bob', 'Mechanical Keyboard', 'Brown switches, RGB backlight.', '2026-10-20', 120.00),
    (1004, 'bob', 'Board Game Bundle', 'Catan, Ticket to Ride and Azul.', '2026-10-20', 60.00),
    (1005, 'carol', 'Desk Lamp', 'LED lamp with adjustable arm.', '2026-10-20', 35.00),
    (1006, 'carol', 'Smartphone', 'Unlocked, 256GB, includes case.', '2026-10-21', 1200.00),
    (1007, 'dave', 'Fantasy Trilogy', 'Hardcover box set.', '2026-10-21', 55.00),
    (1008, 'dave', 'Yoga Mat', 'Non-slip, 6mm thick.', '2026-10-21', 25.00),
    (1009, 'erin', 'Coffee Maker', '12-cup drip coffee maker.', '2026-10-21', 80.00),
    (1010, 'erin', 'Tennis Racket', 'Lightweight graphite frame.', '2026-10-22', 150.00),
    (1011, 'bob', 'Cookbook', 'Weeknight dinners, like new.', '2026-10-22', 30.00),
    (1012, 'alice', 'Headphones', 'Noise cancelling, over-ear.', '2026-10-22', 200.00),
    (1013, 'carol', 'Chess Set', 'Wooden board with weighted pieces.', '2026-10-22', 60.00),
    (1014, 'dave', 'Basketball', 'Official size, indoor/outdoor.', '2026-10-23', 30.00);

INSERT INTO itemCategory (item_id, category_name) VALUES
    (1001, 'electronics'),
    (1001, 'gaming'),
    (1002, 'books'),
    (1003, 'electronics'),
    (1004, 'gaming'),
    (1005, 'home'),
    (1006, 'electronics'),
    (1007, 'books'),
    (1008, 'sports'),
    (1009, 'home'),
    (1010, 'sports'),
    (1011, 'books'),
    (1011, 'home'),
    (1012, 'electronics'),
    (1013, 'gaming'),
    (1014, 'sports');

-- At most 2 reviews per reviewer per date, never on the reviewer's own item.
INSERT INTO review (reviewer_username, item_id, rating, comment, review_date) VALUES
    ('bob', 1001, 'Excellent', 'Runs every game I tried.', '2026-10-24'),
    ('carol', 1001, 'Good', 'Great laptop, a little heavy.', '2026-10-24'),
    ('dave', 1002, 'Excellent', 'Loved every one of them.', '2026-10-24'),
    ('erin', 1002, 'Poor', 'Two books had water damage.', '2026-10-24'),
    ('frank', 1003, 'Good', 'Nice feel, loud keys.', '2026-10-24'),
    ('frank', 1007, 'Fair', 'Corners were worn.', '2026-10-24'),
    ('erin', 1006, 'Poor', 'Battery drains fast.', '2026-10-25'),
    ('dave', 1006, 'Good', 'Works fine for me.', '2026-10-25'),
    ('alice', 1004, 'Good', 'Fun bundle, all pieces there.', '2026-10-25'),
    ('frank', 1011, 'Excellent', 'Every recipe is a winner.', '2026-10-25'),
    ('dave', 1013, 'Poor', 'Missing a knight.', '2026-10-26');
