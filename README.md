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