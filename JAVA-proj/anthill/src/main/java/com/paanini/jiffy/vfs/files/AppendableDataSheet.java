package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.datasheet.meta.DataSheetSchema;
import com.option3.docube.schema.nodes.AppendableDataSheetSchema;
import com.option3.docube.schema.nodes.Mode;
import com.option3.docube.schema.nodes.Status;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.*;

public class AppendableDataSheet extends AppendableDataSheetSchema implements
        ExtraFileProps, BasicFileProps, Persistable, Exportable, DataSheetProps {
  private String path;
  private AccessEntry[] privileges = new AccessEntry[0];
  private String parentId;
  private DataSheetSchema currentSchema;
  private Optional<Long> currentVersionNumber;
  private Long nextVersionNumber;
  public AppendableDataSheet() {
    super();
    setType(Type.SQL_APPENDABLE_DATASHEET);
    setMode(Mode.APPEND);
    setStatus(Status.UNPUBLISHED);
  }

  public AppendableDataSheet(final SpecificRecordBase schema){
    AppendableDataSheetSchema ads = (AppendableDataSheetSchema) schema;
    SchemaUtils.copy(ads, this);
    setType(Type.SQL_APPENDABLE_DATASHEET);
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
  public void accept(VfsVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Object getValue(String fieldName) {
    return this.get(fieldName);
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

  public void setNewHigherBound() {
    this.setHigherBound(new Date().getTime());
  }

  public Optional<Long> getCurrentVersionNumber() {
    return Objects.isNull(versionNumber) ? Optional.empty() : Optional.of(versionNumber);
  }

  public Long getNextVersionNumber() {
    return Objects.isNull(versionNumber) ? 0 : versionNumber + 1;
  }

  public DataSheetSchema getCurrentSchema() {
    return status.equals(Status.PUBLISHED) ? datasheetPublishedSchema : datasheetSourceSchema;
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
