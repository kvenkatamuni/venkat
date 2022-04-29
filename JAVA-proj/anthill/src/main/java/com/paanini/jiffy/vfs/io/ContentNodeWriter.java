package com.paanini.jiffy.vfs.io;

import com.option3.docube.schema.nodes.EncryprionAlgorithms;
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
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by
 */
public class ContentNodeWriter implements SchemaHandler {

  private static final Logger LOGGER =
          LoggerFactory.getLogger(ContentNodeWriter.class);

  private final Persistable file;
  private final Node node;
  private final CipherService cipherService;

  private final SchemaService schemaService;
  private  String externalErroMessage;
  private boolean isExternalCreationSuccess;

  // this is to keep track of the schema versions used by the content node
  private Map<String, String> scehemaVersions;

  public ContentNodeWriter(Persistable file, Node node, Services services) {
    this.file = file;
    this.node = node;
    this.schemaService = services.getSchemaService();
    this.cipherService = services.getCipherService();
    this.isExternalCreationSuccess = true;
  }

  @Override
  public void startRecord(RecordSchema recordSchema) {
    this.scehemaVersions = new HashMap<>();
  }

  @Override
  public boolean endRecord(RecordSchema recordSchema) {
    return isExternalCreationSuccess;
  }

  @Override
  public void processField(FieldSchema fieldSchema) {
    Object value = file.getValue(fieldSchema.getName());
    Node propertyNode = getPropertyNode(fieldSchema);
    writeProperty(fieldSchema, value, propertyNode);
  }

  public String getExternalErroMessage() {
    return externalErroMessage;
  }

  private void writeProperty(FieldSchema schema, Object value, Node node) {
    String propertyName = schema.getFieldDef().getName();
    try {
      if (isignorableItem(propertyName)){
        return;
      }
      // skip if the property is write protected
      if (isProtectedItem(node, propertyName))
        return;

      Object defaultVal = schema.getDefaultValue();
      //if value and default valu eare null, its a propertydeletion
      if (value == null && node.hasProperty(propertyName) &&
              (defaultVal == null ||
                      defaultVal == JsonProperties.NULL_VALUE)) {
        removeProperty(node, propertyName);
      } else if (value != null) {
        writeValue(node, value, schema);
      } else if (defaultVal != null &&
              defaultVal!= JsonProperties.NULL_VALUE) {
        Object concreateDefault = SpecificData.get()
                .getDefaultValue(schema.getOriginal());
        writeValue(node, concreateDefault, schema);
      }
      removeAlternateProp(node, schema);

    } catch (RepositoryException e) {
      LOGGER.error("Error writing " + propertyName + " with value " +
              value, e);
      throw new ProcessingException(e.getMessage());
    }
  }

  private void removeAlternateProp(Node node, FieldSchema schema) throws
          RepositoryException {
    Optional<FieldDef> optionalDef = schema.getAlternativeDef();
    if(optionalDef.isPresent()) {
      FieldDef fieldDef = optionalDef.get();
      String propertyName = fieldDef.getName();
      if(fieldDef.getPath().isEmpty()) {
        if(node.hasProperty(propertyName))
          node.getProperty(propertyName).remove();
      } else {
        final String nodeName = fieldDef.getPath().get(0);
        if(node.hasNode(nodeName))
          node.getNode(nodeName).remove();
      }
    }
  }

  private void removeProperty(Node node, String property)
          throws RepositoryException {
    // dont remove if the property is the schema version since this will
    // always be null in the incoming file.
    //@todo: handle meta properties(may not be passed from UI)
    if (!property.equals(FileProps.AVRO_PUBLISH_SCHEMA_VERSION)
            && !property.equals(FileProps.AVRO_SOURCE_SCHEMA_VERSION)
            && !property.equals(FileProps.JIFFY_TABLE_SCHEMA_VERSION)
            && !property.equals(FileProps.AVRO_DASHBOARD_SCHEMA_VERSION)
            && !property.equals(FileProps.NOTEBOOK_CONFIGURATION_VERSION)
            && !property.equals(FileProps.FOLDER_OPTIONS_VERSION)
            && !property.equals(FileProps.JIFFY_TABLE_FORMS_VERSION)
            && !property.equals(FileProps.JIFFY_TABLE_INDEXES_VERSION))
      node.getProperty(property).remove();
  }

  private Node getPropertyNode(FieldSchema fieldSchema) {
    try {
      return getTargetNode(node, fieldSchema);
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ProcessingException(e.getMessage(), e);
    }
  }

//    /**
//     * write all schema versions onto the node.
//     */
//    private void writeSchemaVersions() {
//        scehemaVersions.forEach((k, v) -> {
//            try {
//                node.setProperty(k, v);
//            } catch (RepositoryException e) {
//                LOGGER.error(e.getMessage(), e);
//                throw new ProcessingException(e.getMessage(), e);
//            }
//        });
//    }

