package com.db440;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    // All user queries are fixed strings with ? placeholders; user input is only
    // ever bound through PreparedStatement.setString, which prevents SQL injection.
    private static final String INSERT_USER_QUERY =
        "INSERT INTO `user` (username, password, firstName, lastName, email, phone) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String USERNAME_EXISTS_QUERY = "SELECT 1 FROM `user` WHERE username = ?";
    private static final String EMAIL_EXISTS_QUERY = "SELECT 1 FROM `user` WHERE email = ?";
    private static final String PHONE_EXISTS_QUERY = "SELECT 1 FROM `user` WHERE phone = ?";
    private static final String FIND_PASSWORD_HASH_QUERY = "SELECT password FROM `user` WHERE username = ?";

    // Each listing row comes back with its categories joined as "books,gaming".
    private static final String SELECT_ITEMS =
        "SELECT i.item_id, i.seller_username, i.title, i.description, i.date_posted, i.price, "
            + "GROUP_CONCAT(c.category_name ORDER BY c.category_name SEPARATOR ',') AS categories "
            + "FROM item i LEFT JOIN itemCategory c ON c.item_id = i.item_id ";
    private static final String GROUP_AND_ORDER_ITEMS =
        " GROUP BY i.item_id ORDER BY i.date_posted DESC, i.item_id DESC";
    private static final String FIND_ALL_ITEMS_QUERY = SELECT_ITEMS + GROUP_AND_ORDER_ITEMS;
    private static final String FIND_ITEMS_BY_SELLER_QUERY =
        SELECT_ITEMS + "WHERE i.seller_username = ?" + GROUP_AND_ORDER_ITEMS;
    private static final String FIND_ITEMS_BY_CATEGORY_QUERY =
        SELECT_ITEMS + "WHERE i.item_id IN (SELECT item_id FROM itemCategory WHERE category_name = ?)"
            + GROUP_AND_ORDER_ITEMS;

    private static final String INSERT_ITEM_QUERY =
        "INSERT INTO item (seller_username, title, description, price) VALUES (?, ?, ?, ?)";
    private static final String INSERT_ITEM_CATEGORY_QUERY =
        "INSERT INTO itemCategory (item_id, category_name) VALUES (?, ?)";

    // Every change to an existing item also matches seller_username, so a user
    // can only change their own items no matter what item_id is passed in.
    private static final String UPDATE_ITEM_PRICE_QUERY =
        "UPDATE item SET price = ? WHERE item_id = ? AND seller_username = ?";
    private static final String DELETE_ITEM_REVIEWS_QUERY =
        "DELETE r FROM review r JOIN item i ON i.item_id = r.item_id WHERE r.item_id = ? AND i.seller_username = ?";
    private static final String DELETE_ITEM_QUERY = "DELETE FROM item WHERE item_id = ? AND seller_username = ?";
    private static final String LOCK_OWN_ITEM_QUERY =
        "SELECT 1 FROM item WHERE item_id = ? AND seller_username = ? FOR UPDATE";
    private static final String COUNT_ITEM_CATEGORIES_QUERY = "SELECT COUNT(*) FROM itemCategory WHERE item_id = ?";
    private static final String DELETE_ITEM_CATEGORY_QUERY =
        "DELETE FROM itemCategory WHERE item_id = ? AND category_name = ?";

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

    public List<Item> findAllItems() throws SQLException {
        return findItems(FIND_ALL_ITEMS_QUERY);
    }

    public List<Item> findItemsBySeller(String seller) throws SQLException {
        return findItems(FIND_ITEMS_BY_SELLER_QUERY, seller);
    }

    // Returns the matching items with all of their categories, not just the one searched for.
    public List<Item> findItemsByCategory(String category) throws SQLException {
        return findItems(FIND_ITEMS_BY_CATEGORY_QUERY, category);
    }

    /**
     * Inserts an item and its categories as one transaction, so an item is never
     * saved without categories. date_posted is filled in by the database.
     */
    public void insertItem(String seller, String title, String description, BigDecimal price,
                           List<String> categories) throws SQLException {
        inTransaction(connection -> {
            int itemId;
            try (PreparedStatement preparedStatement =
                     connection.prepareStatement(INSERT_ITEM_QUERY, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, seller);
                preparedStatement.setString(2, title);
                preparedStatement.setString(3, description);
                preparedStatement.setBigDecimal(4, price);
                preparedStatement.executeUpdate();
                try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                    keys.next();
                    itemId = keys.getInt(1);
                }
            }
            insertCategories(connection, itemId, categories);
            return null;
        });
    }

    /** Returns false if the item doesn't exist or doesn't belong to seller. */
    public boolean updateItemPrice(int itemId, String seller, BigDecimal price) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_ITEM_PRICE_QUERY)) {
            preparedStatement.setBigDecimal(1, price);
            preparedStatement.setInt(2, itemId);
            preparedStatement.setString(3, seller);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    /**
     * Deletes one of seller's items along with its reviews. Its categories are
     * removed by ON DELETE CASCADE. Returns false if the item isn't seller's.
     */
    public boolean deleteItem(int itemId, String seller) throws SQLException {
        return inTransaction(connection -> {
            try (PreparedStatement deleteReviews = connection.prepareStatement(DELETE_ITEM_REVIEWS_QUERY);
                PreparedStatement deleteItem = connection.prepareStatement(DELETE_ITEM_QUERY)) {
                deleteReviews.setInt(1, itemId);
                deleteReviews.setString(2, seller);
                deleteReviews.executeUpdate();
                deleteItem.setInt(1, itemId);
                deleteItem.setString(2, seller);
                return deleteItem.executeUpdate() > 0;
            }
        });
    }

    /** Adds categories to one of seller's items. Returns false if the item isn't seller's. */
    public boolean addCategories(int itemId, String seller, List<String> categories) throws SQLException {
        return inTransaction(connection -> {
            if (!lockOwnItem(connection, itemId, seller)) {
                return false;
            }
            insertCategories(connection, itemId, categories);
            return true;
        });
    }

    /**
     * Removes a category from one of seller's items. Returns false if the item isn't
     * seller's, or if this is its last category, since every item needs at least one.
     */
    public boolean removeCategory(int itemId, String seller, String category) throws SQLException {
        return inTransaction(connection -> {
            if (!lockOwnItem(connection, itemId, seller)) {
                return false;
            }
            try (PreparedStatement count = connection.prepareStatement(COUNT_ITEM_CATEGORIES_QUERY)) {
                count.setInt(1, itemId);
                try (ResultSet resultSet = count.executeQuery()) {
                    resultSet.next();
                    if (resultSet.getInt(1) <= 1) {
                        return false;
                    }
                }
            }
            try (PreparedStatement delete = connection.prepareStatement(DELETE_ITEM_CATEGORY_QUERY)) {
                delete.setInt(1, itemId);
                delete.setString(2, category);
                return delete.executeUpdate() > 0;
            }
        });
    }

    private List<Item> findItems(String query, String... parameters) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Item> items = new ArrayList<>();
                while (resultSet.next()) {
                    String categories = resultSet.getString("categories");
                    items.add(new Item(
                        resultSet.getInt("item_id"),
                        resultSet.getString("seller_username"),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getObject("date_posted", LocalDate.class),
                        resultSet.getBigDecimal("price"),
                        categories == null ? List.of() : Arrays.asList(categories.split(","))));
                }
                return items;
            }
        }
    }

    private static void insertCategories(Connection connection, int itemId, List<String> categories)
            throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_ITEM_CATEGORY_QUERY)) {
            for (String category : categories) {
                preparedStatement.setInt(1, itemId);
                preparedStatement.setString(2, category);
                preparedStatement.executeUpdate();
            }
        }
    }

    // Locks the item's row until the transaction ends, so two category changes
    // to the same item can't interleave. Returns false if it isn't seller's item.
    private static boolean lockOwnItem(Connection connection, int itemId, String seller) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(LOCK_OWN_ITEM_QUERY)) {
            preparedStatement.setInt(1, itemId);
            preparedStatement.setString(2, seller);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }

    // Commits if work finishes normally and rolls back if it throws.
    private <T> T inTransaction(TransactionWork<T> work) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
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

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    // ---------------------------------------------------------------------
    // Reviews (Part 5). The daily limit, no-self-review and read-only rules are
    // enforced by triggers in schema.sql; one review per item is a UNIQUE key.
    // ---------------------------------------------------------------------

    private static final String INSERT_REVIEW_QUERY =
        "INSERT INTO review (reviewer_username, item_id, rating, comment) VALUES (?, ?, ?, ?)";
    private static final String FIND_REVIEWS_FOR_ITEM_QUERY =
        "SELECT review_id, reviewer_username, item_id, rating, comment, review_date "
            + "FROM review WHERE item_id = ? ORDER BY review_date DESC, review_id DESC";
    private static final String HAS_REVIEWED_QUERY =
        "SELECT 1 FROM review WHERE reviewer_username = ? AND item_id = ?";

    /** review_date is filled in by the database (today). */
    public void insertReview(String reviewer, int itemId, String rating, String comment) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_REVIEW_QUERY)) {
            preparedStatement.setString(1, reviewer);
            preparedStatement.setInt(2, itemId);
            preparedStatement.setString(3, rating);
            preparedStatement.setString(4, comment);
            preparedStatement.executeUpdate();
        }
    }

    public List<Review> findReviewsForItem(int itemId) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(FIND_REVIEWS_FOR_ITEM_QUERY)) {
            preparedStatement.setInt(1, itemId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (resultSet.next()) {
                    reviews.add(new Review(
                        resultSet.getInt("review_id"),
                        resultSet.getString("reviewer_username"),
                        resultSet.getInt("item_id"),
                        resultSet.getString("rating"),
                        resultSet.getString("comment"),
                        resultSet.getObject("review_date", LocalDate.class)));
                }
                return reviews;
            }
        }
    }

    public boolean hasReviewed(String reviewer, int itemId) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(HAS_REVIEWED_QUERY)) {
            preparedStatement.setString(1, reviewer);
            preparedStatement.setInt(2, itemId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Returns a message for the user when the database rejected a change because of
     * a business rule, or null if the error was something else (e.g. no connection).
     */
    public static String ruleViolationMessage(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if ("45000".equals(e.getSQLState())) {
            return message; // raised by a trigger in schema.sql with a readable MESSAGE_TEXT
        }
        if (e.getErrorCode() == 1062 && message.contains("uq_review_one_per_item")) {
            return "You have already reviewed this item. Each user can review an item only once.";
        }
        if (e.getErrorCode() == 3819) { // CHECK constraint failed
            if (message.contains("chk_review_rating")) {
                return "Rating must be Excellent, Good, Fair or Poor.";
            }
            if (message.contains("chk_category_lowercase_word")) {
                return "A category must be a single lowercase word, e.g. books.";
            }
            if (message.contains("chk_item_price")) {
                return "Price cannot be negative.";
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Advanced SQL queries (Part 6). Column aliases become the table headers.
    // ---------------------------------------------------------------------

    // Q1: most expensive item(s) in each category; ties all show up because every
    // item whose price equals its category's maximum is returned.
    private static final String QUERY_1 =
        "SELECT c.category_name AS `Category`, i.item_id AS `Item ID`, i.title AS `Title`, "
            + "i.seller_username AS `Seller`, i.price AS `Price` "
            + "FROM item i JOIN itemCategory c ON c.item_id = i.item_id "
            + "WHERE i.price = (SELECT MAX(i2.price) FROM item i2 "
            + "                 JOIN itemCategory c2 ON c2.item_id = i2.item_id "
            + "                 WHERE c2.category_name = c.category_name) "
            + "ORDER BY c.category_name, i.title";

    // Q2: same seller, same day, two different items, one in category X and one in Y.
    private static final String QUERY_2 =
        "SELECT i1.seller_username AS `Username`, i1.date_posted AS `Date Posted`, "
            + "i1.title AS `Item in Category X`, i2.title AS `Item in Category Y` "
            + "FROM item i1 "
            + "JOIN itemCategory c1 ON c1.item_id = i1.item_id AND c1.category_name = ? "
            + "JOIN item i2 ON i2.seller_username = i1.seller_username "
            + "            AND i2.date_posted = i1.date_posted "
            + "            AND i2.item_id <> i1.item_id "
            + "JOIN itemCategory c2 ON c2.item_id = i2.item_id AND c2.category_name = ? "
            + "ORDER BY `Username`, `Date Posted`, `Item in Category X`";

    // Q3: the user's items that have at least one review and no review below Good.
    private static final String QUERY_3 =
        "SELECT i.item_id AS `Item ID`, i.title AS `Title`, i.price AS `Price`, "
            + "i.date_posted AS `Date Posted`, "
            + "(SELECT COUNT(*) FROM review r WHERE r.item_id = i.item_id) AS `Reviews` "
            + "FROM item i "
            + "WHERE i.seller_username = ? "
            + "AND EXISTS (SELECT 1 FROM review r WHERE r.item_id = i.item_id) "
            + "AND NOT EXISTS (SELECT 1 FROM review r WHERE r.item_id = i.item_id "
            + "                AND r.rating NOT IN ('Excellent', 'Good')) "
            + "ORDER BY i.title";

    // Q4: everyone whose count on that date equals the highest count (so ties all show up).
    private static final String QUERY_4 =
        "SELECT i.seller_username AS `Username`, COUNT(*) AS `Items Posted` "
            + "FROM item i WHERE i.date_posted = ? "
            + "GROUP BY i.seller_username "
            + "HAVING COUNT(*) = (SELECT MAX(t.posted) FROM "
            + "                   (SELECT COUNT(*) AS posted FROM item "
            + "                    WHERE date_posted = ? GROUP BY seller_username) t) "
            + "ORDER BY `Username`";

    // Q5: has written a review, and no review they wrote is anything other than Poor.
    private static final String QUERY_5 =
        "SELECT u.username AS `Username`, u.firstName AS `First Name`, u.lastName AS `Last Name`, "
            + "(SELECT COUNT(*) FROM review r WHERE r.reviewer_username = u.username) AS `Reviews Written` "
            + "FROM `user` u "
            + "WHERE EXISTS (SELECT 1 FROM review r WHERE r.reviewer_username = u.username) "
            + "AND NOT EXISTS (SELECT 1 FROM review r WHERE r.reviewer_username = u.username "
            + "                AND r.rating <> 'Poor') "
            + "ORDER BY u.username";

    // Q6: has posted at least one item, and none of their items has a Poor review.
    // Items with no reviews pass automatically. Users who never posted are left out.
    private static final String QUERY_6 =
        "SELECT u.username AS `Username`, u.firstName AS `First Name`, u.lastName AS `Last Name`, "
            + "(SELECT COUNT(*) FROM item i WHERE i.seller_username = u.username) AS `Items Posted` "
            + "FROM `user` u "
            + "WHERE EXISTS (SELECT 1 FROM item i WHERE i.seller_username = u.username) "
            + "AND NOT EXISTS (SELECT 1 FROM item i JOIN review r ON r.item_id = i.item_id "
            + "                WHERE i.seller_username = u.username AND r.rating = 'Poor') "
            + "ORDER BY u.username";

    public QueryResult executeQuery1() throws SQLException {
        return runReport(QUERY_1);
    }

    public QueryResult executeQuery2(String category1, String category2) throws SQLException {
        return runReport(QUERY_2, category1, category2);
    }

    public QueryResult executeQuery3(String username) throws SQLException {
        return runReport(QUERY_3, username);
    }

    public QueryResult executeQuery4(LocalDate date) throws SQLException {
        java.sql.Date sqlDate = java.sql.Date.valueOf(date);
        return runReport(QUERY_4, sqlDate, sqlDate);
    }

    public QueryResult executeQuery5() throws SQLException {
        return runReport(QUERY_5);
    }

    public QueryResult executeQuery6() throws SQLException {
        return runReport(QUERY_6);
    }

    // Runs a read-only query and returns its column labels and rows as text for the results table.
    private QueryResult runReport(String query, Object... parameters) throws SQLException {
        try (Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < parameters.length; i++) {
                preparedStatement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> columns = new ArrayList<>();
                for (int column = 1; column <= columnCount; column++) {
                    columns.add(metaData.getColumnLabel(column));
                }
                List<List<String>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    List<String> row = new ArrayList<>();
                    for (int column = 1; column <= columnCount; column++) {
                        String value = resultSet.getString(column);
                        row.add(value == null ? "" : value);
                    }
                    rows.add(row);
                }
                return new QueryResult(columns, rows);
            }
        }
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
