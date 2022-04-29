package com.paanini.jiffy.vfs.schema;

import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.exception.ProcessingException;
import org.apache.avro.Schema;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A class represents Schema of the field in avro generated class,
 * Definition includes,
 *      1) Field definition {@code: FieldDef}
 *      2) Alternation definition, which is used if original not found,
 *          in order to support backward compatibility
 *      3) Details related to type of field to help reading it from JackRabiit
 *<p>
 *     FieldDef are meta class, information from this class is used for reading
 *     or writing fields from java classes to Jackrabbit stores
 *</p>
 *
 *  @author  Priyanka Bhoir
 *  @date 29/7/19
 */
public class FieldSchema {
  final String name;
  final FieldDef fieldDef;
  final Optional<FieldDef> alternativeDef;
  final Schema.Field originalField;
  private final Schema schema;

  public FieldSchema(String name, FieldDef fieldDef,
                     Optional<FieldDef> alternativeDef,
                     Schema.Field originalField,
                     Schema schema) {
    this.name = name;
    this.fieldDef = fieldDef;
    this.alternativeDef = alternativeDef;
    this.schema = schema;
    this.originalField = originalField;
  }

  public FieldSchema(String name, FieldDef fieldDef,
                     Optional<FieldDef> alternativeDef,
                     Schema.Field originalField) {
    this.name = name;
    this.fieldDef = fieldDef;
    this.alternativeDef = alternativeDef;
    this.originalField = originalField;
    this.schema = originalField.schema();
  }


  public String getName() {
    return name;
  }

  /**
   * Returns {@code: FieldDef} which was build using property name
   * @return {@code: FieldDef}
   */
  public FieldDef getFieldDef() {
    return fieldDef;
  }

  /**
   * Returns Alternative {@code: FieldDef} which was build using property
   * name. Alternative is used when original property is not found on Node.
   * This helps backward compatibility
   * @return {@code: FieldDef}
   */
  public Optional<FieldDef> getAlternativeDef() {
    return alternativeDef;
  }

  /**
   * Returns Datatype for the Field
   * @return
   */
  public Schema.Type getType() {
    return schema.getType();
  }

  /**
   * Position is used to set values in avro classes
   * @return index of field in schema list.
   */
  public int getPosition() {
    return  originalField.pos();
  }

  public Object getDefaultValue() {
    return originalField.defaultVal();
  }

  /**
   * @return Data type class of the record. Eg Enum, Long, DatasheetSchema
   */
  public String getInstanceName() {
    return schema.getFullName();
  }

  /**
   * @return Data type Class name of record
   */
  public String getTypeClassName() {
    return schema.getName();
  }

  /**
   * Returns list of schema class with different Data type from union.
   * Union defines many data types can be applicable to the schema.
   * @return list of {@code: FieldSchema}
   */
  public List<FieldSchema> getUnionType(){
    return schema.getTypes()
            .stream()
            .map(s-> new FieldSchema(name, fieldDef, alternativeDef,
                    originalField, s))
            .collect(Collectors.toList());
  }

  /**
   * Returns the element type of an array.
   * @return list of {@code: FieldSchema}
   */
  public FieldSchema getArrayType() {
    return new FieldSchema(name, fieldDef, alternativeDef, originalField,
            schema.getElementType());
  }

  /**
   * Returns if original definition or alt definition has encryption applied
   * @return      the {@code: boolean}  if original definition or alt definition
   *              has encryption applied
   */
  public boolean isEncrypted() {
    return this.fieldDef.isEncrypted() ||
            (this.alternativeDef.isPresent() && this.alternativeDef.get()
                    .isEncrypted());
  }

  /**
   * Returns if original definition  has encryption applied
   * @return      the {@code: boolean}  if original definition
   *              has encryption applied
   */
  public boolean isEncryptedV2() {
    return this.fieldDef.isEncryptedV2();
  }



  /**
   * Returns if alt definition needs migration
   * @return      the {@code: boolean} alt definition needs migration
   */
  public boolean isMigrationRequired() {
    return (this.alternativeDef.isPresent() && this.alternativeDef.get()
            .isMigrationRequired());
  }

  /**
   * @return A class which is going to migrate the data
   */
  public String getMigrationClass() {
    if(isMigrationRequired()) {
      return this.alternativeDef.get().getMigrationClass();
    }

    throw new ProcessingException("Migration not applicable for "
            + schema.getName());
  }

  /**
   * @return property name whoose value will be acting as key prop
   */
  public String getEncryptionKeyProp() {
    if(isEncryptedV2()) {
      return this.alternativeDef.get().getEncryptionKeyProp().get();
    }

    throw new ProcessingException(Common.ENCRYPTION_NOT_APPLICABLE_FOR
            + schema.getName());
  }


  /**
   * @return A class which is going to migrate the data
   */
  public String getEncryptionClass() {
    if(isEncryptedV2()) {
      return this.fieldDef.getEncryptionClass().get();
    }

    throw new ProcessingException(Common.ENCRYPTION_NOT_APPLICABLE_FOR
            + schema.getName());
  }

  /**
   * @return property name where key is saved
   */
  public String getKeyProp() {
    if(isEncryptedV2()) {
      return this.fieldDef.getEncryptionKeyProp().get();
    }

    throw new ProcessingException(Common.ENCRYPTION_NOT_APPLICABLE_FOR
            + schema.getName());
  }
  /**
   * Returns if defination is serealizable
   * @return      the {@code: boolean} alt definition needs migration
   */
  public boolean isSerealizable() {
    return this.fieldDef.isSerealizable();
  }

  /**
   * Returns the version property for serialization
   * @return
   */
  public FieldDef getVersionFieldDef() {
    return FieldDef.FieldDefBuilder.from(this.fieldDef.getSerializationVersion());
  }

  public Schema.Field getOriginal(){
    return this.originalField;
  }

  public static class FieldSchemaBuilder {

    /**
     * Builds the Field schema from avro schema.
     * Builder maps :
     *     1) name to name of schema
     *     2)first element in alias to original definition
     *     3)second element in alias to alternative definition
     *     4) Type to type of schema
     * @param schema
     * @return      {@code FieldSchema} value build from avro schema
     */
    public static FieldSchema from(Schema.Field schema) {
      String[] aliases = schema.aliases()
              .toArray(new String[0]);
      return new FieldSchema(
              schema.name(),
              getDef(aliases, schema.name()),
              getAlternativeDef(aliases),
              schema);
    }

    private static FieldDef getDef(String[] aliases, String name){
      String propName = aliases.length > 0 ? aliases[0] : name;
      return FieldDef.FieldDefBuilder.from(propName);
    }

    private static Optional<FieldDef> getAlternativeDef(String[] aliases){
      if(aliases.length > 1){
        return Optional.of(FieldDef.FieldDefBuilder.from(aliases[1]));
      }

      return Optional.empty();
    }
  }
}

