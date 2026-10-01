package com.db440;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcDao {

    // Defaults match a local MySQL with root/root. Set DB_USERNAME / DB_PASSWORD
    // environment variables to override without editing this file.
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/comp440?useSSL=false";
    private static final String DATABASE_USERNAME = envOrDefault("DB_USERNAME", "root");
    private static final String DATABASE_PASSWORD = envOrDefault("DB_PASSWORD", "root");
    private static final String INSERT_QUERY = "INSERT INTO registration (full_name, email_id, password) VALUES (?, ?, ?)";

    // Every query goes through here. Callers must close the connection,
    // so always use it in a try-with-resources block.
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
    }

    // Placeholder from the template; replaced by insertUser in the user DAO commit.
    // Exceptions propagate so the caller can tell the user the insert failed.
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
