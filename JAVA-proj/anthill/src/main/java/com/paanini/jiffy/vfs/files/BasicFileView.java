package com.paanini.jiffy.vfs.files;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.option3.docube.schema.nodes.SimpleFileSchema;
import com.option3.docube.schema.nodes.Status;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import javax.jcr.Node;
import java.util.List;
import java.util.Optional;

/**
 * This class is used exclusively for limited read operations like listing.
 */

public class BasicFileView extends SimpleFileSchema implements ExtraFileProps, BasicFileProps, Persistable {

  protected Node node;

  String path;
  AccessEntry[] privileges;
  String parentId;
  public BasicFileView(final SpecificRecordBase schema) {
    SimpleFileSchema cp = (SimpleFileSchema) schema;
    SchemaUtils.copy(cp, this);
    setStatus(Status.UNKNOWN);
  }

  public BasicFileView() {
    super();
    setStatus(Status.UNKNOWN);
  }

  //Field to hold the actual file on disk if any.
  @JsonIgnore
  private transient Optional<Long> sizeOnDisk;

  @JsonIgnore
  public Optional<Long> getSizeOnDisk() {
    return (sizeOnDisk == null) ? Optional.empty() : sizeOnDisk;
  }

  public void setSizeOnDisk(long sizeOnDisk) {
    this.sizeOnDisk = Optional.of(sizeOnDisk);
  }

  @Override
  public String getParentId() {
    return parentId;
  }

  @Override
  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  @JsonIgnore
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
  public AccessEntry[] getPrivileges() {
    return privileges;
  }

  @Override
  public void setPrivileges(AccessEntry[] privileges) {
    this.privileges = privileges;
  }

}