  /**
   * writes the property to the node after typecasting to the required type
   * @param node
   * @param value
   * @param fieldSchema
   * @return
   * @throws RepositoryException
   */
  private Node writeValue(Node node, Object value, FieldSchema fieldSchema)
          throws RepositoryException {
    Schema.Type schemaType = fieldSchema.getType();
    String propName = fieldSchema.getFieldDef().getName();

    switch (schemaType) {
      case INT:
        node.setProperty(propName, (long) value);
        break;
      case NULL:
        // this is a delete property operation and has been handled
        // prior to this method call
        break;
      case ENUM:
        Enum valueEnum = (Enum) value;
        node.setProperty(propName, valueEnum.name());
        break;
      case LONG:
        node.setProperty(propName, (long) value);
        break;
      case FLOAT:
        node.setProperty(propName, (double) value);
        break;
      case UNION:
        setUnionProperty(node, value, fieldSchema);
        break;
      case DOUBLE:
        node.setProperty(propName, (double) value);
        break;
      case STRING:
        node.setProperty(propName, getStringValue(value,fieldSchema));
        break;
      case BOOLEAN:
        node.setProperty(propName, (boolean) value);
        break;
      case RECORD:
        setRecord(node, value, fieldSchema);
        break;
      case ARRAY:
        setArray(node, value, fieldSchema);
        break;
      case FIXED:
      case BYTES:
      case MAP:
        LOGGER.error("Unsupported data type in avro schema : " +
                schemaType);
        throw new ProcessingException("Unsupported data type in avro " +
                "" + "schema : " + schemaType);
    }
    return node;
  }

  /**
   * returns the string value after encryption if needed.
   * @param value
   * @param fieldSchema
   * @return
   * @throws RepositoryException
   */
  private String getStringValue(Object value, FieldSchema fieldSchema) throws RepositoryException {
    String content = (String) value;
    String type = "";
    EncryprionAlgorithms encAlgo = null;

    try {
      if (fieldSchema.isEncrypted()) {
        Value encryptionAlgo = node.hasProperty(Common.ENCRYPTION_ALGO)
                ? node.getProperty(Common.ENCRYPTION_ALGO).getValue()
                : null;
        encAlgo = Objects.nonNull(encryptionAlgo)
                ? EncryprionAlgorithms.valueOf(encryptionAlgo.getString())
                : EncryprionAlgorithms.RSA2048;
        return encrypt(node.getIdentifier(), content, VaultType.HASHICORP.name(),encAlgo);
      }

      if (fieldSchema.isEncryptedV2()) {
        String key = file.getValue(fieldSchema.getKeyProp()).toString();
        type = node.getProperty("vault").getValue().toString();
        Value encryptionAlgo = node.hasProperty(Common.ENCRYPTION_ALGO)
                ? node.getProperty(Common.ENCRYPTION_ALGO).getValue()
                : null;
        encAlgo = Objects.nonNull(encryptionAlgo)
                ? EncryprionAlgorithms.valueOf(encryptionAlgo.getString())
                : EncryprionAlgorithms.RSA2048;
        return encrypt(key, content, type,encAlgo);
      }
    } catch (VaultException e) {
      LOGGER.error("Error while creating vault entry {}", e.getMessage());
      /**
       * Cant delete node immediately, have to wait for all other properties to be iterated
       *
       */
      if(type.equals(VaultType.CYBERARK.name())){
        externalErroMessage = e.getMessage();
      }
      isExternalCreationSuccess = false;
    }
    return content;
  }

  /**
   * sets the union property based on the type of the value
   * @param node
   * @param value
   * @param schema
   * @throws RepositoryException
   */
  private void setUnionProperty(Node node, Object value, FieldSchema schema)
          throws RepositoryException {
    String propName = schema.getFieldDef().getName();
    if (value instanceof String) {
      node.setProperty(propName, (String) value);
    } else if (value instanceof Long) {
      node.setProperty(propName, (long) value);
    } else if (value instanceof Double) {
      node.setProperty(propName, (double) value);
    } else if (value instanceof Boolean) {
      node.setProperty(propName, (boolean) value);
    } else if (value instanceof SpecificRecordBase) {
      Optional<FieldSchema> recordSchema = schema.getUnionType().stream()
              .filter(s -> s.getType().equals(Schema.Type.RECORD))
              .findFirst();
      if(recordSchema.isPresent()) {
        setRecord(node, value, recordSchema.get());
      }
    } else if (value instanceof Enum) {
      Enum status = (Enum) value;
      node.setProperty(propName, status.name());
    } else if (value instanceof List) {
      Optional<FieldSchema> recordSchema = schema.getUnionType().stream()
              .filter(s -> s.getType().equals(Schema.Type.ARRAY))
              .findFirst();
      if(recordSchema.isPresent()) {
        setArray(node, value, recordSchema.get());
      }
    }
    else {
      throw new ProcessingException("Non mapped property " +
              propName + " value type :" + value.getClass());
    }
  }

