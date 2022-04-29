package com.paanini.jiffy.utils;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

public class SchemaUtils {
  public static void copy(final SpecificRecordBase source, final
  SpecificRecordBase target) {
    final List<Schema.Field> fieldList = source.getSchema().getFields();
    for (final Schema.Field field : fieldList) {
      target.put(field.pos(), source.get(field.pos()));
    }
  }
}
