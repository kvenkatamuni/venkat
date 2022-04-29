package com.paanini.jiffy.utils.jackson.impex;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.paanini.jiffy.models.ImportAppOptions;
import com.paanini.jiffy.models.TradeEntity;
import com.paanini.jiffy.models.TradeFile;
import java.io.IOException;
import java.util.HashMap;

/**
 * @author Athul Krishna N S
 * @since 03/11/20
 */
public class ImportOptionsDeserializer extends StdDeserializer<ImportAppOptions> {

  public ImportOptionsDeserializer() {
    super(ImportAppOptions.class);
  }

  private JsonParser getParser(JsonNode node, JsonParser jsonParser) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonFactory jsonFactory = new JsonFactory();
    String treeString = objectMapper.writeValueAsString(objectMapper.treeToValue(node, Object.class));
    JsonParser newParser = jsonFactory.createParser(treeString);
    newParser.setCodec(jsonParser.getCodec());
    newParser.nextToken();

    return newParser;
  }
  @Override
  public ImportAppOptions deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException {

    ObjectMapper mapper = new ObjectMapper();

    JsonNode node = jsonParser.getCodec().readTree(jsonParser);
    if(node == null){
      throw new IOException("Unable to serialize ImportAppOptions, node from parser is null");
    }
    String id = readStringProperty(node, "fileId");
    String name = readStringProperty(node, "appName");
    boolean isNew = node.has("newApp")
        ? node.get("newApp").asBoolean(false)
        : false;
    JsonNode files = node.get("importOptions");
    HashMap<String, TradeFile> tradeEntities = getTradeEntities(mapper, files);

    return new ImportAppOptions(id, isNew, name, tradeEntities);
  }

  private HashMap<String, TradeFile> getTradeEntities(ObjectMapper mapper, JsonNode files) {
    HashMap<String, TradeFile> fileHashMap = new HashMap<>();
    files.fieldNames().forEachRemaining(field -> {
      JsonNode file = files.get(field);
      if(file != null){
        if(file.has("list")) {
          TradeEntity tradeEntity = getTradeEntity(mapper, file);
          if(tradeEntity != null){
            fileHashMap.put(field, tradeEntity);
          }

        } else {
          fileHashMap.put(field, mapper.convertValue(
                  file, TradeFile.class));
        }
      }

    });
    return fileHashMap;
  }

  private TradeEntity getTradeEntity(ObjectMapper mapper, JsonNode file) {
    if(file == null){
      return null;
    }
    String name = readStringProperty(file, "name");
    String type = file.has("type") ? readStringProperty(file, "type") : null;
    boolean selected = file.has("selected") && file.get("selected").asBoolean(true);

    return new TradeEntity(
        name, selected,
        getTradeEntities(mapper, file.get("list")),
        type);
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
