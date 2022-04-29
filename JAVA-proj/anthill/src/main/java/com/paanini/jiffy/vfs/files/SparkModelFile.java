package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.SparkModelFileSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

public class SparkModelFile extends SparkModelFileSchema implements ExtraFileProps, BasicFileProps,
        Persistable, Exportable {

  private String path;
  private AccessEntry[] privileges;
  private String parentId;

  public SparkModelFile(){
    setType(Type.SPARK_MODEL_FILE);
  }

  public SparkModelFile(final SpecificRecordBase schema) {
    SparkModelFileSchema cp = (SparkModelFileSchema) schema;
    SchemaUtils.copy(cp, this);
  }

  @Override
  public Schema getFileSchema() {
    return this.getSchema();
  }

  @Override
  public void setValue(int field, Object value) {
    this.put(field, value);
  }

  @Override
  public Object getValue(String fieldName) {
    return this.get(fieldName);
  }

  @Override
  public void accept(VfsVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public void setValue(String fieldName, Object value) {
    this.put(fieldName, value);
  }

  @Override
  public String getPath() {
    return this.path;
  }

  @Override
  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @Override
  public AccessEntry[] getPrivileges() {
    return privileges;
  }

  @Override
  public void setPrivileges(AccessEntry[] privileges) {
    this.privileges = privileges;
  }

}