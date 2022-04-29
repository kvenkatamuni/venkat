package com.paanini.jiffy.vfs.files;

import com.option3.docube.schema.nodes.SecureVaultEntrySchema;
import com.option3.docube.schema.nodes.Type;
import com.option3.docube.schema.nodes.UserPreferenceSchema;
import com.paanini.jiffy.dto.AccessEntry;
import com.paanini.jiffy.utils.SchemaUtils;
import com.paanini.jiffy.vfs.api.BasicFileProps;
import com.paanini.jiffy.vfs.api.ExtraFileProps;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.api.VfsVisitor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

public class UserPreference extends UserPreferenceSchema implements
        BasicFileProps, Persistable {

  public UserPreference() {
    setType(Type.USERPREFERENCE);
  }

  public UserPreference(final SpecificRecordBase schema) {
    SecureVaultEntrySchema ds = (SecureVaultEntrySchema) schema;
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
}
