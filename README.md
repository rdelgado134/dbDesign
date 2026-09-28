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

      - windows users: run .\mvnw.cmd clean javafx:run

      - mac users: run ./mvnw clean javafx:run

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