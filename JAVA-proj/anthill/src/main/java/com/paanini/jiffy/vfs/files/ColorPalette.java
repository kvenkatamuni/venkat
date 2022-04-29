package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.ColorPaletteSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

public class ColorPalette extends ColorPaletteSchema implements ExtraFileProps, BasicFileProps, Persistable {
  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  public ColorPalette(){
    setType(Type.COLOR_PALETTE);
  }

  public ColorPalette(final SpecificRecordBase schema) {
    ColorPaletteSchema cp = (ColorPaletteSchema) schema;
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
  public void accept(VfsVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Object getValue(String fieldName) {
    return this.get(fieldName);
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
