package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.ConfigSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class Config extends ConfigSchema implements ExtraFileProps, BasicFileProps, Persistable,
        Exportable {

  static Logger logger = LoggerFactory.getLogger(Config.class);

  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  public Config() {
    setType(Type.CONFIGURATION);
  }

  public Config(final SpecificRecordBase schema) {
    ConfigSchema cp = (ConfigSchema) schema;
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
  public void setValue(String fieldName, Object value) {
    this.put(fieldName, value);
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

  @Override
  public Set<String> getDependencies() {
    return Collections.<String>emptySet();
  }

  @Override
  public Set<String> updateDependencies() {
    return Collections.<String>emptySet();
  }

}