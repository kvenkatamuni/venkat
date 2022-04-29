package com.paanini.jiffy.jcrquery;

/**
 * @author Athul Krishna N S
 * @since 30/04/20
 */
public class Order {

    private String column;
    private SortOrder sortOrder;

    public Order(String column, SortOrder sortOrder) {
        this.column = column;
        this.sortOrder = sortOrder;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

}
