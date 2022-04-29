package com.paanini.jiffy.jcrquery;

import com.option3.docube.schema.nodes.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Athul Krishna N S
 * @since 30/04/20
 */
public class QueryModel {

  private List<String> projections;
  private List<Filter> filters;
  private Order order;
  private Integer limit;
  private Integer offset;

  public QueryModel(List<String> projections, List<Filter> filters,
                    Order order, Integer limit, Integer offset) {
    this.projections = projections;
    this.filters = filters;
    this.order = order;
    this.limit = limit;
    this.offset = offset;
  }

  public QueryModel() {
    this.projections = new ArrayList<>();
    this.filters = new ArrayList<>();
    this.order = new Order("name",SortOrder.ASC);
    this.limit = null;
    this.offset = null;
  }

  public List<String> getProjections() {
    return projections;
  }

  public void setProjections(List<String> projections) {
    this.projections = projections;
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public void setFilters(List<Filter> filters) {
    this.filters = filters;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public static Filter getTypeFilter(Type type) {
    return new Filter("type", Operator.EQUAL, type.name());
  }

  public static QueryModel getFilterModel(List<Filter> filters) {
    return new QueryModel(null, filters,
            new Order("name", SortOrder.ASC),
            null, null);
  }

  public QueryModel addTypeFilter(Type type) {
    if (Objects.nonNull(type)) {
      filters.add(new Filter("type", Operator.EQUAL, type.name()));
    }
    return this;
  }

  public QueryModel addFilter(String column, String value) {
    if (Objects.nonNull(value)) {
      filters.add(new Filter(column, Operator.EQUAL, value));
    }
    return this;
  }
}
