package com.paanini.jiffy.jcrquery;

/**
 * @author Athul Krishna N S
 * @since 30/04/20
 */
public class Filter {

    private String column;
    private Operator operator;
    private Object value;

    public Filter(String column, Operator operator, Object value) {
        this.column = column;
        this.operator = operator;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
