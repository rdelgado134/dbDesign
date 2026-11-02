package com.db440;

import java.util.List;

public class QueryResult {
    private final List<String> columns;
    private final List<List<String>> rows;

    public QueryResult(List<String> columns, List<List<String>> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public List<String> getColumns() {
        return columns;
    }
    
    public List<List<String>> getRows() {
        return rows;
    }
}