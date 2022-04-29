package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.AppRolesSchema;
import com.option3.docube.schema.nodes.SubType;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

public class AppRoles extends AppRolesSchema implements ExtraFileProps, BasicFileProps, Persistable {

  public static final String APP_ROLE_PREFIX = "APP_ROLE_";
  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  public AppRoles(){
    setType(Type.APP_ROLES);
    setSubType(SubType.appRoles);
  }

  public AppRoles(final SpecificRecordBase schema) {
    AppRolesSchema jts = (AppRolesSchema) schema;
    SchemaUtils.copy(jts, this);
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

}
