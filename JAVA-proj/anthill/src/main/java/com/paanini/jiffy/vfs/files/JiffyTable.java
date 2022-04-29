package com.paanini.jiffy.vfs.files;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.option3.docube.schema.jiffytable.TableType;
import com.option3.docube.schema.nodes.JiffyTableSchema;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.models.ApiDump;
import com.paanini.jiffy.models.ImpexContent;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.DataExportable;
import com.paanini.jiffy.vfs.api.Exportable;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

/**
 * Holds jiffy table schema
 * Created by Adarsh V
 */
public class JiffyTable extends JiffyTableSchema implements ExtraFileProps,
        BasicFileProps, Persistable, Exportable, DataExportable {

  public static final String CATEGORY_SUFFIX = "_category";
  public static final String PSEUDONYMS_SUFFIX = "_pseudonyms";
  public static final String ACCURACY_SUFFIX = "_accuracy";
  private String path;
  private AccessEntry[] privileges;
  private String parentId;

  @Override
  public List<ImpexContent> retrieveExportables() {
    if(this.getTableType().equals(TableType.DOC_JDI)){
      StringBuilder sb  = new StringBuilder();
      sb.append(this.getPath()).append("/").append("data");
      ApiDump apiDump = new ApiDump(sb.toString());
      List<ImpexContent> exportableContents = new ArrayList<>();
      exportableContents.add(apiDump);
      return exportableContents;
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<ImpexContent> retrieveImportables(String path, String jfsFolderId) {
    if(this.getTableType().equals(TableType.DOC_JDI)){
      StringBuilder sb  = new StringBuilder();
      sb.append(path).append(this.getName()).append("/").append("importData")
              .append("/").append(jfsFolderId);
      ApiDump apiDump = new ApiDump(sb.toString());
      List<ImpexContent> exportableContents = new ArrayList<>();
      exportableContents.add(apiDump);
      return exportableContents;
    } else {
      return Collections.emptyList();
    }
  }

  public enum SelectType {
    ID, PATH
  }

  public JiffyTable(){
    setType(Type.JIFFY_TABLE);
  }

  public JiffyTable(final SpecificRecordBase schema) {
    JiffyTableSchema jts = (JiffyTableSchema) schema;
    SchemaUtils.copy(jts, this);
  }

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

  @Override
  @JsonIgnore
  public Set<String> getDependencies() {
    Set<String> dependencies = this.getForms()
            .stream()
            .flatMap(form -> form.getButtonSettings().stream()
                    .flatMap(btn -> btn.getButtonDetails().stream()
                            .filter(buttonDetails -> Objects.nonNull(buttonDetails.getTask()))
                            .map(buttonDetails -> "@external_id:" + buttonDetails.getTask())
                    ))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    if(this.getTableType().equals(TableType.DOC_JDI)) {
      String[] path = this.getPath().split("/");
      if(path.length > 1) {
        String app = path[path.length - 2];
        dependencies.add(app + PSEUDONYMS_SUFFIX);
        dependencies.add(app + ACCURACY_SUFFIX);
      }
      dependencies.add(this.getName().concat(CATEGORY_SUFFIX));
      dependencies.add(FileSet.APP_STORAGE);
    }
    return dependencies;
  }

  @Override
  public Set<String> updateDependencies() {
    return Collections.<String>emptySet();
  }
}