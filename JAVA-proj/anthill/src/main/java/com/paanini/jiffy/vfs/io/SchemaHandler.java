package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.vfs.schema.FieldSchema;
import com.paanini.jiffy.vfs.schema.RecordSchema;

/**
 * Handler for processing a schema structure.
 *
 * Created by Priyanka Bhoir on 1/8/19
 */
public interface SchemaHandler {

  /**
   * Invoked during start of record visit.
   * @param schema
   */
  void startRecord(RecordSchema schema);

  /**
   * Invoked after visiting all fields of a record.
   * @param schema
   */
  boolean endRecord(RecordSchema schema);

  /**
   * Invoked for each field in schema.
   * @param schema
   */
  void processField(FieldSchema schema);
}

