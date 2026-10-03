package com.db440;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcDao {

    // Defaults match a local MySQL with root/root. Set DB_USERNAME / DB_PASSWORD
    // environment variables to override without editing this file.
    // allowPublicKeyRetrieval lets MySQL 8's default caching_sha2_password login work
    // without SSL. Fine for a local dev database.
    private static final String DATABASE_URL =
        "jdbc:mysql://localhost:3306/comp440?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DATABASE_USERNAME = envOrDefault("DB_USERNAME", "root");
    private static final String DATABASE_PASSWORD = envOrDefault("DB_PASSWORD", "root");
    private static final String INSERT_QUERY = "INSERT INTO registration (full_name, email_id, password) VALUES (?, ?, ?)";

    // All user queries are fixed strings with ? placeholders; user input is only
    // ever bound through PreparedStatement.setString, which prevents SQL injection.
    private static final String INSERT_USER_QUERY =
        "INSERT INTO `user` (username, password, firstName, lastName, email, phone) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String USERNAME_EXISTS_QUERY = "SELECT 1 FROM `user` WHERE username = ?";
    private static final String EMAIL_EXISTS_QUERY = "SELECT 1 FROM `user` WHERE email = ?";
    private static final String PHONE_EXISTS_QUERY = "SELECT 1 FROM `user` WHERE phone = ?";
    private static final String FIND_PASSWORD_HASH_QUERY = "SELECT password FROM `user` WHERE username = ?";

    // Every query goes through here. Callers must close the connection,
    // so always use it in a try-with-resources block.
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
    }

    /**
     * Hashes the password and inserts a new user.
     * Throws SQLIntegrityConstraintViolationException if the username, email or
     * phone is already taken, as a backstop to the *Exists checks.
     */
    public void insertUser(String username, String rawPassword, String firstName, String lastName,
                           String email, String phone) throws SQLException {
        String passwordHash = PasswordHasher.hash(rawPassword.toCharArray());
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_USER_QUERY)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, passwordHash);
            preparedStatement.setString(3, firstName);
            preparedStatement.setString(4, lastName);
            preparedStatement.setString(5, email);
            preparedStatement.setString(6, phone);
            preparedStatement.executeUpdate();
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        return exists(USERNAME_EXISTS_QUERY, username);
    }

    public boolean emailExists(String email) throws SQLException {
        return exists(EMAIL_EXISTS_QUERY, email);
    }

    public boolean phoneExists(String phone) throws SQLException {
        return exists(PHONE_EXISTS_QUERY, phone);
    }

    // Returns the stored hash for login, or empty if no such user exists.
    public Optional<String> findPasswordHash(String username) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(FIND_PASSWORD_HASH_QUERY)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? Optional.of(resultSet.getString("password")) : Optional.empty();
            }
        }
    }

    private boolean exists(String query, String value) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, value);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * @deprecated Template leftover that writes to a table not in comp440.
     * Use {@link #insertUser} instead; remove once RegistrationController is updated.
     */
    @Deprecated
    public void insertRecord(String fullName, String emailId, String password) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_QUERY)) {
            preparedStatement.setString(1, fullName);
            preparedStatement.setString(2, emailId);
            preparedStatement.setString(3, password);
            preparedStatement.executeUpdate();
        }
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    public static void printSQLException(SQLException ex) {
        for (Throwable e: ex) {
            if (e instanceof SQLException) {
                e.printStackTrace(System.err);
                System.err.println("SQLState: " + ((SQLException) e).getSQLState());
                System.err.println("Error Code: " + ((SQLException) e).getErrorCode());
                System.err.println("Message: " + e.getMessage());
                Throwable t = ex.getCause();
                while (t != null) {
                    System.out.println("Cause: " + t);
                    t = t.getCause();
                }
            }
        }
    }

}
