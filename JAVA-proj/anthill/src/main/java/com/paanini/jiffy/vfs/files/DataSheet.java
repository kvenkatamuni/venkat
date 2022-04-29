package com.paanini.jiffy.vfs.files;



import com.option3.docube.schema.datasheet.meta.DataSheetSchema;
import com.option3.docube.schema.nodes.DatasheetSchema;
import com.option3.docube.schema.nodes.Mode;
import com.option3.docube.schema.nodes.Status;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.*;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.*;

/**
 * Created by appmgr on 29/1/16.
 * presentation is stored like:
 *
 *                              ______________
 *                             | Datasheet | --> props
 *                              --------------
 *                                  ||
 *                              JCR_CONTENT (Folder)
 *                          /                          \
 *         data(folder) -->props                 parquet(Folder) --> props
 *               |  .. \                                  |
 *          file(NT_FILE)                               file(NT_FILE)
 *              |                                          |
 *      JCR_CONTENT --> data                            JCR_CONTENT --> data
 */
public class DataSheet extends DatasheetSchema implements ExtraFileProps, BasicFileProps,
        Persistable, Exportable, DataSheetProps {

  public static DataSheet NULL = new DataSheet();

  String content;
  private String path;
  private AccessEntry[] privileges;
  private String parentId;
  private DataSheetSchema currentSchema;
  private Optional<Long> currentVersionNumber;
  private Long nextVersionNumber;
  public DataSheet(final SpecificRecordBase schema) {
    DatasheetSchema ds = (DatasheetSchema) schema;
    SchemaUtils.copy(ds, this);
  }

  public DataSheet(){
    this(Type.DATASHEET);
  }

  public DataSheet(Type type){
    super();
    setType(type);
    setMode(Mode.REPLACE);
    setStatus(Status.UNPUBLISHED);
  }

  public Boolean isCRLFEnding(){
    return getCRLFEnding();
  }

  public DataSheetSchema getCurrentSchema() {
    return status.equals(Status.PUBLISHED) ? datasheetPublishedSchema : datasheetSourceSchema;
  }

  protected String checkNull(String value, String def){
    return value == null ? def : value;
  }

  public Optional<Long> getCurrentVersionNumber() {
    return Objects.isNull(versionNumber) ? Optional.empty() : Optional.of(versionNumber);
  }

  public Long getNextVersionNumber() {
    return Objects.isNull(versionNumber) ? 0 : versionNumber + 1;
  }

  public String getContent() {
    return content;
  }

  private DataSheet setContent(String content) {
    this.content = content;
    return this;
  }

  public void accept(VfsVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Schema getFileSchema() {
    return this.getSchema();
  }

  @Override
  public void setValue(int field$, Object value$) {
    this.put(field$, value$);
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

  @Override
  public Set<String> getDependencies() {
    return Collections.<String>emptySet();
  }

  @Override
  public Set<String> updateDependencies() {
    return Collections.<String>emptySet();
  }

}

