package com.paanini.jiffy.dto;

import com.option3.docube.schema.nodes.Status;
import com.option3.docube.schema.nodes.Type;

import java.util.ArrayList;
import java.util.List;

public class QueryOptions {
  private Type[] types;
  private String orderby;
  private String order;
  private Status status;
  private boolean nested;
  private String filterCharacter="";

  public QueryOptions() {
    setOrder("DESC");
    setOrderby("lastModified");
    setTypes(new Type[] {Type.ALL});
    setNested(false);
  }

  public Type[] getTypes() {
    return types;
  }

  public QueryOptions setTypes(Type ... types) {
    List<Type> typeOptions = new ArrayList<>();
    for(int i =0; i< types.length; i++) {
      if (types[i].equals(Type.DATASHEET)) {
        // Datasheet have 3 subtypes, csv, sql, kudu. Including all of them for querying
        typeOptions.add(Type.DATASHEET);
        typeOptions.add(Type.SQL_DATASHEET);
        typeOptions.add(Type.KUDU_DATASHEET);
        typeOptions.add(Type.SQL_APPENDABLE_DATASHEET);
      } else {
        typeOptions.add(types[i]);
      }
    }

    Type [] t = new Type[typeOptions.size()];
    this.types = typeOptions.toArray(t);
    return this;
  }

  public void setTypes(Type type) {
    if(type == null) return;
    if(type.equals(Type.DATASHEET)) {
      setTypes(type, Type.SQL_DATASHEET, Type.KUDU_DATASHEET, Type.SQL_APPENDABLE_DATASHEET);
    } else {
      setTypes(new Type[] {type});
    }
  }
  public QueryOptions setTypes(List<String> typeStrings) {
    List<Type> types = new ArrayList<>();
    for(int i =0; i< typeStrings.size(); i++) {
      Type t = Type.valueOf(typeStrings.get(i).toUpperCase());
      types.add(t);
    }
    Type [] t = new Type[types.size()];
    setTypes(types.toArray(t));
    return this;
  }

  public QueryOptions setTypes(String type) {
    if(type == null) return this;

    Type t = Type.valueOf(type.toUpperCase());
    if(t.equals(Type.DATASHEET)) {
      setTypes(t, Type.SQL_DATASHEET, Type.KUDU_DATASHEET, Type.SQL_APPENDABLE_DATASHEET);
    } else {
      setTypes(new Type[] {t});
    }
    return this;
  }

  public String getOrderby() {
    return orderby;
  }

  public QueryOptions setOrderby(String orderby) {
    orderby = orderby != null ? orderby : "lastModified";
    this.orderby = orderby;
    return this;
  }

  public String getOrder() {
    return order;
  }

  public QueryOptions setOrder(String order) {
    this.order = order != null ? order : "ASC";
    return this;
  }

  public boolean isNested() {
    return nested;
  }

  public QueryOptions setNested(boolean nested) {
    this.nested = nested;
    return this;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setFilterCharacter(String filterCharacter) {
    this.filterCharacter = filterCharacter;
  }

  public String getFilterCharacter() {
    return filterCharacter;
  }
}
