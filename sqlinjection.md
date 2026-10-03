# SQL Injection Tests

## Login

| Username input | What it would do to vulnerable code |
|---|---|
| `' OR '1'='1` | Makes the WHERE clause always true, so it logs in as the first user |
| `' OR 1=1 -- ` | Same, and comments out the rest of the query, trailing space at the end |
| `yourRealUser' -- ` | Logs in as that user without checking the password |
| `' OR '1'='1' #` | Same as above, using MySQL's # comment |
| `' UNION SELECT 'fakehash' -- ` | Makes the query return a password hash the attacker chose |
| `'; DROP TABLE user; -- ` | Tries to run a second statement that deletes the table |

## Sign Up

| Field | Input |
|---|---|
| Username | `bob'); DROP TABLE user; --` |
| First name | `Robert'); DROP TABLE user;--` |
| Email | `x' OR '1'='1` |
| Last name | `a', 'b', 'c', 'd', 'e') --` |
