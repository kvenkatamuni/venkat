package com.paanini.jiffy.vfs.api;

import org.apache.avro.Schema;

public interface Persistable extends Visitable {
  Schema getFileSchema();
  void setValue(int field, java.lang.Object value);
  void setValue(String fieldName, java.lang.Object value);
  Object getValue(String fieldName);
}
