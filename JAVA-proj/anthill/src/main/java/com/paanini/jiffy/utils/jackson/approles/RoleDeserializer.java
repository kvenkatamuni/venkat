package com.paanini.jiffy.utils.jackson.approles;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.option3.docube.schema.approles.*;
import com.option3.docube.schema.nodes.Type;
import com.paanini.jiffy.constants.Common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by
 */
public class RoleDeserializer extends StdDeserializer<Role> {

  public RoleDeserializer() {
    super(Role.class);
  }

  @Override
  public Role deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    if(node == null){
        throw new IOException("Unable to serialize role, node from parser is null");
    }
    String name = readStringProperty(node, "name");
    List<FileTypePermission> fileTypes = getFileTypePermission(node.get("filesTypes"));
    List<FileIdentifierPermission> fileIdentifierPermissions = getFileIdentifierPermission(
            node.get("filesIdentifiers"));
    List<Configuration> configurations = getConfiguration(node.get("configurations"));
    return new Role(name, fileTypes, fileIdentifierPermissions, configurations);
  }

  private List<FileTypePermission> getFileTypePermission(JsonNode node) {
    List<FileTypePermission> permissions = new ArrayList<>();
    if(node != null) {
      node.forEach(jsonNode -> {
        Type type = Type.valueOf(readStringProperty(jsonNode, "type"));
        Permission permission = Permission.valueOf(readStringProperty(jsonNode, "permission"));
        permissions.add(new FileTypePermission(type, permission));
      });
    }
    return permissions;
  }

  private List<FileIdentifierPermission> getFileIdentifierPermission(JsonNode node) {
    List<FileIdentifierPermission> permissions = new ArrayList<>();
    if(node != null) {
      node.forEach(jsonNode -> {
        String identifier = readStringProperty(jsonNode, "identifier");
        Permission permission = Permission.valueOf(readStringProperty(jsonNode, "permission"));
        permissions.add(new FileIdentifierPermission(identifier, permission));
      });
    }
    return permissions;
  }

  private List<Configuration> getConfiguration(JsonNode node) {
    List<Configuration> permissions = new ArrayList<>();
    if(node != null) {
      node.forEach(jsonNode -> {
        String name = readStringProperty(jsonNode, "name");
        permissions.add(new Configuration(name, getValue(jsonNode)));

      });
    }
    return permissions;
  }

  private Object getValue(JsonNode jsonNode) {
    final JsonNodeType type = jsonNode.get(Common.VALUE).getNodeType();
    switch(type) {
      case BOOLEAN:
        return jsonNode.get(Common.VALUE).asBoolean();
      case STRING:
        return jsonNode.get(Common.VALUE).asText();
      case NUMBER:
        return jsonNode.get(Common.VALUE).asLong();
      default:
        return null;
    }
  }

  /**
   * Returns a string value property if present, else empty
   */
  private String readStringProperty(JsonNode node, String property) {
    if(node != null) {
      JsonNode idNode = node.get(property);
      return idNode != null && idNode.getNodeType() == JsonNodeType.STRING ? idNode.asText() : "";
    }
    return "";
  }
}
