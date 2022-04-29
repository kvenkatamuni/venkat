package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.FileSetSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.Exportable;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.ArrayList;
import java.util.List;

public class FileSet extends FileSetSchema implements ExtraFileProps, BasicFileProps, Persistable,
    Exportable {

  public static String APP_STORAGE = "app_storage";
  public static final String FILE_SET_MEMBER = "filesetMember";
  List<Persistable> children = new ArrayList<>();
  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  public FileSet(){
    setType(Type.FILESET);
  }

  public FileSet(final SpecificRecordBase schema) {
    FileSetSchema cp = (FileSetSchema) schema;
    SchemaUtils.copy(cp, this);
  }


  public List<Persistable> getChildren() {
    return children;
  }

  public void setChildren(List<Persistable> children) {
    this.children = children;
  }

  @Override
  public Schema getFileSchema() {
    return this.getSchema();
  }

  @Override
  public void setValue(int field, Object value) {
    this.put(field, value);
  }


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
