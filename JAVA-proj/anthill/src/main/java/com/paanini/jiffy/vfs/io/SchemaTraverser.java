package com.paanini.jiffy.vfs.io;

import com.paanini.jiffy.vfs.schema.RecordSchema;
import org.apache.avro.Schema;

/**
 * Traverse the Schema structure and initiates operations
 *
 * @author  Priyanka Bhoir
 * @since   5/8/19
 * */
public class SchemaTraverser {
  private final SchemaHandler schemaHandler;
  private Schema schema;

  public SchemaTraverser(Schema schema, SchemaHandler schemaHandler) {
    this.schemaHandler = schemaHandler;
    this.schema = schema;
  }

  public boolean traverse() {
    RecordSchema recordSchema = RecordSchema.RecordSchemaBuilder.from(this.schema);
    schemaHandler.startRecord(recordSchema);
    recordSchema.getFieldList().forEach(schemaHandler::processField);
    return schemaHandler.endRecord(recordSchema);
  }
}
