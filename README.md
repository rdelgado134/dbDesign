# dbDesign
Project for COMP 440:Database Design


## Phase 2 ( Due Nov 14 )

- [ ] ER Design 
- [ ] Relational Schema
- [ ] Item Managament
- [ ] Category Management
- [ ] Review Management
- [ ] Advanced SQL Queries
   - [ ]Q1: Display the most expensive item(s) in each category.
   - [ ]Q2: Find users who posted at leaste two different items on the same day(different categories).
   - [ ]Q3:Display all items posted by a specified user that satisfy the following conditions.
   - [ ]Q4 Given a date entered by the user, display the user(s) who posted the largest number of items on that date.
   - [ ]Q5 Display all users who have submitted one or more reviews, and every review they have written is rated Poor.
   - [ ]Q6: Display all users whose posted items have never received a Poor review

Extend Phase 1 into a functional online marketplace database application.

Each item is identified by a unique **Item ID**, a **title**, a **description**, the **date posted**, a **price**, and one or more **categories**. Each category is represented by a single lowercase word (for example, electronics, books, or gaming).

Only registered users may **post**, **buy**, and **review** items. Each registered user is identified by a unique username (or user ID), password, first name, last name, email address, and phone number.

The following business rules must be enforced:

- A user may post at most two items per calendar day.
- A user may submit at most two reviews per calendar day.
- A user may submit only one review for a particular item.
- A user cannot review their own item.
- An item may receive zero or many reviews.
- Each review consists of:
   - A rating (Excellent, Good, Fair, or Poor)
   - A short comment
   - The review date
- Once submitted, a review cannot be modified.

Whenever possible, business rules should be enforced by the database (for example, using primary keys, foreign keys, UNIQUE constraints, CHECK constraints, triggers, stored procedures, or transactions). Rules that cannot reasonably be enforced by the database may be implemented in the application layer. Your documentation should clearly indicate where each business rule is enforced.

All functionality must be performed through application interface(UI).

## Phase 2 submission notes

**YouTube demo:** _(link)_

### Documents
- ER diagram and relational schema are submitted as separate PDFs (`ER_Diagram.pdf`, `Relational_Schema.pdf`). The relational schema PDF also contains the ER-to-table mapping and the rule-enforcement table.

### Running Phase 2
1. Rebuild the database so the new constraints and triggers are installed. In Workbench run
   `DROP DATABASE comp440;` then run all of `app/src/main/resources/sql/schema.sql`.
2. Load the demo data: run `app/src/main/resources/sql/seed.sql`. It is safe to re-run.
   Demo users `alice`, `bob`, `carol`, `dave`, `erin`, `frank` all use the password **`Password1!`**.
   The top of `seed.sql` lists the expected result of every query.
3. Start the app as usual (`.\mvnw.cmd clean javafx:run` / `./mvnw clean javafx:run` in `app/`).

Pages: **My Items** (post item, update price, delete, add/remove categories) ·
**Item Browser** (all items, search by category, **View Reviews** → read and write reviews) ·
**Project SQL Queries** (Queries 1–6).

### Where each business rule is enforced
| Business rule | Enforced by |
|---|---|
| Unique username, email, phone | DB: `PRIMARY KEY (username)`, `UNIQUE (email)`, `UNIQUE (phone)` |
| At most 2 items per user per calendar day | DB: trigger `trg_item_max_two_per_day` |
| Every item has 1+ categories | DB: item and categories inserted in one transaction; trigger `trg_itemcategory_keep_one` blocks removing the last one |
| Category is one lowercase word | DB: `CHECK chk_category_lowercase_word` (case-sensitive) |
| At most 2 reviews per user per calendar day | DB: trigger `trg_review_max_two_per_day` |
| One review per user per item | DB: `UNIQUE (reviewer_username, item_id)` |
| Cannot review your own item | DB: trigger `trg_review_not_own_item` |
| Rating is Excellent / Good / Fair / Poor | DB: `CHECK chk_review_rating` |
| Reviews cannot be modified | DB: trigger `trg_review_read_only`; the app has no edit option |
| Only the seller can update or delete an item | App: every `UPDATE`/`DELETE` includes `WHERE seller_username = ?` with the logged-in user |