  /**
   * writes a content onto the node
   * @param node
   * @param value
   * @param fieldSchema
   * @throws RepositoryException
   */
  private void setRecord(Node node, Object value, FieldSchema fieldSchema)
          throws RepositoryException {
    String propName = fieldSchema.getFieldDef().getName();
    String recordContent = getSerializedRecord(value, fieldSchema);
    node.setProperty(propName, recordContent);
    this.node.setProperty(fieldSchema.getVersionFieldDef().getName(),
            schemaService.getLatestClassVersion(fieldSchema.getInstanceName()));
//        setSchemaVersion(fieldSchema);
  }

  /**
   * writes an array onto the node
   * @param node
   * @param value
   * @param fieldSchema
   * @throws RepositoryException
   */
  private void setArray(Node node, Object value, FieldSchema fieldSchema) throws RepositoryException {
    String propName = fieldSchema.getFieldDef().getName();
    FieldSchema arrayElementSchema = fieldSchema.getArrayType();
    switch (arrayElementSchema.getType()){
      case STRING:
        node.setProperty(propName, ((List<String>) value).toArray(new
                String[0]));
        break;
      case RECORD:
        List<Object> values = (List<Object>) value;
        final String[] serialzedSchemas = values.stream()
                .map(v -> getSerializedRecord(v, arrayElementSchema))
                .collect(Collectors.toList())
                .toArray(new String[0]);
        node.setProperty(propName, serialzedSchemas);
        this.node.setProperty(fieldSchema.getVersionFieldDef().getName(),
                schemaService.getLatestClassVersion(arrayElementSchema.getInstanceName()));
        break;
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
   * returns the node after traversing through the path if needed.
   *
   * @param node
   * @param fieldSchema
   * @return
   * @throws RepositoryException
   */
  private Node getTargetNode(Node node, FieldSchema fieldSchema)
          throws RepositoryException {
    List<String> paths = fieldSchema.getFieldDef().getPath();
    switch (paths.size()) {
      case 0:
        return node;
      case 1:
        return getNode(node, paths.get(0), NodeType.NT_RESOURCE);
      case 2:
        return getNode(
                getNode(node, paths.get(0), NodeType.NT_FILE),
                paths.get(1), NodeType.NT_RESOURCE);
      default:
        return traverseFolder(node, paths);
    }
  }

  /**
   * traverses to the path of the node if it contains folders within
   * @param childNode
   * @param paths
   * @return
   * @throws RepositoryException
   */
  private Node traverseFolder(Node childNode, List<String> paths)
          throws RepositoryException {
    List<String> subFolders = paths.subList(0, paths.size() - 2);
    for (String folder : subFolders) {
      childNode = getNode(childNode, folder, NodeType.NT_FOLDER);
    }
    childNode = getNode(
            getNode(childNode, paths.get(paths.size() - 2), NodeType.NT_FILE),
            paths.get(paths.size() - 1),
            NodeType.NT_RESOURCE);
    return childNode;
  }

  /**
   * this returns a new node if it is not present (for create operation) and
   * returns the existing node if present (which indicates its an update
   * operation
   *
   * @param node
   * @param name
   * @param type
   * @return
   * @throws RepositoryException
   */
  private Node getNode(Node node, String name, String type)
          throws RepositoryException {
    if(node.hasNode(name)) {
      return node.getNode(name);
    } else {
      return node.addNode(name, type);
    }
  }

  /** This method will ignore those properties that are getting set to
   * previous version**/
// TODO: 09/07/21 Check the other version properties which may cause similar issues and add them in a list
  private boolean isignorableItem(String propName) throws
          RepositoryException {
    return  propName.equals(FileProps.AVRO_DASHBOARD_SCHEMA_VERSION) ||
            propName.equals(FileProps.JIFFY_TABLE_SCHEMA_VERSION) ||
            propName.equals(FileProps.JIFFY_TABLE_INDEXES_VERSION) ||
            propName.equals(FileProps.JIFFY_TABLE_FORMS_VERSION);
  }



  /**
   * checks if the given property is a write protected item.
   * @param node
   * @param propName
   * @return
   * @throws RepositoryException
   */
  private boolean isProtectedItem(Node node, String propName) throws
          RepositoryException {
    return node.hasProperty(propName) &&
            node.getProperty(propName).getDefinition().isProtected();
  }

  /**
   * gets serialized values for the content node
   * @param value
   * @param fSchema
   * @return
   */
  private String getSerializedRecord(Object value, FieldSchema fSchema) {
    try {
      if(fSchema.isSerealizable()) {
        return schemaService.serialize(fSchema.getInstanceName(),
                (SpecificRecordBase) value);
      } else {
        throw new ProcessingException("No serializer found for " +
                "current record " + fSchema.getInstanceName());
      }
            /*switch (fSchema.getTypeClassName()) {
                case "Dashboard":
                    return schemaService.serialize((Dashboard) value);
                case "DataSheetSchema":
                    return schemaService.serializeDatasheetSchema(
                            (DataSheetSchema) value);
                case "DataSheetRestriction2":
                    return schemaService.serializeUserRestriction2(
                            (DataSheetRestriction2) value);
                case "DataSchema":
                    return schemaService.serializeJiffyTableSchema(value);
                default:
                    LOGGER.error("Invalid record type encountered for : " +
                            fSchema.getTypeClassName());
                    return null;
            }*/
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
      throw new ProcessingException("Failed while serialization : " +
              fSchema.getInstanceName());
    }
  }

  String encrypt(String key ,String content,String type,EncryprionAlgorithms encAlgo) throws VaultException {
    String result = StringUtils.EMPTY;
    if (VaultType.HASHICORP.name().equals(type)) {
      HashiCorp vault = (HashiCorp) get(new HashiCorpVaultFactory());
      vault.setURL(cipherService.getVaultUrl());
      vault.setRootToken(cipherService.getVaultToken());
      HashiCorpInput input = new HashiCorpInput();
      input.setKey(key);
      input.setValue(content);
      input.setEncryprionAlgorithm(encAlgo);
      result = vault.insert(input);
    } else if (VaultType.CYBERARK.name().equals(type)) {
            /*try {
                CyberArk cyberArk = (CyberArk) get(new CyberArkVaultFactory());
                CyberArcInput input = new CyberArcInput();
                input.setServices(services);
                input.setAppId(node.getProperty("appId").getString());
                input.setSafe(node.getProperty("safe").getString());
                input.setFolder(node.getProperty("folder").getString());
                input.setCyberArkObject(node.getProperty("cyberArkObject").getString());
                input.setKey(node.getProperty("jcr:title").getString());
                result = cyberArk.insert(input);//No insert support
            } catch (RepositoryException e) {
                LOGGER.error("Error while building the cyberArk object.", e);
            }*/
      return "";
    } else {
      throw new ProcessingException("Vault of Type " + type + " not found");
    }
    return result;
  }

  Vault get(HashiCorpVaultFactory factory){
    return factory.create();
  }

  /**
   * sets the schema versions to be written to the file. this is used for
   * deserialization.
   * @param fSchema
   */
   /* private void setSchemaVersion(FieldSchema fSchema) {
        if (fSchema.getName().equals(DocubeFileProps.PUBLISH_SCHEMA))
            scehemaVersions.put(DocubeFileProps.AVRO_PUBLISH_SCHEMA_VERSION,
                    schemaService.getLatestVersion("datasheet"));
        else if (fSchema.getName().equals(DocubeFileProps.SOURCE_SCHEMA))
            scehemaVersions.put(DocubeFileProps.AVRO_SOURCE_SCHEMA_VERSION,
                    schemaService.getLatestVersion("datasheet"));
        else if (fSchema.getTypeClassName().equals("Dashboard")) {
            scehemaVersions.put(DocubeFileProps
                    .AVRO_DASHBOARD_SCHEMA_VERSION, schemaService
                    .getLatestVersion("dashboard"));
        } else if (fSchema.getTypeClassName().equals("DataSheetRestriction2")) {
            scehemaVersions.put(DocubeFileProps.DataSheetUserRestriction_SCHEMA_VERSION,
                    schemaService.getLatestVersion("datasheetuserrestriction"));
        } else if (fSchema.getTypeClassName().equals("NotebookConfig")) {
            scehemaVersions.put(DocubeFileProps.Notebook_CONFIGURATION_VERSION,
                    schemaService.getLatestVersion("notebookconfig"));
        } else {
            LOGGER.error("Invalid record type encountered for : " + fSchema
                    .getTypeClassName());
            throw new ProcessingException("Invalid record type encountered " +
                    "for : " + fSchema.getTypeClassName());
        }
    }*/
}
