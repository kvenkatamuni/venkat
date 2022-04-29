package com.paanini.jiffy.vfs.schema;

import org.apache.avro.Schema;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Priyanka Bhoir on 29/7/19
 */
public class RecordSchema {
  private final List<FieldSchema> fieldList;

  private RecordSchema(List<FieldSchema> fieldList) {
    this.fieldList  = fieldList;
  }

  public List<FieldSchema> getFieldList() {
    return fieldList;
  }

  public static class RecordSchemaBuilder{
    public static RecordSchema from(Schema schema){
      List<FieldSchema> fieldList = schema.getFields().stream()
              .map(s -> FieldSchema.FieldSchemaBuilder.from(s))
              .collect(Collectors.toList());
      return new RecordSchema(fieldList);
    }
  }
}
