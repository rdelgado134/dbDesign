# dbDesign
Project for COMP 440:Database Design

## Phase 1 ( Due Oct 4th )
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

      - windows users: run .\mvnw clean javafx:run

      - mac users: run ./mvnw clean javafx:run