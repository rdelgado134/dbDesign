package com.db440;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One row of the item table together with its categories. */
public final class Item {

    private final int itemId;
    private final String seller;
    private final String title;
    private final String description;
    private final LocalDate datePosted;
    private final BigDecimal price;
    private final List<String> categories;

    public Item(int itemId, String seller, String title, String description, LocalDate datePosted,
                BigDecimal price, List<String> categories) {
        this.itemId = itemId;
        this.seller = seller;
        this.title = title;
        this.description = description;
        this.datePosted = datePosted;
        this.price = price;
        this.categories = List.copyOf(categories);
    }

    public int getItemId() {
        return itemId;
    }

    public String getSeller() {
        return seller;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDatePosted() {
        return datePosted;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public List<String> getCategories() {
        return categories;
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    /**
     * Craigslist-style line: seller[cat1,cat2](title): datePosted, $price *
     * The trailing * means the item has a description, shown when the listing is clicked.
     */
    public String listingText() {
        String text = seller + "[" + String.join(",", categories) + "](" + title + "): "
            + datePosted + ", " + String.format("$%,.2f", price);
        return hasDescription() ? text + " *" : text;
    }

}