When the database rejects a change, the app shows the trigger's or constraint's message
(e.g. "You can post at most 2 items per day").

### Design decisions
- **Review limit:** the assignment lists both 2 and 3 reviews per day; per the instructor we use **2**.
- **Categories** are a multivalued attribute of Item in the ER diagram, stored in `itemCategory`.
- **Deleting an item** also deletes its categories and reviews (`ON DELETE CASCADE`).
- **Query 2:** the two items must be different items. X and Y may be the same category
  (then the user needs two different items in that category on the same day).
- **Query 6:** lists users who have posted at least one item; users who never posted anything are not included.

## Phase 1 ( Due Oct 4th ) 

[Phase 1 demo video ](https://youtu.be/7HrhmnZ-Bko)

 1. Create a database schema. 
    - The schema of the user table should be:user(username, password, firstName, lastName, email, phone)
 2. UI pages
    - User registration
    - login

SQL injection should be prevented. 
Duplicate username, email and phone must be detected and signup should fail.

Unmatching passwords should be detected as well. 

## Developer tooling install
- JDK version 21 or newer.
   
- Maven:

      - windows users: run .\mvnw.cmd clean javafx:run

      - mac users: run ./mvnw clean javafx:run

   Run these from the `app/` folder.

## Database setup
The app connects to a local MySQL 8 server at `localhost:3306`, database `comp440`.

1. Install MySQL Server 8 and MySQL Workbench, and make sure the MySQL service is running.
2. In Workbench, connect to your local server as `root`, then **File → Open SQL Script...** and open
   `app/src/main/resources/sql/schema.sql`.
3. Run the whole script (lightning bolt / Ctrl+Shift+Enter). It creates the `comp440` database and the `user` table.
4. Check it with:

   ```sql
   DESCRIBE comp440.`user`;
   ```
   `username` should be `PRI`; `email` and `phone` should be `UNI`.

The script uses `IF NOT EXISTS`, so re-running it is safe, but it will not change an existing table.
To pick up schema changes, run `DROP DATABASE comp440;` first (this deletes all users).

### Database login
The app logs in to MySQL as `root` / `root` by default. If your MySQL login is different, set these
environment variables in the same terminal before running the app:

- windows users (PowerShell):

      $env:DB_USERNAME = "root"
      $env:DB_PASSWORD = "yourpassword"

- mac users:

      export DB_USERNAME=root
      export DB_PASSWORD=yourpassword

### Troubleshooting
- **Access denied for user 'root'**: wrong MySQL password; set `DB_PASSWORD` as above.
- **Unknown database 'comp440'** or **Table ... doesn't exist**: run `schema.sql` (see above).
- **Public Key Retrieval is not allowed**: already handled by `allowPublicKeyRetrieval=true` in the
  connection URL in `JdbcDao.java`; make sure you have the latest code.
- **Database Error!** alert in the app: check the terminal for the underlying SQL error.

## Security notes
- **SQL injection**: every query in `JdbcDao.java` is a fixed string with `?` placeholders, and user input is
  only bound through `PreparedStatement.setString`, so input is always treated as data, never as SQL.
- **Hashed passwords**: `PasswordHasher.java` hashes passwords with PBKDF2-HMAC-SHA256 (210,000 iterations,
  random 16-byte salt per user). The `password` column stores `iterations:salt:hash`, never the plain password.
- **Login errors**: a wrong username and a wrong password show the same message, so the form doesn't reveal
  which usernames exist.

### Project directory structure
This project follows the standard Maven directory layout. Here is a quick overview of what each folder does:

```text
app/
├── .mvn/                  # Maven Wrapper files (ensures everyone uses the same Maven version)
├── src/
│   └── main/
│       ├── java/          # All Java source code (.java files)
│       └── resources/     # Non-code assets (images, CSS styles, SQL scripts, FXML if used)
├── target/                # Automatically generated build output (ignored by Git)
├── mvnw / mvnw.cmd        # Wrapper executable scripts for running the project
└── pom.xml                # Main project configuration (dependencies, plugins, database drivers)