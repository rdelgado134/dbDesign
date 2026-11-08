package com.db440;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cleans up and validates what the user typed into item forms.
 * Each method throws IllegalArgumentException with a message meant for the user.
 */
final class ItemInput {

    // Must match the column sizes in schema.sql.
    private static final int MAX_TITLE = 150;
    private static final int MAX_CATEGORY = 50;
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99"); // DECIMAL(10, 2)

    private ItemInput() {
    }

    static String title(String raw) {
        String title = raw.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Please enter a title");
        }
        if (title.length() > MAX_TITLE) {
            throw new IllegalArgumentException("Title is limited to " + MAX_TITLE + " characters");
        }
        return title;
    }

    static String description(String raw) {
        String description = raw.trim();
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Please enter a description");
        }
        return description;
    }

    static BigDecimal price(String raw) {
        String cleaned = raw.trim().replace("$", "").replace(",", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Please enter a price");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Price must be a number, e.g. 19.99");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        if (price.scale() > 2) {
            throw new IllegalArgumentException("Price can have at most two decimal places");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Price cannot be more than $99,999,999.99");
        }
        return price;
    }

    /**
     * Parses a comma-separated list such as "Electronics, gaming" into ["electronics", "gaming"].
     * Input is lowercased here so "Books" is accepted as "books" instead of rejected.
     * The schema's check (chk_category_lowercase_word) is case-sensitive, so the
     * database would refuse an uppercase category even if this step were skipped.
     */
    static List<String> categories(String raw) {
        Set<String> categories = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String category = part.trim().toLowerCase(Locale.ROOT);
            if (category.isEmpty()) {
                continue;
            }
            if (!category.matches("[a-z]+")) {
                throw new IllegalArgumentException("\"" + part.trim() + "\" is not a valid category. "
                    + "A category is a single word using only letters, e.g. electronics");
            }
            if (category.length() > MAX_CATEGORY) {
                throw new IllegalArgumentException("Categories are limited to " + MAX_CATEGORY + " letters");
            }
            categories.add(category);
        }
        if (categories.isEmpty()) {
            throw new IllegalArgumentException("Please enter at least one category");
        }
        return new ArrayList<>(categories);
    }

    static String category(String raw) {
        List<String> categories = categories(raw);
        if (categories.size() > 1) {
            throw new IllegalArgumentException("Please enter one category at a time");
        }
        return categories.get(0);
    }

}
