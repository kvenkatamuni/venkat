package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.FolderSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.ArrayList;
import java.util.List;

public class Folder extends FolderSchema implements ExtraFileProps, BasicFileProps, Persistable {
  List<Persistable> children = new ArrayList<>();
  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  private List<String> role;

  public Folder() {
    setType(Type.FOLDER);
  }

  public Folder(final SpecificRecordBase schema) {
    FolderSchema ds = (FolderSchema) schema;
    SchemaUtils.copy(ds, this);
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
    visitor.enterFolder(this);
    children.forEach(c -> c.accept(visitor));
    visitor.exitFolder(this);
  }

  public void setChildren(List<Persistable> children) {
    this.children = children;
  }

  public List<Persistable> getChildren() {
    return children;
  }

  public void addChild(Persistable child) {
    this.children.add(child);
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

  public List<String> getRole() {
    return role;
  }

  public void setRole(List<String> role) {
    this.role = role;
  }
}
