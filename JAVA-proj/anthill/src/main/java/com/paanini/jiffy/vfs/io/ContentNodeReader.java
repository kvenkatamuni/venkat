package com.paanini.jiffy.vfs.io;


import com.option3.docube.schema.nodes.VaultType;
import com.option3.docube.service.SchemaService;
import com.paanini.jiffy.constants.Common;
import com.paanini.jiffy.constants.FileProps;
import com.paanini.jiffy.encryption.api.*;
import com.paanini.jiffy.exception.ProcessingException;
import com.paanini.jiffy.exception.VaultException;
import com.paanini.jiffy.vfs.api.Persistable;
import com.paanini.jiffy.vfs.schema.FieldDef;
import com.paanini.jiffy.vfs.schema.FieldSchema;
import com.paanini.jiffy.vfs.schema.RecordSchema;
import org.apache.avro.JsonProperties;
import org.apache.avro.specific.SpecificData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *  A visitor class, visits Schema fields in order to read input node into
 *  java class.
 *
 *  @author  Priyanka Bhoir
 *  @date 1/8/19
 */
public class ContentNodeReader implements SchemaHandler {
  private final Persistable file;
  private final Node node;
  private final Services services;
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentNodeReader.class);
  private boolean softRead;

  public ContentNodeReader(Persistable file, Node node, Services services,boolean softRead) {
    this.file = file;
    this.node = node;
    this.services = services;
    this.softRead = softRead;
  }

  @Override
  public void startRecord(RecordSchema schema) {
    // Do nothing
  }

  @Override
  public boolean endRecord(RecordSchema schema) {
    return true;
  }

  /**
   * Method reads the node using {@code FieldSchema}
   * in {@code Persistable} file
   * @param schema
   */
  @Override
  public void processField(FieldSchema schema) {
    try {
      this.file.setValue(
              schema.getPosition(),
              readField(schema, node));
    } catch (RepositoryException e) {

      throw new ProcessingException(e.getMessage(), e);
    }
  }

  /**
   * Methods does :
   *      1) Find a matching Jackrabbit property for schema definition
   *      2) If property is found, read the value
   *      3) If schema has default have return default value
   *      4) if none of above condition matches throw Exception
   *
   * @param schema reference schema for
   * @param node node from which values to be read
   * @return a Object value read from jackrabbit node
   * @throws RepositoryException
   */
  private Object readField(FieldSchema schema, Node node)
          throws RepositoryException {
    Optional<PropertyWrapper> property = getProperty(node, schema);

    if(property.isPresent()) {
      return readValue(property.get(), schema, node);
    }

    if(schema.getDefaultValue() != null) {
      return readDefaultValue(schema);
    }
    //@todo: check if default value has to be returned in case of read error
    throw new ProcessingException("Invalid property for " +
            schema.getName() + " : " +
            schema.getFieldDef().getName());

  }

  /**
   * Method searches a property on Jackrabbit node
   * Steps :
   *        1) Return if main def exists on Jackrabbit node
   *        2) Return if alternative property exists on node
   *        3) Return empty if none of conditions are matching
   * @param node
   * @param schema
   * @return
   */
  private Optional<PropertyWrapper> getProperty(Node node, FieldSchema schema) {
    //Return the property if present.
    Optional<Property> property = readProperty(node, schema.getFieldDef());
    if(property.isPresent()){
      return Optional.of(new PropertyWrapper(property.get()));
    }

    //Return alternative property if main property is not present
    if(schema.getAlternativeDef().isPresent()) {
      Optional<Property> altProperty =
              readProperty(node, schema.getAlternativeDef().get());
      if(altProperty.isPresent()){
        return Optional.of(new PropertyWrapper(altProperty.get(), true));
      }
    }

    return Optional.empty();
  }

  private Optional<Property> readProperty(Node docNode, FieldDef fieldDef) {
    Node node = docNode;
    try {
      if (fieldDef.getPath().size() > 0) {
        for (String path : fieldDef.getPath()) {
          node = node.getNode(path);
        }
      }
      return Optional.of(node.getProperty(fieldDef.getName()));
    } catch(RepositoryException e){
      return Optional.empty();
    }
  }


  private Object readValue(PropertyWrapper wrapper, FieldSchema schema, Node node)
          throws RepositoryException {
    Property property = wrapper.getProperty();
    switch (schema.getType()) {
      case INT:
        return property.getLong();
      case NULL:
        return null;
      case ENUM:
        return readEnum(schema, property.getString());
      case LONG:
        return property.getLong();
      case FLOAT:
        return property.getDouble();
      case UNION:
        return readUnion(wrapper, schema, node);
      case DOUBLE:
        return  property.getDouble();
      case STRING:
        return readString(property, schema, node);
      case BOOLEAN:
        return property.getBoolean();
      case RECORD:
        /**
         * migration can only be applied if old property is being read.
         * Scenario:
         *  - old schema has property in structure 1.
         *  - new schema updates the structure to structure2
         *  - when old files are read, reader should read old property,
         *      convert it into new structure
         * Solution : old props are always alternative props to
         *      support backward compatibility, so
         *      1) verify property being read is alternative one and
         *      it needs migration, then call {@code: migrate} function
         */
        if(wrapper.isAlternate() && schema.isMigrationRequired()) {
          return migrate(schema, property.getString());
        }
        return readRecordProperty(property.getString(), schema, node);
      case ARRAY:
        return readArray(property.getValues(), schema, node);
      case FIXED:
      case BYTES:
      case MAP:
        throw new ProcessingException(
                "Unsupported data type in docube schema :" + schema.getName());
    }
    throw new ProcessingException("Unsupported data type in docube schema");
  }

  /**
   * Reads a propety as a string
   * @param property
   * @param schema
   * @param node
   * @return string value for property
   * @throws RepositoryException
   */
  private Object readString(Property property, FieldSchema schema, Node node)
          throws RepositoryException {
    try {
      /**
       * if schema has marked a fields as encrypted,
       *  and encrypted property of node is set to boolean
       */

      if (schema.isEncrypted() && node.hasProperty(Common.ENCRYPTED) && node.getProperty(Common.ENCRYPTED).getBoolean()) {
        return decrypt(node.getIdentifier(), property.getString(), VaultType.HASHICORP.name());
      } else if (schema.isEncryptedV2() && node.hasProperty(Common.ENCRYPTED) && node.getProperty(Common.ENCRYPTED).getBoolean()) {
        String key = file.getValue(schema.getKeyProp()).toString();
        String type = node.getProperty("vault").getValue().toString();
        return softRead ? "" : decrypt(key, property.getString(), type);
      }
    } catch (VaultException e) {
      LOGGER.error("Error while reading vault entry {}", e.getMessage());
      throw new ProcessingException("Vault could not read value for file : "
              + node.getIdentifier());
    }

    Value value = property.getValue();
    if (Objects.isNull(value)) {
      return null;
    }
    return property.getValue().toString();
  }

  String decrypt(String key ,String content,String type) throws VaultException {
    Vault vault;
    String plaintext = StringUtils.EMPTY;
    if(type.equals(VaultType.HASHICORP.name())){
      vault = get(new HashiCorpVaultFactory());
      ((HashiCorp)vault).setURL(services.getCipherService().getVaultUrl());
      ((HashiCorp)vault).setRootToken(services.getCipherService().getVaultToken());
      HashiCorpInput input = new HashiCorpInput();
      input.setKey(key);
      input.setValue(content);
      plaintext = vault.get(input);
    }
    else if(type.equals(VaultType.CYBERARK.name()))
    {

      vault = get(new CyberArkVaultFactory());
      try {
        CyberArcInput input = new CyberArcInput();
        input.setServices(services);
        input.setCliPath(services.getCipherService().getCliPath());
        input.setAppId(node.getProperty("appId").getString());
        input.setSafe(node.getProperty("safe").getString());
        input.setFolder(node.getProperty("folder").getString());
        input.setCyberArkObject(node.getProperty("cyberArkObject").getString());
        input.setKey(node.getProperty("jcr:title").getString());
        plaintext = vault.get(input);
      } catch (RepositoryException e) {
        throw new ProcessingException("Error while parsing the cyberArc Properties");
      } catch (VaultException ve) {
        throw new ProcessingException(ve.getMessage());
      }
    }
    else{
      throw new ProcessingException("Vault of Type "+ type +" not found");
    }
    return plaintext;
  }

  Vault get(AbstractVaultFactory factory) {
    return factory.create();
  }

  /**
   * Reads a property of type Union.
   *
   * Avro type Union suggests, Data can be any one of the type from union,
   * so method iterates on list of union and best fit read value is return
   * @param wrapper
   * @param schema
   * @param node
   * @return
   */
  private Object readUnion(PropertyWrapper wrapper, FieldSchema schema, Node node) {
    Object o = null;
    for (FieldSchema fs : schema.getUnionType()) {
      try{
        o = readValue(wrapper, fs, node);
      } catch (RepositoryException r) {
        //do Nothing
      }
    }
    return o;
  }

  private Object readEnum(FieldSchema schema, String value) {
    try {
      Class<?> enumCls = Class.forName(schema.getInstanceName());
      return value.isEmpty()
              ? null
              : Enum.valueOf((Class<Enum>) enumCls, value);
    } catch (ClassNotFoundException e) {
      throw new ProcessingException("Enum class not found : "
              + schema.getTypeClassName());
    }
  }

  private Object readDefaultValue(FieldSchema schema) {
    switch (schema.getType()) {
      case INT:
      case LONG:
      case FLOAT:
      case UNION:
      case DOUBLE:
      case STRING:
      case BOOLEAN:
        return getDefaultPrimitive(schema);
      case NULL:
        return null;
      case ENUM:
        return readEnum(schema, (String) schema.getDefaultValue());
      case RECORD:
        return readDefaultRecord(schema);
      case FIXED:
      case BYTES:
      case ARRAY:
        return getDefaultPrimitive(schema);
      case MAP:
        throw new UnsupportedOperationException("Unsupported " +
                "default value " + schema.getType());
    }
    throw new ProcessingException("Unsupported data type in avro docube schema");
  }

  private Object readDefaultRecord(FieldSchema schema) {
    /**only published schema can have default value.*/
    if(schema.getTypeClassName().equals("DataSheetSchema")
            && schema.getName().equals(FileProps.PUBLISH_SCHEMA)) {
      return SpecificData.get().getDefaultValue(schema.getOriginal());
    }
    return null;
  }

  private Object getDefaultPrimitive(FieldSchema schema) {
    return schema.getDefaultValue().equals(JsonProperties.NULL_VALUE) ?
            null :
            schema.getDefaultValue();
  }
  /**
   * Reads Record type Field. Avro Record's Java equivalent is a Java class
   *
   * <p>
   *     Jackrabbit does not support property to be a object, thats why
   *     Objects are serialized and store. So While reading a string value
   *     has be to deserialize,
   *
   *     to support backward and foreword compatibility,
   *     The Record schemas are versioned. thse version are used to read
   *     the records
   * </p>
   * @param recordStr
   * @param schema
   * @param node
   * @return Object of class which is mentioned as a type of field
   * @throws RepositoryException
   */
  private Object readRecordProperty(String recordStr, FieldSchema schema,
                                    Node node) throws RepositoryException {
    try {
      if(schema.isSerealizable()) {
        SchemaService schemaService = services.getSchemaService();
        FieldDef versionDef = schema.getVersionFieldDef();
        Optional<Property> property = readProperty(node, versionDef);
        if(property.isPresent()) {
          String version = property.get().getString();

          return schemaService.deSerialize(
                  schema.getInstanceName(), recordStr, version);
        }

      }
    } catch (IOException e) {
      LOGGER.error("error during deserialization : ",e);
      throw new ProcessingException("Failed while deserialization: " +
              schema.getInstanceName());
    }
        /*try {
            SchemaService schemaService = services.getSchemaService();
            switch (schema.getTypeClassName()) {
                case "Dashboard":
                    String version = getSchemaVersion(node,
                            DocubeFileProps.AVRO_DASHBOARD_SCHEMA_VERSION);
                    return  schemaService.deSerialize(recordStr, version);
                case "DataSheetSchema":
                    return readDatasheetSchema(recordStr, schema.getName(), node);
                case "DataSheetRestriction2":
                    String v = getSchemaVersion(node,
                            DocubeFileProps.DataSheetUserRestriction_SCHEMA_VERSION);
                    return v == null ? migrate(schema, recordStr) :
                            schemaService.deSerializeUserRestriction2(recordStr, v);
                case "DataSchema":
                    return schemaService.deSerializeJiffyTableSchema(recordStr);

            } */

    return null;
  }

  /**
   * Reads Array type Field. Returns a list as the equivalent in the
   * schema generated java classes for an array is a list
   * @param values
   * @param schema
   * @param node
   * @return
   * @throws RepositoryException
   */
  private List<? extends Object> readArray(Value[] values, FieldSchema schema,
                                           Node node) throws RepositoryException {
    FieldSchema arrayElementSchema = schema.getArrayType();
    switch (arrayElementSchema.getType()){
      case STRING:
        List<String> valueList = new ArrayList<>();
        for (Value value : values) {
          valueList.add(value.getString());
        }
        return valueList;
      case RECORD:
        List<Object> deserializerData = new ArrayList<>();
        for (Value value : values) {
          Object deserializeData = readRecordProperty(value.getString(),
                  arrayElementSchema,
                  node);
          deserializerData.add(deserializeData);
        }
        return deserializerData;
      case UNION:
        throw new ProcessingException("Only homogeneous arrays are " +
                "supported");
      default:
        throw new ProcessingException("Array of type " +
                arrayElementSchema.getType().getName() + " is not " +
                "supported in current version");
    }
  }


  /**
   * Deserializes the Record of type DatasheetSchema.
   *
   * @param strSchema
   * @param name
   * @param node
   * @return Obejct of type DatahseetSchema
   * @throws IOException
   * @throws RepositoryException
   */
  private Object readDatasheetSchema(String strSchema, String name, Node node)
          throws IOException, RepositoryException {
    SchemaService schemaService = services.getSchemaService();
    if(name.equals(FileProps.SOURCE_SCHEMA)) {
      return schemaService.deSerializeDatasheetSchema(strSchema,
              getSchemaVersion(node, FileProps.AVRO_SOURCE_SCHEMA_VERSION));

    } else if(name.equals(FileProps.PUBLISH_SCHEMA) &&
            node.hasProperty(FileProps.AVRO_PUBLISH_SCHEMA_VERSION)) {
      return schemaService.deSerializeDatasheetSchema(strSchema,
              getSchemaVersion(node, FileProps.AVRO_PUBLISH_SCHEMA_VERSION));

    }
    return null;
  }

  /**
   * returns the schema version property from the jackrabbit
   * @param node
   * @param propName
   * @return
   * @throws RepositoryException
   */
  private String getSchemaVersion(Node node, String propName)
          throws RepositoryException {
    return node.hasProperty(propName) ?
            node.getProperty(propName).getString(): null;

  }

  /**
   * Finds the migration class and run migrate method
   * @param schema
   * @param recordStr
   * @return
   */
  private Object migrate(FieldSchema schema, String recordStr) {
    try {
      Class migrationClass = Class.forName(schema.getMigrationClass());
      Method migrate =
              migrationClass.getMethod("migrate",
                      String.class,
                      String.class,
                      com.option3.docube.schema.nodes.Type.class,
                      SchemaService.class);
      return migrate.invoke(migrationClass.newInstance(), recordStr,
              schema.getName(),
              NodeUtils.getType(node),
              services.getSchemaService());
    } catch (ClassNotFoundException | NoSuchMethodException |
            IllegalAccessException  | InvocationTargetException |
            InstantiationException e) {
      throw new ProcessingException(e.getMessage(), e);
    }
  }
}
